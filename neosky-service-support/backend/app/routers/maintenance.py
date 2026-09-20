import uuid as uuid_mod
from datetime import datetime, timezone

from fastapi import APIRouter, Depends, HTTPException, Request, status
from sqlalchemy.orm import Session

from app.core.audit import write_audit_log
from app.core.customer_scope import get_current_customer
from app.core.ownership import get_owned_drone_or_404
from app.core.security import require_role
from app.database import get_db
from app.models.customer import Customer
from app.models.drone import Drone
from app.models.maintenance import MaintenanceRecord, MaintenanceSchedule
from app.models.service_record import ServiceRecord
from app.schemas.maintenance import (
    MaintenanceOut,
    MaintenanceRecordCreate,
    MaintenanceRecordOut,
    MaintenanceScheduleOut,
)
from app.services.maintenance_service import derive_maintenance_status
from app.services.notification_service import create_notification

router = APIRouter(prefix="/api/drones", tags=["maintenance"])


def _parse_uuid_or_404(value: str) -> uuid_mod.UUID:
    try:
        return uuid_mod.UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )


@router.get("/{drone_id}/maintenance", response_model=MaintenanceOut)
def get_maintenance(
    drone_id: str, customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)
):
    drone = get_owned_drone_or_404(db, _parse_uuid_or_404(drone_id), customer.id)
    schedule = db.query(MaintenanceSchedule).filter(MaintenanceSchedule.drone_id == drone.id).first()

    schedule_out = None
    if schedule is not None:
        current_hours = float(drone.total_flight_hours)
        status_ = derive_maintenance_status(current_hours, schedule.next_due_hours, schedule.next_due_date)
        remaining_hours = None
        if schedule.next_due_hours is not None:
            remaining_hours = round(float(schedule.next_due_hours) - current_hours, 2)
        schedule_out = MaintenanceScheduleOut(
            interval_flight_hours=(
                float(schedule.interval_flight_hours) if schedule.interval_flight_hours is not None else None
            ),
            interval_calendar_days=schedule.interval_calendar_days,
            last_maintenance_date=schedule.last_maintenance_date,
            current_flight_hours=current_hours,
            next_due_hours=(float(schedule.next_due_hours) if schedule.next_due_hours is not None else None),
            remaining_hours=remaining_hours,
            next_due_date=schedule.next_due_date,
            status=status_,
        )

    records = (
        db.query(MaintenanceRecord)
        .filter(MaintenanceRecord.drone_id == drone.id)
        .order_by(MaintenanceRecord.performed_at.desc())
        .all()
    )
    return MaintenanceOut(
        schedule=schedule_out, records=[MaintenanceRecordOut.model_validate(r) for r in records]
    )


@router.post(
    "/{drone_id}/maintenance", response_model=MaintenanceRecordOut, status_code=status.HTTP_201_CREATED
)
def log_maintenance(
    drone_id: str,
    payload: MaintenanceRecordCreate,
    request: Request,
    current_user=Depends(require_role("service_engineer", "admin")),
    db: Session = Depends(get_db),
):
    drone_uuid = _parse_uuid_or_404(drone_id)
    drone = db.get(Drone, drone_uuid)
    if drone is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )

    performed_at = payload.performed_at or datetime.now(timezone.utc)
    schedule = db.query(MaintenanceSchedule).filter(MaintenanceSchedule.drone_id == drone.id).first()

    record = MaintenanceRecord(
        drone_id=drone.id,
        maintenance_schedule_id=schedule.id if schedule else None,
        ticket_id=payload.ticket_id,
        performed_by=current_user.id,
        maintenance_type=payload.maintenance_type,
        flight_hours_at_service=payload.flight_hours_at_service,
        description=payload.description,
        parts_replaced=payload.parts_replaced,
        performed_at=performed_at,
        status="completed",
    )
    db.add(record)
    db.flush()

    if schedule is not None:
        schedule.last_maintenance_date = (
            performed_at.date() if isinstance(performed_at, datetime) else performed_at
        )
        schedule.last_maintenance_hours = payload.flight_hours_at_service or drone.total_flight_hours
        if schedule.interval_flight_hours is not None:
            schedule.next_due_hours = float(schedule.last_maintenance_hours) + float(
                schedule.interval_flight_hours
            )
        if schedule.interval_calendar_days is not None:
            from datetime import timedelta

            schedule.next_due_date = schedule.last_maintenance_date + timedelta(
                days=schedule.interval_calendar_days
            )
        schedule.status = "completed"
        db.add(schedule)

    drone.last_service_at = performed_at
    db.add(drone)

    service_record = ServiceRecord(
        drone_id=drone.id,
        ticket_id=payload.ticket_id,
        maintenance_record_id=record.id,
        service_date=performed_at.date() if isinstance(performed_at, datetime) else performed_at,
        issue_summary=payload.maintenance_type,
        action_taken=payload.description,
        performed_by=current_user.id,
        status="completed",
    )
    db.add(service_record)

    write_audit_log(
        db,
        user_id=current_user.id,
        action="log_maintenance",
        entity_type="maintenance_records",
        entity_id=record.id,
        ip_address=request.client.host if request.client else None,
        metadata={"drone_id": str(drone.id)},
    )

    customer = drone.customer
    create_notification(
        db,
        user=customer.user,
        type_="service_completed",
        title="Maintenance completed",
        body=f"Maintenance ({payload.maintenance_type}) completed for {drone.drone_name}.",
        related_entity_type="drone",
        related_entity_id=drone.id,
    )

    db.commit()
    db.refresh(record)
    return record
