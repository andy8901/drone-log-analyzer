import uuid

from sqlalchemy import Boolean, Date, DateTime, ForeignKey, Integer, Numeric, String, Text, func, text
from sqlalchemy.dialects.postgresql import UUID
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database import Base
from app.models.enums import flight_result_enum, sync_status_enum


class FlightLog(Base):
    __tablename__ = "flight_logs"

    id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), primary_key=True, server_default=text("gen_random_uuid()")
    )
    client_uuid: Mapped[uuid.UUID | None] = mapped_column(UUID(as_uuid=True), unique=True, nullable=True)
    drone_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("drones.id", ondelete="CASCADE"), nullable=False
    )
    customer_id: Mapped[uuid.UUID] = mapped_column(
        UUID(as_uuid=True), ForeignKey("customers.id", ondelete="CASCADE"), nullable=False
    )
    pilot_id: Mapped[uuid.UUID] = mapped_column(UUID(as_uuid=True), ForeignKey("users.id"), nullable=False)
    flight_date: Mapped[object] = mapped_column(Date, nullable=False)
    start_time: Mapped[object] = mapped_column(DateTime(timezone=True), nullable=False)
    end_time: Mapped[object] = mapped_column(DateTime(timezone=True), nullable=False)
    duration_minutes: Mapped[float] = mapped_column(Numeric(10, 2), nullable=False)
    location: Mapped[str | None] = mapped_column(String, nullable=True)
    max_altitude_m: Mapped[float | None] = mapped_column(Numeric(10, 2), nullable=True)
    distance_travelled_km: Mapped[float | None] = mapped_column(Numeric(10, 2), nullable=True)
    mission_type: Mapped[str | None] = mapped_column(String, nullable=True)
    payload_used: Mapped[str | None] = mapped_column(String, nullable=True)
    battery_used: Mapped[str | None] = mapped_column(String, nullable=True)
    battery_cycle: Mapped[int | None] = mapped_column(Integer, nullable=True)
    weather: Mapped[str | None] = mapped_column(String, nullable=True)
    flight_result: Mapped[str] = mapped_column(
        flight_result_enum, nullable=False, server_default="successful"
    )
    remarks: Mapped[str | None] = mapped_column(Text, nullable=True)
    incident_flag: Mapped[bool] = mapped_column(Boolean, nullable=False, server_default=text("false"))
    sync_status: Mapped[str] = mapped_column(sync_status_enum, nullable=False, server_default="synced")
    created_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[object] = mapped_column(DateTime(timezone=True), server_default=func.now())

    drone = relationship("Drone", back_populates="flight_logs")
    customer = relationship("Customer")
    pilot = relationship("User")
