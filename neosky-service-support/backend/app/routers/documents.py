import os
import uuid as uuid_mod

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy import or_
from sqlalchemy.orm import Session

from app.config import settings
from app.core.customer_scope import get_current_customer
from app.database import get_db
from app.models.customer import Customer
from app.models.document import Document
from app.models.drone import Drone
from app.models.ticket import Ticket
from app.schemas.document import DocumentOut

router = APIRouter(prefix="/api/documents", tags=["documents"])


def _owned_document_filter(db: Session, customer: Customer):
    drone_ids = [d.id for d in db.query(Drone.id).filter(Drone.customer_id == customer.id).all()]
    ticket_ids = [t.id for t in db.query(Ticket.id).filter(Ticket.customer_id == customer.id).all()]
    clauses = [Document.customer_id == customer.id]
    if drone_ids:
        clauses.append(Document.drone_id.in_(drone_ids))
    if ticket_ids:
        clauses.append(Document.ticket_id.in_(ticket_ids))
    return or_(*clauses)


@router.get("", response_model=list[DocumentOut])
def list_documents(
    drone_id: str | None = Query(default=None),
    ticket_id: str | None = Query(default=None),
    type: str | None = Query(default=None),
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    query = db.query(Document).filter(_owned_document_filter(db, customer))
    if drone_id:
        try:
            query = query.filter(Document.drone_id == uuid_mod.UUID(drone_id))
        except ValueError:
            return []
    if ticket_id:
        try:
            query = query.filter(Document.ticket_id == uuid_mod.UUID(ticket_id))
        except ValueError:
            return []
    if type:
        query = query.filter(Document.document_type == type)
    return query.order_by(Document.created_at.desc()).all()


@router.get("/{document_id}/download")
def download_document(
    document_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    try:
        doc_uuid = uuid_mod.UUID(document_id)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    document = (
        db.query(Document)
        .filter(Document.id == doc_uuid)
        .filter(_owned_document_filter(db, customer))
        .first()
    )
    if document is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )

    if document.file_url.startswith("/uploads/"):
        local_path = os.path.join(settings.UPLOAD_DIR, document.file_url[len("/uploads/") :])
        if os.path.isfile(local_path):
            with open(local_path, "rb") as fh:
                data = fh.read()
            return Response(
                content=data,
                media_type="application/octet-stream",
                headers={"Content-Disposition": f'attachment; filename="{document.file_name}"'},
            )

    # Dummy/seeded file_url values (e.g. seed data) have nothing on disk;
    # signal that with 404 rather than pretending to stream bytes.
    raise HTTPException(
        status_code=status.HTTP_404_NOT_FOUND,
        detail={"error": {"code": "FILE_NOT_FOUND", "message": "Underlying file not found"}},
    )
