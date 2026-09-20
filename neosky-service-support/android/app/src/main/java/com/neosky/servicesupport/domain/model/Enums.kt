package com.neosky.servicesupport.domain.model

/**
 * Status/category enumerations mirroring the Postgres enums in `database/schema.sql`
 * and the "Status Enumerations" section of `docs/API_SPEC.md`.
 *
 * Every enum carries a [wireValue] equal to the exact string the backend sends/expects,
 * and a [fromWire] companion parser that falls back to a sentinel `UNKNOWN` entry instead
 * of throwing, so an unrecognized value from a newer backend never crashes the client.
 */

enum class UserRole(val wireValue: String) {
    CUSTOMER("customer"),
    SERVICE_ENGINEER("service_engineer"),
    ADMIN("admin"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): UserRole = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class DroneStatus(val wireValue: String, val displayName: String) {
    ACTIVE("active", "Active"),
    UNDER_SERVICE("under_service", "Under Service"),
    GROUNDED("grounded", "Grounded"),
    RETIRED("retired", "Retired"),
    LOST_STOLEN("lost_stolen", "Lost / Stolen"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): DroneStatus = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class ComponentType(val wireValue: String) {
    BATTERY("battery"),
    CONTROLLER_GCS("controller_gcs"),
    PAYLOAD("payload"),
    PROPELLER("propeller"),
    MOTOR("motor"),
    ESC("esc"),
    FLIGHT_CONTROLLER("flight_controller"),
    OTHER("other"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): ComponentType = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class TicketCategory(val wireValue: String, val displayName: String) {
    FLIGHT_ISSUE("flight_issue", "Flight Issue"),
    HARDWARE_ISSUE("hardware_issue", "Hardware Issue"),
    SOFTWARE_ISSUE("software_issue", "Software Issue"),
    BATTERY("battery", "Battery"),
    CAMERA_PAYLOAD("camera_payload", "Camera / Payload"),
    CONTROLLER_GCS("controller_gcs", "Controller / GCS"),
    CONNECTIVITY("connectivity", "Connectivity"),
    FIRMWARE("firmware", "Firmware"),
    CRASH_DAMAGE("crash_damage", "Crash / Damage"),
    WARRANTY("warranty", "Warranty"),
    AMC_SERVICE("amc_service", "AMC Service"),
    TRAINING("training", "Training"),
    OTHER("other", "Other"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): TicketCategory = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class TicketPriority(val wireValue: String, val displayName: String) {
    LOW("low", "Low"),
    MEDIUM("medium", "Medium"),
    HIGH("high", "High"),
    CRITICAL("critical", "Critical"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): TicketPriority = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class TicketStatus(val wireValue: String, val displayName: String) {
    NEW("new", "New"),
    ASSIGNED("assigned", "Assigned"),
    IN_PROGRESS("in_progress", "In Progress"),
    WAITING_CUSTOMER("waiting_customer", "Waiting on You"),
    WAITING_PARTS("waiting_parts", "Waiting on Parts"),
    SERVICE("service", "In Service"),
    QC("qc", "Quality Check"),
    RESOLVED("resolved", "Resolved"),
    CLOSED("closed", "Closed"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): TicketStatus = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class AttachmentType(val wireValue: String) {
    PHOTO("photo"),
    VIDEO("video"),
    DOCUMENT("document"),
    LOG("log"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): AttachmentType = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class FlightResult(val wireValue: String, val displayName: String) {
    SUCCESSFUL("successful", "Successful"),
    ABORTED("aborted", "Aborted"),
    CRASHED("crashed", "Crashed"),
    PARTIAL("partial", "Partial"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): FlightResult = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class SyncStatus(val wireValue: String) {
    SYNCED("synced"),
    PENDING_SYNC("pending_sync");

    companion object {
        fun fromWire(value: String?): SyncStatus = entries.firstOrNull { it.wireValue == value } ?: PENDING_SYNC
    }
}

enum class MaintenanceStatus(val wireValue: String, val displayName: String) {
    UPCOMING("upcoming", "Upcoming"),
    DUE_SOON("due_soon", "Due Soon"),
    DUE("due", "Due"),
    OVERDUE("overdue", "Overdue"),
    COMPLETED("completed", "Completed"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): MaintenanceStatus = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class WarrantyStatus(val wireValue: String, val displayName: String) {
    ACTIVE("active", "Active"),
    EXPIRING_SOON("expiring_soon", "Expiring Soon"),
    EXPIRED("expired", "Expired"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): WarrantyStatus = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class WarrantyClaimStatus(val wireValue: String, val displayName: String) {
    SUBMITTED("submitted", "Submitted"),
    APPROVED("approved", "Approved"),
    REJECTED("rejected", "Rejected"),
    COMPLETED("completed", "Completed"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): WarrantyClaimStatus = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class PaymentStatus(val wireValue: String, val displayName: String) {
    PAID("paid", "Paid"),
    PENDING("pending", "Pending"),
    PARTIALLY_PAID("partially_paid", "Partially Paid"),
    CANCELLED("cancelled", "Cancelled"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): PaymentStatus = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class WarrantyType(val wireValue: String, val displayName: String) {
    WARRANTY("warranty", "Warranty"),
    NON_WARRANTY("non_warranty", "Non-Warranty"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): WarrantyType = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class DocumentType(val wireValue: String, val displayName: String) {
    INVOICE("invoice", "Invoice"),
    WARRANTY_CERTIFICATE("warranty_certificate", "Warranty Certificate"),
    SERVICE_REPORT("service_report", "Service Report"),
    QC_REPORT("qc_report", "QC Report"),
    RCA_REPORT("rca_report", "RCA Report"),
    MAINTENANCE_REPORT("maintenance_report", "Maintenance Report"),
    DRONE_MANUAL("drone_manual", "Drone Manual"),
    TRAINING_CERTIFICATE("training_certificate", "Training Certificate"),
    OTHER("other", "Other"),
    UNKNOWN("unknown", "Unknown");

    companion object {
        fun fromWire(value: String?): DocumentType = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}

enum class NotificationType(val wireValue: String) {
    TICKET_CREATED("ticket_created"),
    TICKET_ASSIGNED("ticket_assigned"),
    TICKET_STATUS_CHANGED("ticket_status_changed"),
    ENGINEER_COMMENT("engineer_comment"),
    CUSTOMER_RESPONSE_REQUIRED("customer_response_required"),
    TICKET_RESOLVED("ticket_resolved"),
    TICKET_CLOSED("ticket_closed"),
    WARRANTY_EXPIRING("warranty_expiring"),
    MAINTENANCE_DUE("maintenance_due"),
    MAINTENANCE_OVERDUE("maintenance_overdue"),
    INVOICE_GENERATED("invoice_generated"),
    PAYMENT_PENDING("payment_pending"),
    SERVICE_COMPLETED("service_completed"),
    UNKNOWN("unknown");

    companion object {
        fun fromWire(value: String?): NotificationType = entries.firstOrNull { it.wireValue == value } ?: UNKNOWN
    }
}
