from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.database import get_db
from app.models.customer import Customer
from app.models.drone import Drone
from app.models.flight import FlightLog
from app.models.invoice import Invoice
from app.models.maintenance import MaintenanceSchedule
from app.models.ticket import Ticket
from app.models.warranty import Warranty
from app.schemas.dashboard import DashboardOut, RecentFlightOut, RecentInvoiceOut, RecentTicketOut
from app.services.maintenance_service import derive_maintenance_status
from app.services.warranty_service import derive_warranty_status

router = APIRouter(prefix="/api/dashboard", tags=["dashboard"])

OPEN_TICKET_STATUSES = {
    "new",
    "assigned",
    "in_progress",
    "waiting_customer",
    "waiting_parts",
    "service",
    "qc",
}
PENDING_SERVICE_STATUSES = {"service", "waiting_parts"}


@router.get("", response_model=DashboardOut)
def get_dashboard(customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)):
    drones = db.query(Drone).filter(Drone.customer_id == customer.id).all()
    drone_ids = [d.id for d in drones]

    active_warranty_drones = 0
    warranty_expiring_soon = 0
    if drone_ids:
        warranties = db.query(Warranty).filter(Warranty.drone_id.in_(drone_ids)).all()
        for w in warranties:
            derived = derive_warranty_status(w.end_date)
            if derived == "active":
                active_warranty_drones += 1
            elif derived == "expiring_soon":
                warranty_expiring_soon += 1

    tickets = db.query(Ticket).filter(Ticket.customer_id == customer.id).all()
    open_tickets = sum(1 for t in tickets if t.status in OPEN_TICKET_STATUSES)
    pending_service_requests = sum(1 for t in tickets if t.status in PENDING_SERVICE_STATUSES)

    upcoming_maintenance = 0
    if drone_ids:
        schedules = db.query(MaintenanceSchedule).filter(MaintenanceSchedule.drone_id.in_(drone_ids)).all()
        drones_by_id = {d.id: d for d in drones}
        for s in schedules:
            drone = drones_by_id.get(s.drone_id)
            current_hours = float(drone.total_flight_hours) if drone else 0.0
            status = derive_maintenance_status(current_hours, s.next_due_hours, s.next_due_date)
            if status in ("due_soon", "due", "overdue"):
                upcoming_maintenance += 1

    total_flight_hours = sum(float(d.total_flight_hours) for d in drones)

    recent_flight_row = (
        db.query(FlightLog, Drone.drone_name)
        .join(Drone, Drone.id == FlightLog.drone_id)
        .filter(FlightLog.customer_id == customer.id)
        .order_by(FlightLog.end_time.desc())
        .first()
    )
    recent_flight = None
    if recent_flight_row is not None:
        flight, drone_name = recent_flight_row
        recent_flight = RecentFlightOut(
            id=flight.id,
            drone_name=drone_name,
            flight_date=flight.flight_date,
            duration_minutes=float(flight.duration_minutes),
        )

    recent_ticket_row = (
        db.query(Ticket).filter(Ticket.customer_id == customer.id).order_by(Ticket.created_at.desc()).first()
    )
    recent_ticket = None
    if recent_ticket_row is not None:
        recent_ticket = RecentTicketOut(
            id=recent_ticket_row.id,
            ticket_number=recent_ticket_row.ticket_number,
            subject=recent_ticket_row.subject,
            status=recent_ticket_row.status,
        )

    recent_invoice_row = (
        db.query(Invoice)
        .filter(Invoice.customer_id == customer.id)
        .order_by(Invoice.created_at.desc())
        .first()
    )
    recent_invoice = None
    if recent_invoice_row is not None:
        recent_invoice = RecentInvoiceOut(
            id=recent_invoice_row.id,
            invoice_number=recent_invoice_row.invoice_number,
            total_amount=float(recent_invoice_row.total_amount),
            payment_status=recent_invoice_row.payment_status,
        )

    return DashboardOut(
        registered_drones=len(drones),
        active_warranty_drones=active_warranty_drones,
        warranty_expiring_soon=warranty_expiring_soon,
        open_tickets=open_tickets,
        pending_service_requests=pending_service_requests,
        upcoming_maintenance=upcoming_maintenance,
        total_flight_hours=round(total_flight_hours, 2),
        recent_flight=recent_flight,
        recent_ticket=recent_ticket,
        recent_invoice=recent_invoice,
    )
