from pydantic import BaseModel

from app.schemas.drone import DroneOut
from app.schemas.invoice import InvoiceOut
from app.schemas.service_record import ServiceRecordOut
from app.schemas.ticket import TicketOut


class SearchResultsOut(BaseModel):
    drones: list[DroneOut] = []
    tickets: list[TicketOut] = []
    invoices: list[InvoiceOut] = []
    service_records: list[ServiceRecordOut] = []
