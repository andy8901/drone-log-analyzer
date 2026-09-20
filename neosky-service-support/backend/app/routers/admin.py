import uuid as uuid_mod
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Query, Request, status
from sqlalchemy.orm import Session

from app.core.audit import write_audit_log
from app.core.security import require_role
from app.database import get_db
from app.models.customer import Customer
from app.models.document import Document
from app.models.drone import Drone
from app.models.flight import FlightLog
from app.models.invoice import Invoice
from app.models.maintenance import MaintenanceSchedule
from app.models.service_record import ServiceRecord
from app.models.ticket import Ticket
from app.models.user import User
from app.models.warranty import Warranty, WarrantyClaim
from app.schemas.admin import AdminCustomerHistoryOut, AdminCustomerOut
from app.schemas.common import paginate
from app.schemas.drone import DroneCreate, DroneDetailOut, DroneOut
from app.schemas.flight import FlightOut
from app.schemas.invoice import InvoiceCreate, InvoiceDetailOut
from app.schemas.maintenance import MaintenanceScheduleUpsert
from app.schemas.service_record import ServiceRecordCreate, ServiceRecordOut
from app.schemas.ticket import (
    TicketAssignRequest,
    TicketCommentCreate,
    TicketCommentOut,
    TicketOut,
    TicketStatusUpdateRequest,
)
from app.schemas.warranty import WarrantyClaimCreate, WarrantyClaimFullOut, WarrantyOut, WarrantyUpdate
from app.services.drone_service import build_drone_detail, create_drone
from app.services.invoice_service import create_invoice
from app.services.notification_service import create_notification
from app.services.ticket_service import add_comment, add_status_transition
from app.services.warranty_service import days_remaining, derive_warranty_status

router = APIRouter(prefix="/api/admin", tags=["admin"])

RESOLVED_STATUSES_REQUIRING_NOTIFICATION = {
    "resolved": "ticket_resolved",
    "waiting_customer": "customer_response_required",
    "assigned": "ticket_assigned",
    "closed": "ticket_closed",
}


def _uuid_or_404(value: str) -> uuid_mod.UUID:
    try:
        return uuid_mod.UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )


def _client_ip(request: Request) -> str | None:
    return request.client.host if request.client else None


def _customer_out(customer: Customer) -> AdminCustomerOut:
    return AdminCustomerOut(
        id=customer.id,
        customer_code=customer.customer_code,
        full_name=customer.user.full_name,
        email=customer.user.email,
        phone=customer.user.phone,
        company_name=customer.company_name,
    )


# ---------------------------------------------------------------- customers
@router.get("/customers", response_model=list[AdminCustomerOut])
def list_customers(
    current_user: User = Depends(require_role("service_engineer", "admin")), db: Session = Depends(get_db)
):
    customers = db.query(Customer).all()
    return [_customer_out(c) for c in customers]


@router.get("/customers/{customer_id}", response_model=AdminCustomerOut)
def get_customer(
    customer_id: str,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    customer = db.get(Customer, _uuid_or_404(customer_id))
    if customer is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    return _customer_out(customer)


@router.get("/customers/{customer_id}/history", response_model=AdminCustomerHistoryOut)
def customer_history(
    customer_id: str,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    customer = db.get(Customer, _uuid_or_404(customer_id))
    if customer is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    drones = db.query(Drone).filter(Drone.customer_id == customer.id).all()
    tickets = db.query(Ticket).filter(Ticket.customer_id == customer.id).all()
    invoices = db.query(Invoice).filter(Invoice.customer_id == customer.id).all()
    drone_ids = [d.id for d in drones]
    service_records = (
        db.query(ServiceRecord).filter(ServiceRecord.drone_id.in_(drone_ids)).all() if drone_ids else []
    )
    return AdminCustomerHistoryOut(
        customer=_customer_out(customer),
        drones=[DroneOut.model_validate(d) for d in drones],
        tickets=[TicketOut.model_validate(t) for t in tickets],
        invoices=[InvoiceDetailOut.model_validate(i) for i in invoices],
        service_records=[ServiceRecordOut.model_validate(s) for s in service_records],
    )


# -------------------------------------------------------------------- drones
@router.get("/drones", response_model=list[DroneOut])
def list_all_drones(
    current_user: User = Depends(require_role("service_engineer", "admin")), db: Session = Depends(get_db)
):
    return db.query(Drone).order_by(Drone.created_at.desc()).all()


@router.post("/drones", response_model=DroneDetailOut, status_code=status.HTTP_201_CREATED)
def admin_register_drone(
    payload: DroneCreate,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    try:
        drone = create_drone(db, payload)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "customer not found"}},
        )
    write_audit_log(
        db,
        user_id=current_user.id,
        action="register_drone",
        entity_type="drones",
        entity_id=drone.id,
        ip_address=_client_ip(request),
        metadata={"customer_id": str(drone.customer_id)},
    )
    db.commit()
    return build_drone_detail(db, drone)


