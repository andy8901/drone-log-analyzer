package com.neosky.servicesupport.fcm

import com.neosky.servicesupport.domain.model.NotificationType
import com.neosky.servicesupport.presentation.navigation.Screen

/**
 * Maps a push notification's `notification_type` + `related_entity_id` (see
 * docs/API_SPEC.md § 12's `notification_type` enum) to the in-app route it should open,
 * used by [NeoSkyFirebaseMessagingService] (via MainActivity's intent extras) to deep-link a
 * tapped notification into the right screen.
 */
object NotificationDeepLink {

    fun routeFor(typeWireValue: String?, relatedEntityId: String?): String {
        val type = NotificationType.fromWire(typeWireValue)
        return when (type) {
            NotificationType.TICKET_CREATED,
            NotificationType.TICKET_ASSIGNED,
            NotificationType.TICKET_STATUS_CHANGED,
            NotificationType.ENGINEER_COMMENT,
            NotificationType.CUSTOMER_RESPONSE_REQUIRED,
            NotificationType.TICKET_RESOLVED,
            NotificationType.TICKET_CLOSED,
            -> relatedEntityId?.let { Screen.TicketDetail.createRoute(it) } ?: Screen.TicketList.route

            NotificationType.WARRANTY_EXPIRING ->
                relatedEntityId?.let { Screen.Warranty.createRoute(it) } ?: Screen.DroneList.route

            NotificationType.MAINTENANCE_DUE,
            NotificationType.MAINTENANCE_OVERDUE,
            -> relatedEntityId?.let { Screen.Maintenance.createRoute(it) } ?: Screen.DroneList.route

            NotificationType.INVOICE_GENERATED,
            NotificationType.PAYMENT_PENDING,
            -> relatedEntityId?.let { Screen.InvoiceDetail.createRoute(it) } ?: Screen.InvoiceList.route

            NotificationType.SERVICE_COMPLETED ->
                relatedEntityId?.let { Screen.DroneDetail.createRoute(it) } ?: Screen.DroneList.route

            NotificationType.UNKNOWN -> Screen.Notifications.route
        }
    }
}
