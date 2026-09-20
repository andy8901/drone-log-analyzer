package com.neosky.servicesupport.presentation.navigation

/** Every navigable destination's route, as a sealed hierarchy so routes are never hand-typed. */
sealed class Screen(val route: String) {
    data object Login : Screen("login")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")

    data object Dashboard : Screen("dashboard")

    data object DroneList : Screen("drones")
    data object DroneDetail : Screen("drones/{droneId}") {
        fun createRoute(droneId: String) = "drones/$droneId"
    }

    data object TicketList : Screen("tickets")
    data object CreateTicket : Screen("tickets/new?droneId={droneId}") {
        fun createRoute(droneId: String? = null) = if (droneId != null) "tickets/new?droneId=$droneId" else "tickets/new"
    }
    data object TicketDetail : Screen("tickets/{ticketId}") {
        fun createRoute(ticketId: String) = "tickets/$ticketId"
    }

    data object FlightLogList : Screen("flights")
    data object AddFlightLog : Screen("flights/new?droneId={droneId}") {
        fun createRoute(droneId: String? = null) = if (droneId != null) "flights/new?droneId=$droneId" else "flights/new"
    }

    data object Warranty : Screen("drones/{droneId}/warranty") {
        fun createRoute(droneId: String) = "drones/$droneId/warranty"
    }
    data object Maintenance : Screen("drones/{droneId}/maintenance") {
        fun createRoute(droneId: String) = "drones/$droneId/maintenance"
    }

    data object InvoiceList : Screen("invoices")
    data object InvoiceDetail : Screen("invoices/{invoiceId}") {
        fun createRoute(invoiceId: String) = "invoices/$invoiceId"
    }

    data object Documents : Screen("documents")
    data object Notifications : Screen("notifications")
    data object Search : Screen("search")
    data object Profile : Screen("profile")
}

/** The five bottom-navigation destinations, in the exact order the design calls for. */
enum class BottomNavDestination(val screen: Screen, val label: String) {
    HOME(Screen.Dashboard, "Home"),
    MY_DRONES(Screen.DroneList, "My Drones"),
    TICKETS(Screen.TicketList, "Tickets"),
    FLIGHT_LOG(Screen.FlightLogList, "Flight Log"),
    PROFILE(Screen.Profile, "Profile"),
}