@router.get("/drones/{drone_id}/flights", response_model=list[FlightOut])
def admin_drone_flights(
    drone_id: str,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    drone = db.get(Drone, _uuid_or_404(drone_id))
    if drone is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    flights = (
        db.query(FlightLog).filter(FlightLog.drone_id == drone.id).order_by(FlightLog.start_time.desc()).all()
    )
    return flights


@router.put("/drones/{drone_id}/warranty", response_model=WarrantyOut)
def admin_update_warranty(
    drone_id: str,
    payload: WarrantyUpdate,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    drone = db.get(Drone, _uuid_or_404(drone_id))
    if drone is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    warranty = db.query(Warranty).filter(Warranty.drone_id == drone.id).first()
    if warranty is None:
        if payload.start_date is None or payload.end_date is None:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail={
                    "error": {
                        "code": "VALIDATION_ERROR",
                        "message": "start_date and end_date are required to create a warranty",
                    }
                },
            )
        warranty = Warranty(drone_id=drone.id, start_date=payload.start_date, end_date=payload.end_date)
        db.add(warranty)
        db.flush()

    data = payload.model_dump(exclude_unset=True)
    for field in ("start_date", "end_date", "covered_items", "excluded_items"):
        if field in data and data[field] is not None:
            setattr(warranty, field, data[field])
    warranty.status = derive_warranty_status(warranty.end_date)
    db.add(warranty)

    write_audit_log(
        db,
        user_id=current_user.id,
        action="update_warranty",
        entity_type="warranties",
        entity_id=warranty.id,
        ip_address=_client_ip(request),
        metadata={"drone_id": str(drone.id)},
    )
    db.commit()
    db.refresh(warranty)
    return WarrantyOut(
        drone_id=drone.id,
        start_date=warranty.start_date,
        end_date=warranty.end_date,
        days_remaining=days_remaining(warranty.end_date),
        status=derive_warranty_status(warranty.end_date),
        covered_items=warranty.covered_items,
        excluded_items=warranty.excluded_items,
        claims=[],
    )


@router.post(
    "/drones/{drone_id}/warranty/claims",
    response_model=WarrantyClaimFullOut,
    status_code=status.HTTP_201_CREATED,
)
def admin_create_warranty_claim(
    drone_id: str,
    payload: WarrantyClaimCreate,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    drone = db.get(Drone, _uuid_or_404(drone_id))
    if drone is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    warranty = db.query(Warranty).filter(Warranty.drone_id == drone.id).first()
    if warranty is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Drone has no warranty on file"}},
        )
    ticket = db.get(Ticket, payload.ticket_id)
    if ticket is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "ticket not found"}},
        )

    year = datetime.now(timezone.utc).year
    count = db.query(WarrantyClaim).filter(WarrantyClaim.claim_number.like(f"WC-{year}-%")).count()
    claim_number = f"WC-{year}-{count + 1:04d}"

    claim = WarrantyClaim(
        warranty_id=warranty.id,
        ticket_id=payload.ticket_id,
        claim_number=claim_number,
        description=payload.description,
        status="submitted",
    )
    db.add(claim)

    write_audit_log(
        db,
        user_id=current_user.id,
        action="create_warranty_claim",
        entity_type="warranty_claims",
        entity_id=None,
        ip_address=_client_ip(request),
        metadata={"drone_id": str(drone.id), "ticket_id": str(payload.ticket_id)},
    )
    db.commit()
    db.refresh(claim)
    return claim


