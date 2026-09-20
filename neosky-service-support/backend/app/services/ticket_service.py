"""Ticket business logic: ticket_number generation and status-transition
timeline entries (written into ticket_comments)."""

import uuid
from datetime import datetime, timezone

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.models.ticket import Ticket, TicketComment


def generate_ticket_number(db: Session) -> str:
    """NS-{year}-{zero-padded 5-digit sequence}, sequence resets per year."""
    year = datetime.now(timezone.utc).year
    prefix = f"NS-{year}-"
    count = db.query(func.count(Ticket.id)).filter(Ticket.ticket_number.like(f"{prefix}%")).scalar() or 0
    seq = count + 1
    # Guard against a rare race with a serial number already taken (unique
    # constraint on ticket_number) by walking forward until free.
    while True:
        candidate = f"{prefix}{seq:05d}"
        exists = db.query(Ticket.id).filter(Ticket.ticket_number == candidate).first()
        if exists is None:
            return candidate
        seq += 1


def add_status_transition(
    db: Session,
    ticket: Ticket,
    *,
    author_id: uuid.UUID,
    status_from: str | None,
    status_to: str,
    comment: str,
    is_internal: bool = False,
) -> TicketComment:
    entry = TicketComment(
        ticket_id=ticket.id,
        author_id=author_id,
        comment=comment,
        status_from=status_from,
        status_to=status_to,
        is_internal=is_internal,
    )
    db.add(entry)
    db.flush()
    return entry


def add_comment(
    db: Session,
    ticket: Ticket,
    *,
    author_id: uuid.UUID,
    comment: str,
    is_internal: bool = False,
) -> TicketComment:
    entry = TicketComment(
        ticket_id=ticket.id,
        author_id=author_id,
        comment=comment,
        status_from=None,
        status_to=None,
        is_internal=is_internal,
    )
    db.add(entry)
    db.flush()
    return entry
