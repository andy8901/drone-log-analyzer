import uuid
from datetime import date, datetime

from pydantic import BaseModel


class WarrantyClaimOut(BaseModel):
    id: uuid.UUID
    claim_number: str
    ticket_id: uuid.UUID
    status: str

    model_config = {"from_attributes": True}


class WarrantyOut(BaseModel):
    drone_id: uuid.UUID
    start_date: date
    end_date: date
    days_remaining: int
    status: str
    covered_items: list[str]
    excluded_items: list[str]
    claims: list[WarrantyClaimOut] = []


class WarrantyUpdate(BaseModel):
    start_date: date | None = None
    end_date: date | None = None
    covered_items: list[str] | None = None
    excluded_items: list[str] | None = None


class WarrantyClaimCreate(BaseModel):
    ticket_id: uuid.UUID
    description: str | None = None


class WarrantyClaimFullOut(BaseModel):
    id: uuid.UUID
    warranty_id: uuid.UUID
    ticket_id: uuid.UUID
    claim_number: str
    description: str | None = None
    status: str
    claimed_at: datetime
    resolved_at: datetime | None = None

    model_config = {"from_attributes": True}
