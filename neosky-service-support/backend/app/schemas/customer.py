import uuid

from pydantic import BaseModel


class CustomerProfileOut(BaseModel):
    id: uuid.UUID
    customer_code: str
    full_name: str
    email: str
    phone: str | None = None
    company_name: str | None = None
    billing_address: str | None = None
    gstin: str | None = None

    model_config = {"from_attributes": True}


class CustomerProfileUpdate(BaseModel):
    full_name: str | None = None
    phone: str | None = None
    company_name: str | None = None
    billing_address: str | None = None
    gstin: str | None = None
    fcm_token: str | None = None


class FcmTokenUpdate(BaseModel):
    fcm_token: str
