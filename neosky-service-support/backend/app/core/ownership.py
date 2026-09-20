"""Structurally-hard-to-get-wrong "owned or 404" helpers.

Every customer-facing endpoint that takes a resource id must go through one
of these instead of writing an ad hoc query, so that a row belonging to
another customer always 404s (never 403, never leaks existence).
"""

import uuid

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.models.drone import Drone
from app.models.flight import FlightLog
from app.models.invoice import Invoice
from app.models.ticket import Ticket

NOT_FOUND = HTTPException(
    status_code=status.HTTP_404_NOT_FOUND,
    detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
)


def get_owned_drone_or_404(db: Session, drone_id: uuid.UUID, customer_id: uuid.UUID) -> Drone:
    drone = db.query(Drone).filter(Drone.id == drone_id, Drone.customer_id == customer_id).first()
    if drone is None:
        raise NOT_FOUND
    return drone


def get_owned_ticket_or_404(db: Session, ticket_id: uuid.UUID, customer_id: uuid.UUID) -> Ticket:
    ticket = db.query(Ticket).filter(Ticket.id == ticket_id, Ticket.customer_id == customer_id).first()
    if ticket is None:
        raise NOT_FOUND
    return ticket


def get_owned_invoice_or_404(db: Session, invoice_id: uuid.UUID, customer_id: uuid.UUID) -> Invoice:
    invoice = db.query(Invoice).filter(Invoice.id == invoice_id, Invoice.customer_id == customer_id).first()
    if invoice is None:
        raise NOT_FOUND
    return invoice


def get_owned_flight_or_404(db: Session, flight_id: uuid.UUID, customer_id: uuid.UUID) -> FlightLog:
    flight = (
        db.query(FlightLog).filter(FlightLog.id == flight_id, FlightLog.customer_id == customer_id).first()
    )
    if flight is None:
        raise NOT_FOUND
    return flight
