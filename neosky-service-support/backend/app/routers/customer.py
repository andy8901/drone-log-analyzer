from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.database import get_db
from app.models.customer import Customer
from app.schemas.customer import CustomerProfileOut, CustomerProfileUpdate, FcmTokenUpdate

router = APIRouter(prefix="/api/customer", tags=["customer"])


def _to_profile_out(customer: Customer) -> CustomerProfileOut:
    return CustomerProfileOut(
        id=customer.id,
        customer_code=customer.customer_code,
        full_name=customer.user.full_name,
        email=customer.user.email,
        phone=customer.user.phone,
        company_name=customer.company_name,
        billing_address=customer.billing_address,
        gstin=customer.gstin,
    )


@router.get("/profile", response_model=CustomerProfileOut)
def get_profile(customer: Customer = Depends(get_current_customer)):
    return _to_profile_out(customer)


@router.put("/profile", response_model=CustomerProfileOut)
def update_profile(
    payload: CustomerProfileUpdate,
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    data = payload.model_dump(exclude_unset=True)
    if "full_name" in data and data["full_name"] is not None:
        customer.user.full_name = data["full_name"]
    if "phone" in data and data["phone"] is not None:
        customer.user.phone = data["phone"]
    if "fcm_token" in data and data["fcm_token"] is not None:
        customer.user.fcm_token = data["fcm_token"]
    if "company_name" in data:
        customer.company_name = data["company_name"]
    if "billing_address" in data:
        customer.billing_address = data["billing_address"]
    if "gstin" in data:
        customer.gstin = data["gstin"]
    db.add(customer)
    db.add(customer.user)
    db.commit()
    db.refresh(customer)
    return _to_profile_out(customer)


@router.put("/fcm-token", response_model=CustomerProfileOut)
def update_fcm_token(
    payload: FcmTokenUpdate,
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    customer.user.fcm_token = payload.fcm_token
    db.add(customer.user)
    db.commit()
    db.refresh(customer)
    return _to_profile_out(customer)
