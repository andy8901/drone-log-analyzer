import uuid as uuid_mod

from fastapi import APIRouter, Depends, File, Form, HTTPException, Query, UploadFile, status
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.core.ownership import get_owned_ticket_or_404
from app.core.security import get_current_user
from app.database import get_db
from app.models.customer import Customer
from app.models.drone import Drone
from app.models.ticket import Ticket, TicketAttachment
from app.models.user import User
from app.schemas.common import paginate
from app.schemas.ticket import (
    TicketAttachmentOut,
    TicketCloseRequest,
    TicketCommentCreate,
    TicketCommentOut,
    TicketCreate,
    TicketDetailOut,
    TicketOut,
    TimelineEntryOut,
)
from app.services.notification_service import create_notification
from app.services.ticket_service import add_comment, generate_ticket_number
from app.services.upload_service import save_upload

router = APIRouter(prefix="/api/tickets", tags=["tickets"])

CLOSED_STATUSES = {"resolved", "closed"}


def _parse_uuid_or_404(value: str) -> uuid_mod.UUID:
    try:
        return uuid_mod.UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )


def _to_detail(ticket: Ticket) -> TicketDetailOut:
    timeline = [
        TimelineEntryOut(
            status_from=c.status_from,
            status_to=c.status_to,
            comment=c.comment,
            author_id=c.author_id,
            created_at=c.created_at,
        )
        for c in ticket.comments
        if c.status_to is not None
    ]
    return TicketDetailOut(
        id=ticket.id,
        ticket_number=ticket.ticket_number,
        drone_id=ticket.drone_id,
        category=ticket.category,
        sub_category=ticket.sub_category,
        priority=ticket.priority,
        subject=ticket.subject,
        description=ticket.description,
        status=ticket.status,
        issue_datetime=ticket.issue_datetime,
        location=ticket.location,
        flight_hours_at_issue=(
            float(ticket.flight_hours_at_issue) if ticket.flight_hours_at_issue is not None else None
        ),
        assigned_engineer_id=ticket.assigned_engineer_id,
        resolved_at=ticket.resolved_at,
        closed_at=ticket.closed_at,
        customer_confirmed_resolution=ticket.customer_confirmed_resolution,
        created_at=ticket.created_at,
        updated_at=ticket.updated_at,
        timeline=timeline,
        attachments=[TicketAttachmentOut.model_validate(a) for a in ticket.attachments],
        comments=[TicketCommentOut.model_validate(c) for c in ticket.comments if not c.is_internal],
    )


