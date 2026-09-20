from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.core.ownership import get_owned_drone_or_404
from app.core.security import require_role
from app.database import get_db
from app.models.customer import Customer
from app.models.drone import Drone, DroneComponent
from app.schemas.drone import DroneCreate, DroneDetailOut, DroneOut
from app.services.drone_service import build_drone_detail, create_drone

router = APIRouter(prefix="/api/drones", tags=["drones"])


@router.get("", response_model=list[DroneOut])
def list_drones(
    status_filter: str | None = Query(default=None, alias="status"),
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    query = db.query(Drone).filter(Drone.customer_id == customer.id)
    if status_filter:
        query = query.filter(Drone.status == status_filter)
    drones = query.order_by(Drone.created_at.desc()).all()
    return drones


@router.get("/search", response_model=list[DroneOut])
def search_drones(
    q: str = Query(...),
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    like = f"%{q}%"
    drones = (
        db.query(Drone)
        .filter(Drone.customer_id == customer.id)
        .filter(
            (Drone.drone_name.ilike(like)) | (Drone.serial_number.ilike(like)) | (Drone.model.ilike(like))
        )
        .all()
    )
    return drones


@router.get("/{drone_id}", response_model=DroneDetailOut)
def get_drone(
    drone_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    import uuid as uuid_mod

    try:
        drone_uuid = uuid_mod.UUID(drone_id)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    drone = get_owned_drone_or_404(db, drone_uuid, customer.id)
    return build_drone_detail(db, drone)


@router.get("/{drone_id}/components")
def get_drone_components(
    drone_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    import uuid as uuid_mod

    from app.schemas.drone import DroneComponentOut

    try:
        drone_uuid = uuid_mod.UUID(drone_id)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )
    drone = get_owned_drone_or_404(db, drone_uuid, customer.id)
    components = db.query(DroneComponent).filter(DroneComponent.drone_id == drone.id).all()
    return [DroneComponentOut.model_validate(c) for c in components]


@router.post("", response_model=DroneDetailOut, status_code=status.HTTP_201_CREATED)
def register_drone(
    payload: DroneCreate,
    current_user=Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    try:
        drone = create_drone(db, payload)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail={"error": {"code": "VALIDATION_ERROR", "message": "customer not found"}},
        )
    return build_drone_detail(db, drone)
