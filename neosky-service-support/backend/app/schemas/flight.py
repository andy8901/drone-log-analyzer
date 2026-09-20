import uuid
from datetime import date, datetime

from pydantic import BaseModel


class FlightCreate(BaseModel):
    client_uuid: uuid.UUID | None = None
    drone_id: uuid.UUID
    start_time: datetime
    end_time: datetime
    duration_minutes: float | None = None
    location: str | None = None
    max_altitude_m: float | None = None
    distance_travelled_km: float | None = None
    mission_type: str | None = None
    payload_used: str | None = None
    battery_used: str | None = None
    battery_cycle: int | None = None
    weather: str | None = None
    flight_result: str = "successful"
    remarks: str | None = None
    incident_flag: bool = False


class FlightUpdate(BaseModel):
    start_time: datetime | None = None
    end_time: datetime | None = None
    duration_minutes: float | None = None
    location: str | None = None
    max_altitude_m: float | None = None
    distance_travelled_km: float | None = None
    mission_type: str | None = None
    payload_used: str | None = None
    battery_used: str | None = None
    battery_cycle: int | None = None
    weather: str | None = None
    flight_result: str | None = None
    remarks: str | None = None
    incident_flag: bool | None = None


class FlightOut(BaseModel):
    id: uuid.UUID
    client_uuid: uuid.UUID | None = None
    drone_id: uuid.UUID
    pilot_id: uuid.UUID
    flight_date: date
    start_time: datetime
    end_time: datetime
    duration_minutes: float
    location: str | None = None
    max_altitude_m: float | None = None
    distance_travelled_km: float | None = None
    mission_type: str | None = None
    payload_used: str | None = None
    battery_used: str | None = None
    battery_cycle: int | None = None
    weather: str | None = None
    flight_result: str
    remarks: str | None = None
    incident_flag: bool
    sync_status: str

    model_config = {"from_attributes": True}


class MonthlyHoursOut(BaseModel):
    month: str
    hours: float


class HoursPerDroneOut(BaseModel):
    drone_id: uuid.UUID
    drone_name: str
    hours: float


class HoursPerPilotOut(BaseModel):
    pilot_id: uuid.UUID
    pilot_name: str
    hours: float


class FlightStatsOut(BaseModel):
    total_flights: int
    total_flight_hours: float
    monthly_flight_hours: list[MonthlyHoursOut]
    average_duration_minutes: float
    hours_per_drone: list[HoursPerDroneOut]
    hours_per_pilot: list[HoursPerPilotOut]
