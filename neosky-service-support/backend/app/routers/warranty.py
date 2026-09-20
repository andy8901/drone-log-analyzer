import uuid as uuid_mod

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.core.ownership import get_owned_drone_or_404
from app.database import get_db
from app.models.customer import Customer
from app.models.warranty import Warranty
from app.schemas.warranty import WarrantyClaimOut, WarrantyOut
from app.services.warranty_service import days_remaining, derive_warranty_status

router = APIRouter(prefix="/api/drones", tags=["warranty"])


@router.get("/{drone_id}/warranty", response_model=WarrantyOut)
def get_warranty(
    drone_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    try:
        drone_uuid = uuid_mod.UUID(drone_id)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    drone = get_owned_drone_or_404(db, drone_uuid, customer.id)
    warranty = db.query(Warranty).filter(Warranty.drone_id == drone.id).first()
    if warranty is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    return WarrantyOut(
        drone_id=drone.id,
        start_date=warranty.start_date,
        end_date=warranty.end_date,
        days_remaining=days_remaining(warranty.end_date),
        status=derive_warranty_status(warranty.end_date),
        covered_items=warranty.covered_items,
        excluded_items=warranty.excluded_items,
        claims=[WarrantyClaimOut.model_validate(c) for c in warranty.claims],
    )
