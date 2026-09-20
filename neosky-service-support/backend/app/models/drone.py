import uuid

from sqlalchemy import (
    Date,
    DateTime,
    ForeignKey,
    Integer,
    Numeric,
    String,
    func,
    text,
)
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base
from app.models.enums import component_type_enum, drone_status_enum


class Drone(Base):
    __tablename__ = "drones"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    customer_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("customers.id", ondelete="CASCADE"), nullable=False
    )
    drone_name: Mapped[str] = mapped_column(String, nullable=False)
    model: Mapped[str] = mapped_column(String, nullable=False)
    serial_number: Mapped[str] = mapped_column(String, unique=True, nullable=False)
    uin: Mapped[str | None] = mapped_column(String, nullable=True)
    purchase_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    delivery_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    invoice_number: Mapped[str | None] = mapped_column(String, nullable=True)
    invoice_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    warranty_start_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    warranty_end_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    total_flight_hours: Mapped[float] = mapped_column(
        Numeric(10, 2), nullable=False, server_default=text("0")
    )
    total_flights: Mapped[int] = mapped_column(Integer, nullable=False, server_default=text("0"))
    last_flight_at: Mapped[object | None] = mapped_column(DateTime(timezone=True), nullable=True)
    last_service_at: Mapped[object | None] = mapped_column(DateTime(timezone=True), nullable=True)
    next_maintenance_due_hours: Mapped[float | None] = mapped_column(Numeric(10, 2), nullable=True)
    next_maintenance_due_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    firmware_version: Mapped[str | None] = mapped_column(String, nullable=True)
    status: Mapped[str] = mapped_column(drone_status_enum, nullable=False, server_default="active")
    created_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())

    customer = relationship("Customer", back_populates="drones")
    components = relationship("DroneComponent", back_populates="drone", cascade="all, delete-orphan")
    tickets = relationship("Ticket", back_populates="drone", cascade="all, delete-orphan")
    flight_logs = relationship("FlightLog", back_populates="drone", cascade="all, delete-orphan")
    maintenance_schedule = relationship(
        "MaintenanceSchedule", back_populates="drone", uselist=False, cascade="all, delete-orphan"
    )
    maintenance_records = relationship(
        "MaintenanceRecord", back_populates="drone", cascade="all, delete-orphan"
    )
    warranty = relationship("Warranty", back_populates="drone", uselist=False, cascade="all, delete-orphan")
    service_records = relationship("ServiceRecord", back_populates="drone", cascade="all, delete-orphan")


class DroneComponent(Base):
    __tablename__ = "drone_components"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    drone_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("drones.id", ondelete="CASCADE"), nullable=False
    )
    component_type: Mapped[str] = mapped_column(component_type_enum, nullable=False)
    name: Mapped[str] = mapped_column(String, nullable=False)
    serial_number: Mapped[str | None] = mapped_column(String, nullable=True)
    cycle_count: Mapped[int | None] = mapped_column(Integer, server_default=text("0"))
    installed_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    status: Mapped[str] = mapped_column(String, nullable=False, server_default="active")
    metadata_: Mapped[dict] = mapped_column(
        "metadata", JSONB, nullable=False, server_default=text("'{}'::jsonb")
    )
    created_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())

    drone = relationship("Drone", back_populates="components")
