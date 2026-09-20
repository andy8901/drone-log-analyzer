"""The single choke point every customer-scoped router uses to resolve the
authenticated user's `customers` row. `customer_id` must NEVER be trusted
from client input (path/query/body) — it is always derived here, from the
JWT-authenticated user.
"""

from fastapi import Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.security import get_current_user
from app.database import get_db
from app.models.customer import Customer
from app.models.user import User


def get_current_customer(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Customer:
    if current_user.role != "customer":
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    customer = db.query(Customer).filter(Customer.user_id == current_user.id).first()
    if customer is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    return customer
