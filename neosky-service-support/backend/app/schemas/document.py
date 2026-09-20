import uuid
from datetime import datetime

from pydantic import BaseModel


class DocumentOut(BaseModel):
    id: uuid.UUID
    document_type: str
    drone_id: uuid.UUID | None = None
    ticket_id: uuid.UUID | None = None
    service_record_id: uuid.UUID | None = None
    file_name: str
    file_url: str
    created_at: datetime

    model_config = {"from_attributes": True}
