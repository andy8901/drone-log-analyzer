import uuid
from datetime import datetime

from pydantic import BaseModel


class NotificationOut(BaseModel):
    id: uuid.UUID
    type: str
    title: str
    body: str
    related_entity_type: str | None = None
    related_entity_id: uuid.UUID | None = None
    is_read: bool
    created_at: datetime

    model_config = {"from_attributes": True}
