import uuid
from datetime import date, datetime

from pydantic import BaseModel


class ServiceRecordOut(BaseModel):
    id: uuid.UUID
    ticket_id: uuid.UUID | None = None
    maintenance_record_id: uuid.UUID | None = None
    service_date: date
    issue_summary: str | None = None
    action_taken: str | None = None
    performed_by: uuid.UUID | None = None
    status: str
    created_at: datetime

    model_config = {"from_attributes": True}


class ServiceRecordCreate(BaseModel):
    drone_id: uuid.UUID
    ticket_id: uuid.UUID | None = None
    maintenance_record_id: uuid.UUID | None = None
    service_date: date
    issue_summary: str | None = None
    action_taken: str | None = None
    status: str = "completed"
    document_file_name: str | None = None
    document_file_url: str | None = None
    document_type: str | None = None
