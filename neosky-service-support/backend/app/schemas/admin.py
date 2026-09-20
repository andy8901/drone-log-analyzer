import uuid

from pydantic import BaseModel

from app.schemas.drone import DroneOut
from app.schemas.flight import FlightOut
from app.schemas.invoice import InvoiceOut
from app.schemas.service_record import ServiceRecordOut
from app.schemas.ticket import TicketOut


class AdminCustomerOut(BaseModel):
    id: uuid.UUID
    customer_code: str
    full_name: str
    email: str
    phone: str | None = None
    company_name: str | None = None


class AdminCustomerHistoryOut(BaseModel):
    customer: AdminCustomerOut
    drones: list[DroneOut] = []
    tickets: list[TicketOut] = []
    invoices: list[InvoiceOut] = []
    service_records: list[ServiceRecordOut] = []


class AdminTicketOut(TicketOut):
    customer_id: uuid.UUID


class AdminFlightsOut(BaseModel):
    items: list[FlightOut]
    total: int
