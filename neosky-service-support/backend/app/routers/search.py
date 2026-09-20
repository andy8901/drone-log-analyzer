from fastapi import APIRouter, Depends, Query
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.database import get_db
from app.models.customer import Customer
from app.models.drone import Drone
from app.models.invoice import Invoice
from app.models.service_record import ServiceRecord
from app.models.ticket import Ticket
from app.schemas.drone import DroneOut
from app.schemas.invoice import InvoiceOut
from app.schemas.search import SearchResultsOut
from app.schemas.service_record import ServiceRecordOut
from app.schemas.ticket import TicketOut

router = APIRouter(prefix="/api/search", tags=["search"])


@router.get("", response_model=SearchResultsOut)
def search(
    q: str = Query(..., min_length=1),
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    like = f"%{q}%"

    drones = (
        db.query(Drone)
        .filter(Drone.customer_id == customer.id)
        .filter(
            (Drone.drone_name.ilike(like)) | (Drone.serial_number.ilike(like)) | (Drone.model.ilike(like))
        )
        .all()
    )
    drone_ids = [d.id for d in db.query(Drone.id).filter(Drone.customer_id == customer.id).all()]

    tickets = (
        db.query(Ticket)
        .filter(Ticket.customer_id == customer.id)
        .filter(
            (Ticket.subject.ilike(like))
            | (Ticket.ticket_number.ilike(like))
            | (Ticket.description.ilike(like))
        )
        .all()
    )

    invoices = (
        db.query(Invoice)
        .filter(Invoice.customer_id == customer.id)
        .filter((Invoice.invoice_number.ilike(like)) | (Invoice.product_service.ilike(like)))
        .all()
    )

    service_records = []
    if drone_ids:
        service_records = (
            db.query(ServiceRecord)
            .filter(ServiceRecord.drone_id.in_(drone_ids))
            .filter((ServiceRecord.issue_summary.ilike(like)) | (ServiceRecord.action_taken.ilike(like)))
            .all()
        )

    return SearchResultsOut(
        drones=[DroneOut.model_validate(d) for d in drones],
        tickets=[TicketOut.model_validate(t) for t in tickets],
        invoices=[InvoiceOut.model_validate(i) for i in invoices],
        service_records=[ServiceRecordOut.model_validate(s) for s in service_records],
    )
