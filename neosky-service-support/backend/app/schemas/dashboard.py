import uuid
from datetime import date

from pydantic import BaseModel


class RecentFlightOut(BaseModel):
    id: uuid.UUID
    drone_name: str
    flight_date: date
    duration_minutes: float


class RecentTicketOut(BaseModel):
    id: uuid.UUID
    ticket_number: str
    subject: str
    status: str


class RecentInvoiceOut(BaseModel):
    id: uuid.UUID
    invoice_number: str
    total_amount: float
    payment_status: str


class DashboardOut(BaseModel):
    registered_drones: int
    active_warranty_drones: int
    warranty_expiring_soon: int
    open_tickets: int
    pending_service_requests: int
    upcoming_maintenance: int
    total_flight_hours: float
    recent_flight: RecentFlightOut | None = None
    recent_ticket: RecentTicketOut | None = None
    recent_invoice: RecentInvoiceOut | None = None
