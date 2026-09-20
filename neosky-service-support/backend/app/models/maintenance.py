import uuid

from sqlalchemy import Date, DateTime, ForeignKey, Integer, Numeric, String, Text, func, text
from sqlalchemy.dialects.postgresql import JSONB, UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base
from app.models.enums import maintenance_status_enum


class MaintenanceSchedule(Base):
    __tablename__ = "maintenance_schedule"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    drone_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("drones.id", ondelete="CASCADE"), unique=True, nullable=False
    )
    interval_flight_hours: Mapped[float | None] = mapped_column(Numeric(10, 2), nullable=True)
    interval_calendar_days: Mapped[int | None] = mapped_column(Integer, nullable=True)
    last_maintenance_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    last_maintenance_hours: Mapped[float | None] = mapped_column(Numeric(10, 2), nullable=True)
    next_due_date: Mapped[object | None] = mapped_column(Date, nullable=True)
    next_due_hours: Mapped[float | None] = mapped_column(Numeric(10, 2), nullable=True)
    status: Mapped[str] = mapped_column(maintenance_status_enum, nullable=False, server_default="upcoming")
    created_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())

    drone = relationship("Drone", back_populates="maintenance_schedule")


class MaintenanceRecord(Base):
    __tablename__ = "maintenance_records"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    drone_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("drones.id", ondelete="CASCADE"), nullable=False
    )
    maintenance_schedule_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("maintenance_schedule.id"), nullable=True
    )
    ticket_id: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("tickets.id"), nullable=True
    )
    performed_by: Mapped[uuid.UUID | None] = mapped_column(
        UUID(as_uuid=True), ForeignKey("users.id"), nullable=True
    )
    maintenance_type: Mapped[str] = mapped_column(String, nullable=False)
    flight_hours_at_service: Mapped[float | None] = mapped_column(Numeric(10, 2), nullable=True)
    description: Mapped[str | None] = mapped_column(Text, nullable=True)
    parts_replaced: Mapped[list] = mapped_column(JSONB, nullable=False, server_default=text("'[]'::jsonb"))
    performed_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())
    status: Mapped[str] = mapped_column(String, nullable=False, server_default="completed")
    created_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())

    drone = relationship("Drone", back_populates="maintenance_records")
