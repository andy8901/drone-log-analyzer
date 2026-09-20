"""Pure-function tests for flight duration computation and maintenance/
warranty status derivation. No DB needed."""

from datetime import date, datetime, timedelta, timezone

from app.services.flight_service import compute_duration_minutes
from app.services.maintenance_service import derive_maintenance_status
from app.services.warranty_service import derive_warranty_status


def test_compute_duration_minutes_basic():
    start = datetime(2026, 9, 20, 9, 0, 0, tzinfo=timezone.utc)
    end = datetime(2026, 9, 20, 9, 32, 0, tzinfo=timezone.utc)
    assert compute_duration_minutes(start, end) == 32.0


def test_compute_duration_minutes_with_seconds_rounds_to_2dp():
    start = datetime(2026, 9, 20, 9, 0, 0, tzinfo=timezone.utc)
    end = start + timedelta(minutes=12, seconds=45)
    assert compute_duration_minutes(start, end) == 12.75


def test_compute_duration_minutes_zero_length():
    start = datetime(2026, 9, 20, 9, 0, 0, tzinfo=timezone.utc)
    assert compute_duration_minutes(start, start) == 0.0


def test_compute_duration_minutes_across_midnight():
    start = datetime(2026, 9, 20, 23, 50, 0, tzinfo=timezone.utc)
    end = datetime(2026, 9, 21, 0, 10, 0, tzinfo=timezone.utc)
    assert compute_duration_minutes(start, end) == 20.0


def test_warranty_status_active():
    today = date(2026, 1, 1)
    assert derive_warranty_status(date(2027, 1, 1), today=today) == "active"


def test_warranty_status_expiring_soon():
    today = date(2026, 1, 1)
    assert derive_warranty_status(date(2026, 1, 20), today=today) == "expiring_soon"


def test_warranty_status_expired():
    today = date(2026, 1, 1)
    assert derive_warranty_status(date(2025, 12, 31), today=today) == "expired"


def test_maintenance_status_overdue_by_hours():
    assert (
        derive_maintenance_status(current_flight_hours=60, next_due_hours=50, next_due_date=None) == "overdue"
    )


def test_maintenance_status_overdue_by_date():
    today = date(2026, 9, 20)
    assert (
        derive_maintenance_status(
            current_flight_hours=10, next_due_hours=None, next_due_date=date(2026, 9, 1), today=today
        )
        == "overdue"
    )


def test_maintenance_status_due_soon():
    assert (
        derive_maintenance_status(current_flight_hours=40, next_due_hours=50, next_due_date=None)
        == "due_soon"
    )


def test_maintenance_status_upcoming():
    assert (
        derive_maintenance_status(current_flight_hours=5, next_due_hours=50, next_due_date=None) == "upcoming"
    )


def test_maintenance_status_due():
    assert derive_maintenance_status(current_flight_hours=47, next_due_hours=50, next_due_date=None) == "due"
