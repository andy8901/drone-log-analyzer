import uuid
from datetime import date, datetime

from pydantic import BaseModel


class DroneComponentOut(BaseModel):
    id: uuid.UUID
    component_type: str
    name: str
    serial_number: str | None = None
    cycle_count: int | None = None
    installed_date: date | None = None
    status: str

    model_config = {"from_attributes": True}


class DroneOut(BaseModel):
    id: uuid.UUID
    drone_name: str
    model: str
    serial_number: str
    uin: str | None = None
    status: str
    total_flight_hours: float
    total_flights: int
    last_flight_at: datetime | None = None
    warranty_end_date: date | None = None
    firmware_version: str | None = None

    model_config = {"from_attributes": True}


class WarrantySummaryOut(BaseModel):
    status: str | None = None
    end_date: date | None = None
    days_remaining: int | None = None


class MaintenanceSummaryOut(BaseModel):
    status: str | None = None
    next_due_date: date | None = None
    next_due_hours: float | None = None
    remaining_hours: float | None = None


class DroneDetailOut(DroneOut):
    purchase_date: date | None = None
    delivery_date: date | None = None
    invoice_number: str | None = None
    invoice_date: date | None = None
    warranty_start_date: date | None = None
    next_maintenance_due_hours: float | None = None
    next_maintenance_due_date: date | None = None
    last_service_at: datetime | None = None
    components: list[DroneComponentOut] = []
    warranty_summary: WarrantySummaryOut | None = None
    maintenance_summary: MaintenanceSummaryOut | None = None


class DroneCreate(BaseModel):
    customer_id: uuid.UUID
    drone_name: str
    model: str
    serial_number: str
    uin: str | None = None
    purchase_date: date | None = None
    delivery_date: date | None = None
    invoice_number: str | None = None
    invoice_date: date | None = None
    warranty_start_date: date | None = None
    warranty_end_date: date | None = None
    firmware_version: str | None = None
    status: str = "active"
