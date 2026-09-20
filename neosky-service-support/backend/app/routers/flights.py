import uuid as uuid_mod
from datetime import date

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session

from app.core.customer_scope import get_current_customer
from app.core.ownership import get_owned_drone_or_404, get_owned_flight_or_404
from app.core.security import get_current_user
from app.database import get_db
from app.models.customer import Customer
from app.models.drone import Drone
from app.models.flight import FlightLog
from app.models.user import User
from app.schemas.common import paginate
from app.schemas.flight import (
    FlightCreate,
    FlightOut,
    FlightStatsOut,
    FlightUpdate,
    HoursPerDroneOut,
    HoursPerPilotOut,
    MonthlyHoursOut,
)
from app.services.flight_service import (
    compute_duration_minutes,
    find_by_client_uuid,
    recompute_drone_flight_stats,
)

router = APIRouter(prefix="/api/flights", tags=["flights"])


def _parse_uuid_or_404(value: str) -> uuid_mod.UUID:
    try:
        return uuid_mod.UUID(value)
    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
        )


@router.get("")
def list_flights(
    drone_id: str | None = Query(default=None),
    from_: date | None = Query(default=None, alias="from"),
    to: date | None = Query(default=None),
    page: int = Query(default=1, ge=1),
    page_size: int = Query(default=20, ge=1, le=100),
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    query = db.query(FlightLog).filter(FlightLog.customer_id == customer.id)
    if drone_id:
        query = query.filter(FlightLog.drone_id == _parse_uuid_or_404(drone_id))
    if from_:
        query = query.filter(FlightLog.flight_date >= from_)
    if to:
        query = query.filter(FlightLog.flight_date <= to)

    total = query.count()
    items = (
        query.order_by(FlightLog.flight_date.desc(), FlightLog.start_time.desc())
        .offset((page - 1) * page_size)
        .limit(page_size)
        .all()
    )
    return paginate([FlightOut.model_validate(i) for i in items], total, page, page_size)


@router.get("/stats", response_model=FlightStatsOut)
def flight_stats(customer: Customer = Depends(get_current_customer), db: Session = Depends(get_db)):
    flights = db.query(FlightLog).filter(FlightLog.customer_id == customer.id).all()
    total_flights = len(flights)
    total_minutes = sum(float(f.duration_minutes) for f in flights)
    total_hours = round(total_minutes / 60.0, 2)
    average_duration = round(total_minutes / total_flights, 2) if total_flights else 0.0

    monthly: dict[str, float] = {}
    for f in flights:
        key = f.flight_date.strftime("%Y-%m")
        monthly[key] = monthly.get(key, 0.0) + float(f.duration_minutes) / 60.0
    monthly_flight_hours = [MonthlyHoursOut(month=k, hours=round(v, 2)) for k, v in sorted(monthly.items())]

    drones_by_id = {d.id: d for d in db.query(Drone).filter(Drone.customer_id == customer.id).all()}
    hours_per_drone_map: dict[uuid_mod.UUID, float] = {}
    for f in flights:
        hours_per_drone_map[f.drone_id] = (
            hours_per_drone_map.get(f.drone_id, 0.0) + float(f.duration_minutes) / 60.0
        )
    hours_per_drone = [
        HoursPerDroneOut(drone_id=did, drone_name=drones_by_id[did].drone_name, hours=round(h, 2))
        for did, h in hours_per_drone_map.items()
        if did in drones_by_id
    ]

    pilot_ids = {f.pilot_id for f in flights}
    pilots_by_id = {u.id: u for u in db.query(User).filter(User.id.in_(pilot_ids)).all()} if pilot_ids else {}
    hours_per_pilot_map: dict[uuid_mod.UUID, float] = {}
    for f in flights:
        hours_per_pilot_map[f.pilot_id] = (
            hours_per_pilot_map.get(f.pilot_id, 0.0) + float(f.duration_minutes) / 60.0
        )
    hours_per_pilot = [
        HoursPerPilotOut(pilot_id=pid, pilot_name=pilots_by_id[pid].full_name, hours=round(h, 2))
        for pid, h in hours_per_pilot_map.items()
        if pid in pilots_by_id
    ]

    return FlightStatsOut(
        total_flights=total_flights,
        total_flight_hours=total_hours,
        monthly_flight_hours=monthly_flight_hours,
        average_duration_minutes=average_duration,
        hours_per_drone=hours_per_drone,
        hours_per_pilot=hours_per_pilot,
    )


@router.post("", response_model=FlightOut, status_code=status.HTTP_201_CREATED)
def create_flight(
    payload: FlightCreate,
    customer: Customer = Depends(get_current_customer),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    drone = get_owned_drone_or_404(db, payload.drone_id, customer.id)

    existing = find_by_client_uuid(db, payload.client_uuid)
    if existing is not None:
        if existing.customer_id != customer.id:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail={"error": {"code": "NOT_FOUND", "message": "Not found"}},
            )
        return existing  # idempotent retry (offline sync)

    duration_minutes = payload.duration_minutes
    if duration_minutes is None:
        duration_minutes = compute_duration_minutes(payload.start_time, payload.end_time)

    flight = FlightLog(
        client_uuid=payload.client_uuid,
        drone_id=drone.id,
        customer_id=customer.id,
        pilot_id=current_user.id,
        flight_date=payload.start_time.date(),
        start_time=payload.start_time,
        end_time=payload.end_time,
        duration_minutes=duration_minutes,
        location=payload.location,
        max_altitude_m=payload.max_altitude_m,
        distance_travelled_km=payload.distance_travelled_km,
        mission_type=payload.mission_type,
        payload_used=payload.payload_used,
        battery_used=payload.battery_used,
        battery_cycle=payload.battery_cycle,
        weather=payload.weather,
        flight_result=payload.flight_result,
        remarks=payload.remarks,
        incident_flag=payload.incident_flag,
        sync_status="synced",
    )
    db.add(flight)
    db.flush()
    recompute_drone_flight_stats(db, drone.id)
    db.commit()
    db.refresh(flight)
    return flight


@router.put("/{flight_id}", response_model=FlightOut)
def update_flight(
    flight_id: str,
    payload: FlightUpdate,
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    flight = get_owned_flight_or_404(db, _parse_uuid_or_404(flight_id), customer.id)
    data = payload.model_dump(exclude_unset=True)
    for field, value in data.items():
        setattr(flight, field, value)
    if "start_time" in data or "end_time" in data:
        if payload.duration_minutes is None:
            flight.duration_minutes = compute_duration_minutes(flight.start_time, flight.end_time)
        flight.flight_date = flight.start_time.date()
    db.add(flight)
    db.flush()
    recompute_drone_flight_stats(db, flight.drone_id)
    db.commit()
    db.refresh(flight)
    return flight


@router.delete("/{flight_id}", status_code=status.HTTP_204_NO_CONTENT)
def delete_flight(
    flight_id: str,
    customer: Customer = Depends(get_current_customer),
    db: Session = Depends(get_db),
):
    flight = get_owned_flight_or_404(db, _parse_uuid_or_404(flight_id), customer.id)
    drone_id = flight.drone_id
    db.delete(flight)
    db.flush()
    recompute_drone_flight_stats(db, drone_id)
    db.commit()
    return None
