from sqlalchemy.orm import Session

from app.models.customer import Customer
from app.models.drone import Drone
from app.schemas.drone import DroneCreate


def create_drone(db: Session, payload: DroneCreate) -> Drone:
    customer = db.get(Customer, payload.customer_id)
    if customer is None:
        raise ValueError("customer not found")
    drone = Drone(**payload.model_dump())
    db.add(drone)
    db.commit()
    db.refresh(drone)
    return drone


def build_drone_detail(db: Session, drone: Drone):
    from app.schemas.drone import (
        DroneComponentOut,
        DroneDetailOut,
        MaintenanceSummaryOut,
        WarrantySummaryOut,
    )
    from app.services.maintenance_service import derive_maintenance_status
    from app.services.warranty_service import days_remaining, derive_warranty_status

    warranty_summary = None
    if drone.warranty is not None:
        status = derive_warranty_status(drone.warranty.end_date)
        warranty_summary = WarrantySummaryOut(
            status=status,
            end_date=drone.warranty.end_date,
            days_remaining=days_remaining(drone.warranty.end_date),
        )

    maintenance_summary = None
    if drone.maintenance_schedule is not None:
        sched = drone.maintenance_schedule
        current_hours = float(drone.total_flight_hours)
        status = derive_maintenance_status(current_hours, sched.next_due_hours, sched.next_due_date)
        remaining_hours = None
        if sched.next_due_hours is not None:
            remaining_hours = round(float(sched.next_due_hours) - current_hours, 2)
        maintenance_summary = MaintenanceSummaryOut(
            status=status,
            next_due_date=sched.next_due_date,
            next_due_hours=float(sched.next_due_hours) if sched.next_due_hours is not None else None,
            remaining_hours=remaining_hours,
        )

    return DroneDetailOut(
        id=drone.id,
        drone_name=drone.drone_name,
        model=drone.model,
        serial_number=drone.serial_number,
        uin=drone.uin,
        status=drone.status,
        total_flight_hours=float(drone.total_flight_hours),
        total_flights=drone.total_flights,
        last_flight_at=drone.last_flight_at,
        warranty_end_date=drone.warranty_end_date,
        firmware_version=drone.firmware_version,
        purchase_date=drone.purchase_date,
        delivery_date=drone.delivery_date,
        invoice_number=drone.invoice_number,
        invoice_date=drone.invoice_date,
        warranty_start_date=drone.warranty_start_date,
        next_maintenance_due_hours=(
            float(drone.next_maintenance_due_hours) if drone.next_maintenance_due_hours is not None else None
        ),
        next_maintenance_due_date=drone.next_maintenance_due_date,
        last_service_at=drone.last_service_at,
        components=[DroneComponentOut.model_validate(c) for c in drone.components],
        warranty_summary=warranty_summary,
        maintenance_summary=maintenance_summary,
    )
