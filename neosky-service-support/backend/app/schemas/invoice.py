import uuid
from datetime import date, datetime

from pydantic import BaseModel


class InvoiceItemOut(BaseModel):
    id: uuid.UUID
    description: str
    quantity: float
    unit_price: float
    amount: float

    model_config = {"from_attributes": True}


class InvoiceItemCreate(BaseModel):
    description: str
    quantity: float = 1
    unit_price: float


class PaymentOut(BaseModel):
    id: uuid.UUID
    amount: float
    payment_date: datetime
    payment_method: str | None = None
    reference_number: str | None = None

    model_config = {"from_attributes": True}


class InvoiceOut(BaseModel):
    id: uuid.UUID
    invoice_number: str
    drone_id: uuid.UUID | None = None
    invoice_date: date
    product_service: str
    subtotal_amount: float
    gst_amount: float
    total_amount: float
    payment_status: str
    warranty_type: str
    pdf_url: str | None = None

    model_config = {"from_attributes": True}


class InvoiceDetailOut(InvoiceOut):
    invoice_items: list[InvoiceItemOut] = []
    payments: list[PaymentOut] = []


class InvoiceCreate(BaseModel):
    customer_id: uuid.UUID
    drone_id: uuid.UUID | None = None
    invoice_date: date
    product_service: str
    warranty_type: str = "non_warranty"
    items: list[InvoiceItemCreate]
    gst_rate: float = 18.0
