import uuid
from datetime import datetime

from pydantic import BaseModel


class TicketCreate(BaseModel):
    drone_id: uuid.UUID
    category: str
    sub_category: str | None = None
    priority: str = "medium"
    subject: str
    description: str
    issue_datetime: datetime | None = None
    location: str | None = None
    flight_hours_at_issue: float | None = None


class TicketCommentCreate(BaseModel):
    comment: str
    is_internal: bool = False


class TicketCommentOut(BaseModel):
    id: uuid.UUID
    author_id: uuid.UUID
    comment: str
    status_from: str | None = None
    status_to: str | None = None
    is_internal: bool
    created_at: datetime

    model_config = {"from_attributes": True}


class TicketAttachmentOut(BaseModel):
    id: uuid.UUID
    file_url: str
    file_type: str
    file_name: str
    file_size_bytes: int | None = None
    created_at: datetime

    model_config = {"from_attributes": True}


class TicketOut(BaseModel):
    id: uuid.UUID
    ticket_number: str
    drone_id: uuid.UUID
    category: str
    sub_category: str | None = None
    priority: str
    subject: str
    description: str
    status: str
    issue_datetime: datetime | None = None
    location: str | None = None
    flight_hours_at_issue: float | None = None
    assigned_engineer_id: uuid.UUID | None = None
    resolved_at: datetime | None = None
    closed_at: datetime | None = None
    customer_confirmed_resolution: bool
    created_at: datetime
    updated_at: datetime

    model_config = {"from_attributes": True}


class TimelineEntryOut(BaseModel):
    status_from: str | None = None
    status_to: str | None = None
    comment: str
    author_id: uuid.UUID
    created_at: datetime


class TicketDetailOut(TicketOut):
    timeline: list[TimelineEntryOut] = []
    attachments: list[TicketAttachmentOut] = []
    comments: list[TicketCommentOut] = []


class TicketCloseRequest(BaseModel):
    confirmed: bool
    feedback: str | None = None


class TicketAssignRequest(BaseModel):
    engineer_id: uuid.UUID


class TicketStatusUpdateRequest(BaseModel):
    status: str
    comment: str | None = None
