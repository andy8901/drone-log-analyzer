"""Maintenance due/overdue derivation from flight hours & calendar interval."""

from datetime import date, timedelta

DUE_HOURS_THRESHOLD = 5
DUE_SOON_HOURS_THRESHOLD = 15
DUE_DAYS_THRESHOLD = 7
DUE_SOON_DAYS_THRESHOLD = 30


def compute_next_due(
    last_maintenance_hours: float | None,
    last_maintenance_date: date | None,
    interval_flight_hours: float | None,
    interval_calendar_days: int | None,
) -> tuple[float | None, date | None]:
    next_due_hours = None
    if last_maintenance_hours is not None and interval_flight_hours is not None:
        next_due_hours = round(float(last_maintenance_hours) + float(interval_flight_hours), 2)

    next_due_date = None
    if last_maintenance_date is not None and interval_calendar_days is not None:
        next_due_date = last_maintenance_date + timedelta(days=interval_calendar_days)

    return next_due_hours, next_due_date


def derive_maintenance_status(
    current_flight_hours: float,
    next_due_hours: float | None,
    next_due_date: date | None,
    today: date | None = None,
) -> str:
    today = today or date.today()

    remaining_hours = None
    if next_due_hours is not None:
        remaining_hours = round(float(next_due_hours) - float(current_flight_hours), 2)

    remaining_days = None
    if next_due_date is not None:
        remaining_days = (next_due_date - today).days

    # Overdue: past due by hours or by date.
    if remaining_hours is not None and remaining_hours <= 0:
        return "overdue"
    if remaining_days is not None and remaining_days < 0:
        return "overdue"

    # Due: close by hours or by date.
    if remaining_hours is not None and remaining_hours <= DUE_HOURS_THRESHOLD:
        return "due"
    if remaining_days is not None and remaining_days <= DUE_DAYS_THRESHOLD:
        return "due"

    # Due soon.
    if remaining_hours is not None and remaining_hours <= DUE_SOON_HOURS_THRESHOLD:
        return "due_soon"
    if remaining_days is not None and remaining_days <= DUE_SOON_DAYS_THRESHOLD:
        return "due_soon"

    if remaining_hours is None and remaining_days is None:
        return "upcoming"

    return "upcoming"
