"""Flight-log business logic: server-side duration calc, offline-sync
idempotent upsert by client_uuid, and drone flight-stat recomputation."""

import uuid
from datetime import datetime

from sqlalchemy import func
from sqlalchemy.orm import Session

from app.models.drone import Drone
from app.models.flight import FlightLog


def compute_duration_minutes(start_time: datetime, end_time: datetime) -> float:
    """Pure function: minutes between start_time and end_time, rounded to 2dp."""
    delta = end_time - start_time
    minutes = delta.total_seconds() / 60.0
    return round(minutes, 2)


def find_by_client_uuid(db: Session, client_uuid: uuid.UUID | None) -> FlightLog | None:
    if client_uuid is None:
        return None
    return db.query(FlightLog).filter(FlightLog.client_uuid == client_uuid).first()


def recompute_drone_flight_stats(db: Session, drone_id: uuid.UUID) -> None:
    drone = db.get(Drone, drone_id)
    if drone is None:
        return
    total_minutes, total_flights, last_end = (
        db.query(
            func.coalesce(func.sum(FlightLog.duration_minutes), 0),
            func.count(FlightLog.id),
            func.max(FlightLog.end_time),
        )
        .filter(FlightLog.drone_id == drone_id)
        .one()
    )
    drone.total_flight_hours = round(float(total_minutes) / 60.0, 2)
    drone.total_flights = int(total_flights)
    drone.last_flight_at = last_end
    db.add(drone)
    db.flush()