@router.get("")
def list_tickets(
    status_filter: str | None = Query(default=None, alias="status"),
    drone_id: str | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    query = db.query(Ticket).filter(Ticket.customer_id == customer.id)
    if status_filter == "open":
        query = query.filter(~Ticket.status.in_(list(CLOSED_STATUSES)))
    elif status_filter == "closed":
        query = query.filter(Ticket.status.in_(list(CLOSED_STATUSES)))
    elif status_filter:
        query = query.filter(Ticket.status == status_filter)
    if drone_id:
        query = query.filter(Ticket.drone_id == _parse_uuid_or_404(drone_id))

    total = query.count()
    items = query.order_by(Ticket.created_at.desc()).offset((page - 1) * page_size).limit(page_size).all()
    return paginate([TicketOut.model_validate(i) for i in items], total, page, page_size)


@router.get("/{ticket_id}", response_model=TicketDetailOut)
def get_ticket(
    ticket_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    ticket = get_owned_ticket_or_404(db, _parse_uuid_or_404(ticket_id), customer.id)
    return _to_detail(ticket)


@router.post("", response_model=TicketOut, status_code=status.HTTP_201_CREATED)
def create_ticket(
    payload: TicketCreate,
    customer: Customer = Depends(get_current_customer),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    drone = db.query(Drone).filter(Drone.id == payload.drone_id, Drone.customer_id == customer.id).first()
    if drone is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )

    ticket = Ticket(
        ticket_number=generate_ticket_number(db),
        customer_id=customer.id,
        drone_id=drone.id,
        category=payload.category,
        sub_category=payload.sub_category,
        priority=payload.priority,
        subject=payload.subject,
        description=payload.description,
        status="new",
        issue_datetime=payload.issue_datetime,
        location=payload.location,
        flight_hours_at_issue=payload.flight_hours_at_issue,
    )
    db.add(ticket)
    db.flush()

    add_comment(
        db,
        ticket,
        author_id=current_user.id,
        comment="Ticket created.",
        is_internal=False,
    )

    create_notification(
        db,
        user=current_user,
        type_="ticket_created",
        title="Ticket created",
        body=f"Your ticket {ticket.ticket_number} ({ticket.subject}) has been created.",
        related_entity_type="ticket",
        related_entity_id=ticket.id,
    )

    db.commit()
    db.refresh(ticket)
    return ticket


@router.post("/{ticket_id}/comments", response_model=TicketCommentOut, status_code=status.HTTP_201_CREATED)
def add_ticket_comment(
    ticket_id: str,
    payload: TicketCommentCreate,
    customer: Customer = Depends(get_current_customer),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    ticket = get_owned_ticket_or_404(db, _parse_uuid_or_404(ticket_id), customer.id)
    comment = add_comment(db, ticket, author_id=current_user.id, comment=payload.comment, is_internal=False)

    if ticket.assigned_engineer_id is not None:
        engineer = db.get(User, ticket.assigned_engineer_id)
        if engineer is not None:
            create_notification(
                db,
                user=engineer,
                type_="engineer_comment",
                title="New customer comment",
                body=f"New comment on ticket {ticket.ticket_number}.",
                related_entity_type="ticket",
                related_entity_id=ticket.id,
            )

    db.commit()
    db.refresh(comment)
    return comment


@router.post(
    "/{ticket_id}/attachments", response_model=TicketAttachmentOut, status_code=status.HTTP_201_CREATED
)
def add_ticket_attachment(
    ticket_id: str,
    file: UploadFile = File(...),
    file_type: str = Form(...),
    customer: Customer = Depends(get_current_customer),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    if file_type not in ("photo", "video", "document", "log"):
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "invalid file_type"}},
        )
    ticket = get_owned_ticket_or_404(db, _parse_uuid_or_404(ticket_id), customer.id)
    file_url, size_bytes = save_upload(file, subdir=f"tickets/{ticket.id}")

    attachment = TicketAttachment(
        ticket_id=ticket.id,
        uploaded_by=current_user.id,
        file_url=file_url,
        file_type=file_type,
        file_name=file.filename or "upload",
        file_size_bytes=size_bytes,
    )
    db.add(attachment)
    db.commit()
    db.refresh(attachment)
    return attachment


@router.post("/{ticket_id}/close", response_model=TicketOut)
def close_ticket(
    ticket_id: str,
    payload: TicketCloseRequest,
    customer: Customer = Depends(get_current_customer),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    from datetime import datetime, timezone

    ticket = get_owned_ticket_or_404(db, _parse_uuid_or_404(ticket_id), customer.id)
    if ticket.status != "resolved":
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail={
                "error": {
                    "code": "INVALID_STATE",
                    "message": "Ticket can only be closed once it has been resolved",
                }
            },
        )

    previous_status = ticket.status
    ticket.status = "closed"
    ticket.customer_confirmed_resolution = bool(payload.confirmed)
    ticket.closed_at = datetime.now(timezone.utc)
    db.add(ticket)

    comment_text = payload.feedback or "Customer confirmed resolution and closed the ticket."
    from app.services.ticket_service import add_status_transition

    add_status_transition(
        db,
        ticket,
        author_id=current_user.id,
        status_from=previous_status,
        status_to="closed",
        comment=comment_text,
    )

    create_notification(
        db,
        user=current_user,
        type_="ticket_closed",
        title="Ticket closed",
        body=f"Ticket {ticket.ticket_number} has been closed.",
        related_entity_type="ticket",
        related_entity_id=ticket.id,
    )

    db.commit()
    db.refresh(ticket)
    return ticket
