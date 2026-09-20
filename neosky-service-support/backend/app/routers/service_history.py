import uuid as uuid_mod

from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.core.ownership import get_owned_drone_or_404
from app.database import get_db
from app.models.customer import Customer
from app.models.service_record import ServiceRecord
from app.schemas.service_record import ServiceRecordOut

router = APIRouter(prefix="/api/drones", tags=["service-history"])


@router.get("/{drone_id}/service-history", response_model=list[ServiceRecordOut])
def get_service_history(
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
    records = (
        db.query(ServiceRecord)
        .filter(ServiceRecord.drone_id == drone.id)
        .order_by(ServiceRecord.service_date.desc())
        .all()
    )
    return records
