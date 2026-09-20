"""Invoice business logic: totals calc + a minimal hand-rolled PDF renderer
(no PDF library is in requirements.txt, so this emits a small but valid
single-page PDF directly)."""

from sqlalchemy.orm import Session

from app.models.invoice import Invoice, InvoiceItem
from app.schemas.invoice import InvoiceCreate


def generate_invoice_number(db: Session) -> str:
    from datetime import datetime, timezone

    year = datetime.now(timezone.utc).year
    prefix = f"INV-{year}-"
    count = db.query(Invoice).filter(Invoice.invoice_number.like(f"{prefix}%")).count()
    seq = count + 1
    while True:
        candidate = f"{prefix}{seq:05d}"
        if db.query(Invoice.id).filter(Invoice.invoice_number == candidate).first() is None:
            return candidate
        seq += 1


def create_invoice(db: Session, payload: InvoiceCreate) -> Invoice:
    subtotal = sum(item.quantity * item.unit_price for item in payload.items)
    gst_amount = round(subtotal * (payload.gst_rate / 100.0), 2)
    total = round(subtotal + gst_amount, 2)

    invoice = Invoice(
        invoice_number=generate_invoice_number(db),
        customer_id=payload.customer_id,
        drone_id=payload.drone_id,
        invoice_date=payload.invoice_date,
        product_service=payload.product_service,
        subtotal_amount=round(subtotal, 2),
        gst_amount=gst_amount,
        total_amount=total,
        payment_status="pending",
        warranty_type=payload.warranty_type,
    )
    db.add(invoice)
    db.flush()

    for item in payload.items:
        db.add(
            InvoiceItem(
                invoice_id=invoice.id,
                description=item.description,
                quantity=item.quantity,
                unit_price=item.unit_price,
                amount=round(item.quantity * item.unit_price, 2),
            )
        )
    db.flush()
    return invoice


def _pdf_escape(text: str) -> str:
    return text.replace("\\", r"\\").replace("(", r"\(").replace(")", r"\)")


def render_invoice_pdf(invoice: Invoice) -> bytes:
    """Produces a minimal, valid, single-page PDF (no external library)."""
    lines = [
        f"Invoice: {invoice.invoice_number}",
        f"Date: {invoice.invoice_date}",
        f"Product/Service: {invoice.product_service}",
        f"Subtotal: {invoice.subtotal_amount}",
        f"GST: {invoice.gst_amount}",
        f"Total: {invoice.total_amount}",
        f"Payment status: {invoice.payment_status}",
    ]
    text_ops = ["BT", "/F1 14 Tf", "50 780 Td"]
    for i, line in enumerate(lines):
        if i > 0:
            text_ops.append("0 -20 Td")
        text_ops.append(f"({_pdf_escape(line)}) Tj")
    text_ops.append("ET")
    content_stream = "\n".join(text_ops).encode("latin-1", errors="replace")

    objects = []
    objects.append(b"<< /Type /Catalog /Pages 2 0 R >>")
    objects.append(b"<< /Type /Pages /Kids [3 0 R] /Count 1 >>")
    objects.append(
        b"<< /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> "
        b"/MediaBox [0 0 612 792] /Contents 5 0 R >>"
    )
    objects.append(b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>")
    stream_obj = b"<< /Length %d >>\nstream\n%s\nendstream" % (len(content_stream), content_stream)
    objects.append(stream_obj)

    pdf = bytearray()
    pdf.extend(b"%PDF-1.4\n")
    offsets = [0]
    for i, obj in enumerate(objects, start=1):
        offsets.append(len(pdf))
        pdf.extend(f"{i} 0 obj\n".encode())
        pdf.extend(obj)
        pdf.extend(b"\nendobj\n")

    xref_offset = len(pdf)
    pdf.extend(f"xref\n0 {len(objects) + 1}\n".encode())
    pdf.extend(b"0000000000 65535 f \n")
    for off in offsets[1:]:
        pdf.extend(f"{off:010d} 00000 n \n".encode())
    pdf.extend(
        f"trailer\n<< /Size {len(objects) + 1} /Root 1 0 R >>\nstartxref\n{xref_offset}\n%%EOF".encode()
    )
    return bytes(pdf)