@router.post("/drones/{drone_id}/maintenance/schedule", status_code=status.HTTP_200_OK)
def admin_upsert_maintenance_schedule(
    drone_id: str,
    payload: MaintenanceScheduleUpsert,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    drone = db.get(Drone, _uuid_or_404(drone_id))
    if drone is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    schedule = db.query(MaintenanceSchedule).filter(MaintenanceSchedule.drone_id == drone.id).first()
    if schedule is None:
        schedule = MaintenanceSchedule(drone_id=drone.id)
        db.add(schedule)
        db.flush()

    if payload.interval_flight_hours is not None:
        schedule.interval_flight_hours = payload.interval_flight_hours
    if payload.interval_calendar_days is not None:
        schedule.interval_calendar_days = payload.interval_calendar_days

    from app.services.maintenance_service import compute_next_due

    next_due_hours, next_due_date = compute_next_due(
        schedule.last_maintenance_hours,
        schedule.last_maintenance_date,
        schedule.interval_flight_hours,
        schedule.interval_calendar_days,
    )
    if next_due_hours is not None:
        schedule.next_due_hours = next_due_hours
    if next_due_date is not None:
        schedule.next_due_date = next_due_date
    db.add(schedule)

    write_audit_log(
        db,
        user_id=current_user.id,
        action="upsert_maintenance_schedule",
        entity_type="maintenance_schedule",
        entity_id=schedule.id,
        ip_address=_client_ip(request),
        metadata={"drone_id": str(drone.id)},
    )
    db.commit()
    return {"message": "Maintenance schedule updated"}


# -------------------------------------------------------------------- tickets
@router.get("/tickets")
def admin_list_tickets(
    status_filter: str | None = Query(default=None, alias="status"),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    query = db.query(Ticket)
    if status_filter:
        query = query.filter(Ticket.status == status_filter)
    total = query.count()
    items = query.order_by(Ticket.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()
    return paginate([TicketOut.model_validate(i) for i in items], total, page, page_size)


@router.put("/tickets/{ticket_id}/assign", response_model=TicketOut)
def assign_ticket(
    ticket_id: str,
    payload: TicketAssignRequest,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    ticket = db.get(Ticket, _uuid_or_404(ticket_id))
    if ticket is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    engineer = db.get(User, payload.engineer_id)
    if engineer is None or engineer.role not in ("service_engineer", "admin"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={
                "error": {
                    "code": "VALIDATION_ERROR",
                    "message": "engineer_id must reference a service_engineer/admin user",
                }
            },
        )

    ticket.assigned_engineer_id = engineer.id
    previous_status = ticket.status
    if ticket.status == "new":
        ticket.status = "assigned"
    db.add(ticket)

    add_status_transition(
        db,
        ticket,
        author_id=current_user.id,
        status_from=previous_status,
        status_to=ticket.status,
        comment=f"Assigned to {engineer.full_name}.",
    )

    write_audit_log(
        db,
        user_id=current_user.id,
        action="assign_ticket",
        entity_type="tickets",
        entity_id=ticket.id,
        ip_address=_client_ip(request),
        metadata={"engineer_id": str(engineer.id)},
    )

    customer_user = ticket.customer.user
    create_notification(
        db,
        user=customer_user,
        type_="ticket_assigned",
        title="Ticket assigned",
        body=f"Ticket {ticket.ticket_number} has been assigned to an engineer.",
        related_entity_type="ticket",
        related_entity_id=ticket.id,
    )
    create_notification(
        db,
        user=engineer,
        type_="ticket_assigned",
        title="Ticket assigned to you",
        body=f"Ticket {ticket.ticket_number} ({ticket.subject}) has been assigned to you.",
        related_entity_type="ticket",
        related_entity_id=ticket.id,
    )

    db.commit()
    db.refresh(ticket)
    return ticket


VALID_STATUSES = {
    "new",
    "assigned",
    "in_progress",
    "waiting_customer",
    "waiting_parts",
    "service",
    "qc",
    "resolved",
    "closed",
}


@router.put("/tickets/{ticket_id}/status", response_model=TicketOut)
def update_ticket_status(
    ticket_id: str,
    payload: TicketStatusUpdateRequest,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    if payload.status not in VALID_STATUSES:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "invalid status"}},
        )
    ticket = db.get(Ticket, _uuid_or_404(ticket_id))
    if ticket is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )

    previous_status = ticket.status
    ticket.status = payload.status
    if payload.status == "resolved":
        ticket.resolved_at = datetime.now(timezone.utc)
    if payload.status == "closed":
        ticket.closed_at = datetime.now(timezone.utc)
    db.add(ticket)

    add_status_transition(
        db,
        ticket,
        author_id=current_user.id,
        status_from=previous_status,
        status_to=payload.status,
        comment=payload.comment or f"Status changed to {payload.status}.",
    )

    write_audit_log(
        db,
        user_id=current_user.id,
        action="update_ticket_status",
        entity_type="tickets",
        entity_id=ticket.id,
        ip_address=_client_ip(request),
        metadata={"status_from": previous_status, "status_to": payload.status},
    )

    notif_type = RESOLVED_STATUSES_REQUIRING_NOTIFICATION.get(payload.status, "ticket_status_changed")
    create_notification(
        db,
        user=ticket.customer.user,
        type_=notif_type,
        title="Ticket status updated",
        body=f"Ticket {ticket.ticket_number} status changed to {payload.status}.",
        related_entity_type="ticket",
        related_entity_id=ticket.id,
    )

    db.commit()
    db.refresh(ticket)
    return ticket


@router.post(
    "/tickets/{ticket_id}/comments", response_model=TicketCommentOut, status_code=status.HTTP_201_CREATED
)
def admin_add_ticket_comment(
    ticket_id: str,
    payload: TicketCommentCreate,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    ticket = db.get(Ticket, _uuid_or_404(ticket_id))
    if ticket is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    comment = add_comment(
        db, ticket, author_id=current_user.id, comment=payload.comment, is_internal=payload.is_internal
    )

    if not payload.is_internal:
        create_notification(
            db,
            user=ticket.customer.user,
            type_="engineer_comment",
            title="New comment on your ticket",
            body=f"New update on ticket {ticket.ticket_number}.",
            related_entity_type="ticket",
            related_entity_id=ticket.id,
        )

    db.commit()
    db.refresh(comment)
    return comment


# --------------------------------------------------------------- inv/service
@router.post("/invoices", response_model=InvoiceDetailOut, status_code=status.HTTP_201_CREATED)
def admin_create_invoice(
    payload: InvoiceCreate,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    customer = db.get(Customer, payload.customer_id)
    if customer is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "customer not found"}},
        )
    invoice = create_invoice(db, payload)

    write_audit_log(
        db,
        user_id=current_user.id,
        action="create_invoice",
        entity_type="invoices",
        entity_id=invoice.id,
        ip_address=_client_ip(request),
        metadata={"customer_id": str(payload.customer_id), "total_amount": str(invoice.total_amount)},
    )

    create_notification(
        db,
        user=customer.user,
        type_="invoice_generated",
        title="New invoice",
        body=f"Invoice {invoice.invoice_number} for {invoice.total_amount} has been generated.",
        related_entity_type="invoice",
        related_entity_id=invoice.id,
    )
    if invoice.payment_status == "pending":
        create_notification(
            db,
            user=customer.user,
            type_="payment_pending",
            title="Payment pending",
            body=f"Invoice {invoice.invoice_number} is pending payment.",
            related_entity_type="invoice",
            related_entity_id=invoice.id,
        )

    db.commit()
    db.refresh(invoice)
    return invoice


