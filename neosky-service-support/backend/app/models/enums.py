"""Postgres ENUM types mirrored from database/schema.sql.

`create_type=False` on every one of these: the enum types are created by
schema.sql, not by SQLAlchemy. These objects only describe the existing
Postgres types to the ORM.
"""

from sqlalchemy.dialects.postgresql import ENUM as PGEnum

user_role_enum = PGEnum(
    "customer",
    "service_engineer",
    "admin",
    name="user_role",
    create_type=False,
)

drone_status_enum = PGEnum(
    "active",
    "under_service",
    "grounded",
    "retired",
    "lost_stolen",
    name="drone_status",
    create_type=False,
)

component_type_enum = PGEnum(
    "battery",
    "controller_gcs",
    "payload",
    "propeller",
    "motor",
    "esc",
    "flight_controller",
    "other",
    name="component_type",
    create_type=False,
)

ticket_category_enum = PGEnum(
    "flight_issue",
    "hardware_issue",
    "software_issue",
    "battery",
    "camera_payload",
    "controller_gcs",
    "connectivity",
    "firmware",
    "crash_damage",
    "warranty",
    "amc_service",
    "training",
    "other",
    name="ticket_category",
    create_type=False,
)

ticket_priority_enum = PGEnum(
    "low",
    "medium",
    "high",
    "critical",
    name="ticket_priority",
    create_type=False,
)

ticket_status_enum = PGEnum(
    "new",
    "assigned",
    "in_progress",
    "waiting_customer",
    "waiting_parts",
    "service",
    "qc",
    "resolved",
    "closed",
    name="ticket_status",
    create_type=False,
)

attachment_type_enum = PGEnum(
    "photo",
    "video",
    "document",
    "log",
    name="attachment_type",
    create_type=False,
)

flight_result_enum = PGEnum(
    "successful",
    "aborted",
    "crashed",
    "partial",
    name="flight_result",
    create_type=False,
)

sync_status_enum = PGEnum(
    "synced",
    "pending_sync",
    name="sync_status",
    create_type=False,
)

maintenance_status_enum = PGEnum(
    "upcoming",
    "due_soon",
    "due",
    "overdue",
    "completed",
    name="maintenance_status",
    create_type=False,
)

warranty_status_enum = PGEnum(
    "active",
    "expiring_soon",
    "expired",
    name="warranty_status",
    create_type=False,
)

warranty_claim_status_enum = PGEnum(
    "submitted",
    "approved",
    "rejected",
    "completed",
    name="warranty_claim_status",
    create_type=False,
)

payment_status_enum = PGEnum(
    "paid",
    "pending",
    "partially_paid",
    "cancelled",
    name="payment_status",
    create_type=False,
)

warranty_type_enum = PGEnum(
    "warranty",
    "non_warranty",
    name="warranty_type",
    create_type=False,
)

document_type_enum = PGEnum(
    "invoice",
    "warranty_certificate",
    "service_report",
    "qc_report",
    "rca_report",
    "maintenance_report",
    "drone_manual",
    "training_certificate",
    "other",
    name="document_type",
    create_type=False,
)

notification_type_enum = PGEnum(
    "ticket_created",
    "ticket_assigned",
    "ticket_status_changed",
    "engineer_comment",
    "customer_response_required",
    "ticket_resolved",
    "ticket_closed",
    "warranty_expiring",
    "maintenance_due",
    "maintenance_overdue",
    "invoice_generated",
    "payment_pending",
    "service_completed",
    name="notification_type",
    create_type=False,
)
