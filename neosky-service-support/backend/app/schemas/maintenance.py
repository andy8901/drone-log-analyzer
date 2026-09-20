import uuid
from datetime import date, datetime

from pydantic import BaseModel


class MaintenanceScheduleOut(BaseModel):
    interval_flight_hours: float | None = None
    interval_calendar_days: int | None = None
    last_maintenance_date: date | None = None
    current_flight_hours: float
    next_due_hours: float | None = None
    remaining_hours: float | None = None
    next_due_date: date | None = None
    status: str


class MaintenanceRecordOut(BaseModel):
    id: uuid.UUID
    maintenance_type: str
    performed_at: datetime
    description: str | None = None
    status: str

    model_config = {"from_attributes": True}


class MaintenanceOut(BaseModel):
    schedule: MaintenanceScheduleOut | None = None
    records: list[MaintenanceRecordOut] = []


class MaintenanceScheduleUpsert(BaseModel):
    interval_flight_hours: float | None = None
    interval_calendar_days: int | None = None


class MaintenanceRecordCreate(BaseModel):
    maintenance_type: str
    flight_hours_at_service: float | None = None
    description: str | None = None
    parts_replaced: list[dict] = []
    ticket_id: uuid.UUID | None = None
    performed_at: datetime | None = None
