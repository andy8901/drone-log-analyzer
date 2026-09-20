import uuid as uuid_mod

from fastapi import APIRouter, Depends, HTTPException, Query, Response, status
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.core.ownership import get_owned_invoice_or_404
from app.database import get_db
from app.models.customer import Customer
from app.models.invoice import Invoice
from app.schemas.common import paginate
from app.schemas.invoice import InvoiceDetailOut, InvoiceOut
from app.services.invoice_service import render_invoice_pdf

router = APIRouter(prefix="/api/invoices", tags=["invoices"])


def _parse_uuid_or_404(value: str) -> uuid_mod.UUID:
    try:
        return uuid_mod.UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )


@router.get("")
def list_invoices(
    status_filter: str | None = Query(default=None, alias="status"),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    query = db.query(Invoice).filter(Invoice.customer_id == customer.id)
    if status_filter:
        query = query.filter(Invoice.payment_status == status_filter)
    total = query.count()
    items = query.order_by(Invoice.invoice_date.desc()).offset((page - 1) * page_size).limit(page_size).all()
    return paginate([InvoiceOut.model_validate(i) for i in items], total, page, page_size)


@router.get("/{invoice_id}", response_model=InvoiceDetailOut)
def get_invoice(
    invoice_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    invoice = get_owned_invoice_or_404(db, _parse_uuid_or_404(invoice_id), customer.id)
    return invoice


@router.get("/{invoice_id}/pdf")
def get_invoice_pdf(
    invoice_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    invoice = get_owned_invoice_or_404(db, _parse_uuid_or_404(invoice_id), customer.id)
    pdf_bytes = render_invoice_pdf(invoice)
    return Response(
        content=pdf_bytes,
        media_type="application/pdf",
        headers={"Content-Disposition": f'attachment; filename="{invoice.invoice_number}.pdf"'},
    )
