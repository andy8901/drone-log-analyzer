"""Import every model so SQLAlchemy's declarative registry sees all
mappers and relationships resolve (string-based relationship() targets)
before the app or Alembic-less startup tries to use them."""

from app.models.audit import AuditLog  # noqa: F401
from app.models.customer import Customer  # noqa: F401
from app.models.document import Document  # noqa: F401
from app.models.drone import Drone, DroneComponent  # noqa: F401
from app.models.flight import FlightLog  # noqa: F401
from app.models.invoice import Invoice, InvoiceItem, Payment  # noqa: F401
from app.models.maintenance import MaintenanceRecord, MaintenanceSchedule  # noqa: F401
from app.models.notification import Notification  # noqa: F401
from app.models.service_record import ServiceRecord  # noqa: F401
from app.models.ticket import Ticket, TicketAttachment, TicketComment  # noqa: F401
from app.models.user import User  # noqa: F401
from app.models.warranty import Warranty, WarrantyClaim  # noqa: F401

__all__ = [
    "AuditLog",
    "Customer",
    "Document",
    "Drone",
    "DroneComponent",
    "FlightLog",
    "Invoice",
    "InvoiceItem",
    "Payment",
    "MaintenanceRecord",
    "MaintenanceSchedule",
    "Notification",
    "ServiceRecord",
    "Ticket",
    "TicketAttachment",
    "TicketComment",
    "User",
    "Warranty",
    "WarrantyClaim",
]
