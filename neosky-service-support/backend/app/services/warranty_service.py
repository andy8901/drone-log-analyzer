"""Warranty status derivation."""

from datetime import date, timedelta

EXPIRING_SOON_WINDOW_DAYS = 30


def derive_warranty_status(end_date: date, today: date | None = None) -> str:
    today = today or date.today()
    if end_date < today:
        return "expired"
    if end_date <= today + timedelta(days=EXPIRING_SOON_WINDOW_DAYS):
        return "expiring_soon"
    return "active"


def days_remaining(end_date: date, today: date | None = None) -> int:
    today = today or date.today()
    return (end_date - today).days