@router.post("/service-records", response_model=ServiceRecordOut, status_code=status.HTTP_201_CREATED)
def admin_create_service_record(
    payload: ServiceRecordCreate,
    request: Request,
    current_user: User = Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    drone = db.get(Drone, payload.drone_id)
    if drone is None:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "drone not found"}},
        )
    record = ServiceRecord(
        drone_id=payload.drone_id,
        ticket_id=payload.ticket_id,
        maintenance_record_id=payload.maintenance_record_id,
        service_date=payload.service_date,
        issue_summary=payload.issue_summary,
        action_taken=payload.action_taken,
        performed_by=current_user.id,
        status=payload.status,
    )
    db.add(record)
    db.flush()

    if payload.document_file_url and payload.document_file_name:
        db.add(
            Document(
                document_type=payload.document_type or "service_report",
                customer_id=drone.customer_id,
                drone_id=drone.id,
                service_record_id=record.id,
                file_name=payload.document_file_name,
                file_url=payload.document_file_url,
                uploaded_by=current_user.id,
            )
        )

    write_audit_log(
        db,
        user_id=current_user.id,
        action="create_service_record",
        entity_type="service_records",
        entity_id=record.id,
        ip_address=_client_ip(request),
        metadata={"drone_id": str(drone.id)},
    )

    create_notification(
        db,
        user=drone.customer.user,
        type_="service_completed",
        title="Service completed",
        body=f"Service completed for {drone.drone_name}.",
        related_entity_type="drone",
        related_entity_id=drone.id,
    )

    db.commit()
    db.refresh(record)
    return record
