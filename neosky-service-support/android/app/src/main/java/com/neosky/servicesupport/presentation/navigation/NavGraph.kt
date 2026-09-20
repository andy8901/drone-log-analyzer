package com.neosky.servicesupport.presentation.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.compose.runtime.getValue
import com.neosky.servicesupport.presentation.auth.ForgotPasswordScreen
import com.neosky.servicesupport.presentation.auth.LoginScreen
import com.neosky.servicesupport.presentation.auth.RegisterScreen
import com.neosky.servicesupport.presentation.common.OfflineBanner
import com.neosky.servicesupport.presentation.dashboard.DashboardScreen
import com.neosky.servicesupport.presentation.documents.DocumentsScreen
import com.neosky.servicesupport.presentation.drones.DroneDetailScreen
import com.neosky.servicesupport.presentation.drones.DroneListScreen
import com.neosky.servicesupport.presentation.flights.AddFlightLogScreen
import com.neosky.servicesupport.presentation.flights.FlightLogListScreen
import com.neosky.servicesupport.presentation.invoices.InvoiceDetailScreen
import com.neosky.servicesupport.presentation.invoices.InvoiceListScreen
import com.neosky.servicesupport.presentation.maintenance.MaintenanceScreen
import com.neosky.servicesupport.presentation.notifications.NotificationsScreen
import com.neosky.servicesupport.presentation.profile.ProfileScreen
import com.neosky.servicesupport.presentation.search.SearchScreen
import com.neosky.servicesupport.presentation.tickets.CreateTicketScreen
import com.neosky.servicesupport.presentation.tickets.TicketDetailScreen
import com.neosky.servicesupport.presentation.tickets.TicketListScreen
import com.neosky.servicesupport.presentation.warranty.WarrantyScreen

private val topLevelRoutes = BottomNavDestination.entries.map { it.screen.route }.toSet()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeoSkyNavHost(
    isLoggedIn: Boolean,
    isOffline: Boolean,
    pendingDeepLinkRoute: String? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = isLoggedIn && currentRoute in topLevelRoutes
    val showOfflineBanner = isLoggedIn && isOffline

    LaunchedEffect(isLoggedIn, pendingDeepLinkRoute) {
        if (isLoggedIn && pendingDeepLinkRoute != null) {
            navController.navigate(pendingDeepLinkRoute)
            onDeepLinkConsumed()
        }
    }

    Column {
        OfflineBanner(isOffline = showOfflineBanner)
        Scaffold(
            bottomBar = {
                if (showBottomBar) {
                    NeoSkyBottomNavBar(currentRoute = currentRoute) { destination ->
                        navController.navigate(destination.screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            },
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (isLoggedIn) Screen.Dashboard.route else Screen.Login.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                // ---------------------------------------------------------------- Auth
                composable(Screen.Login.route) {
                    LoginScreen(
                        onLoginSuccess = {
                            navController.navigate(Screen.Dashboard.route) {
                                popUpTo(0)
                            }
                        },
                        onNavigateToRegister = { navController.navigate(Screen.Register.route) },
                        onNavigateToForgotPassword = { navController.navigate(Screen.ForgotPassword.route) },
                    )
                }
                composable(Screen.Register.route) {
                    RegisterScreen(
                        onRegisterSuccess = { navController.popBackStack() },
                        onNavigateToLogin = { navController.popBackStack() },
                    )
                }
                composable(Screen.ForgotPassword.route) {
                    ForgotPasswordScreen(
                        onDone = { navController.popBackStack() },
                        onNavigateToLogin = { navController.popBackStack() },
                    )
                }

                // ----------------------------------------------------------- Dashboard
                composable(Screen.Dashboard.route) {
                    DashboardScreen(
                        onNavigateToDrones = { navController.navigate(Screen.DroneList.route) },
                        onNavigateToTickets = { navController.navigate(Screen.TicketList.route) },
                        onNavigateToFlights = { navController.navigate(Screen.FlightLogList.route) },
                        onNavigateToInvoices = { navController.navigate(Screen.InvoiceList.route) },
                        onRaiseTicket = { navController.navigate(Screen.CreateTicket.createRoute()) },
                        onLogFlight = { navController.navigate(Screen.AddFlightLog.createRoute()) },
                        onNavigateToWarrantyOrMaintenance = { navController.navigate(Screen.DroneList.route) },
                        onNotifications = { navController.navigate(Screen.Notifications.route) },
                        onSearch = { navController.navigate(Screen.Search.route) },
                        onTicketClick = { ticketId -> navController.navigate(Screen.TicketDetail.createRoute(ticketId)) },
                        onInvoiceClick = { invoiceId -> navController.navigate(Screen.InvoiceDetail.createRoute(invoiceId)) },
                    )
                }

                // -------------------------------------------------------------- Drones
                composable(Screen.DroneList.route) {
                    DroneListScreen(
                        onDroneClick = { droneId -> navController.navigate(Screen.DroneDetail.createRoute(droneId)) },
                        onSearch = { navController.navigate(Screen.Search.route) },
                    )
                }
                composable(
                    route = Screen.DroneDetail.route,
                    arguments = listOf(navArgument("droneId") { type = NavType.StringType }),
                ) { backStack ->
                    val droneId = backStack.arguments?.getString("droneId").orEmpty()
                    DroneDetailScreen(
                        droneId = droneId,
                        onBack = { navController.popBackStack() },
                        onRaiseTicket = { id -> navController.navigate(Screen.CreateTicket.createRoute(id)) },
                        onLogFlight = { id -> navController.navigate(Screen.AddFlightLog.createRoute(id)) },
                    )
                }

                // ------------------------------------------------------------- Tickets
                composable(Screen.TicketList.route) {
                    TicketListScreen(
                        onTicketClick = { ticketId -> navController.navigate(Screen.TicketDetail.createRoute(ticketId)) },
                        onCreateTicket = { navController.navigate(Screen.CreateTicket.createRoute()) },
                    )
                }
                composable(
                    route = Screen.CreateTicket.route,
                    arguments = listOf(navArgument("droneId") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { backStack ->
                    CreateTicketScreen(
                        preselectedDroneId = backStack.arguments?.getString("droneId"),
                        onSubmitted = { ticketId ->
                            navController.navigate(Screen.TicketDetail.createRoute(ticketId)) {
                                popUpTo(Screen.TicketList.route)
                            }
                        },
                        onBack = { navController.popBackStack() },
                    )
                }
                composable(
                    route = Screen.TicketDetail.route,
                    arguments = listOf(navArgument("ticketId") { type = NavType.StringType }),
                ) { backStack ->
                    val ticketId = backStack.arguments?.getString("ticketId").orEmpty()
                    TicketDetailScreen(ticketId = ticketId, onBack = { navController.popBackStack() })
                }

                // ------------------------------------------------------------- Flights
                composable(Screen.FlightLogList.route) {
                    FlightLogListScreen(onAddFlight = { navController.navigate(Screen.AddFlightLog.createRoute()) })
                }
                composable(
                    route = Screen.AddFlightLog.route,
                    arguments = listOf(navArgument("droneId") { type = NavType.StringType; nullable = true; defaultValue = null }),
                ) { backStack ->
                    AddFlightLogScreen(
                        preselectedDroneId = backStack.arguments?.getString("droneId"),
                        onSaved = { navController.popBackStack() },
                        onBack = { navController.popBackStack() },
                    )
                }

                // Standalone Warranty/Maintenance routes — used by FCM deep links; the same
                // screens are also embedded as tabs inside DroneDetailScreen.
                composable(
                    route = Screen.Warranty.route,
                    arguments = listOf(navArgument("droneId") { type = NavType.StringType }),
                ) {
                    Scaffold(topBar = {
                        TopAppBar(
                            title = { Text("Warranty") },
                            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                        )
                    }) { padding -> WarrantyScreen(modifier = Modifier.padding(padding)) }
                }
                composable(
                    route = Screen.Maintenance.route,
                    arguments = listOf(navArgument("droneId") { type = NavType.StringType }),
                ) {
                    Scaffold(topBar = {
                        TopAppBar(
                            title = { Text("Maintenance") },
                            navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") } },
                        )
                    }) { padding -> MaintenanceScreen(modifier = Modifier.padding(padding)) }
                }

                // ------------------------------------------------------------ Invoices
                composable(Screen.InvoiceList.route) {
                    InvoiceListScreen(onInvoiceClick = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) })
                }
                composable(
                    route = Screen.InvoiceDetail.route,
                    arguments = listOf(navArgument("invoiceId") { type = NavType.StringType }),
                ) { backStack ->
                    val invoiceId = backStack.arguments?.getString("invoiceId").orEmpty()
                    InvoiceDetailScreen(invoiceId = invoiceId, onBack = { navController.popBackStack() })
                }

                // ----------------------------------------------------------- Documents
                composable(Screen.Documents.route) {
                    DocumentsScreen(onBack = { navController.popBackStack() })
                }

                // ------------------------------------------------------- Notifications
                composable(Screen.Notifications.route) {
                    NotificationsScreen(onBack = { navController.popBackStack() })
                }

                // ------------------------------------------------------------- Search
                composable(Screen.Search.route) {
                    SearchScreen(
                        onBack = { navController.popBackStack() },
                        onDroneClick = { id -> navController.navigate(Screen.DroneDetail.createRoute(id)) },
                        onTicketClick = { id -> navController.navigate(Screen.TicketDetail.createRoute(id)) },
                        onInvoiceClick = { id -> navController.navigate(Screen.InvoiceDetail.createRoute(id)) },
                    )
                }

                // ------------------------------------------------------------- Profile
                composable(Screen.Profile.route) {
                    ProfileScreen(
                        onLoggedOut = {
                            navController.navigate(Screen.Login.route) { popUpTo(0) }
                        },
                        onNotifications = { navController.navigate(Screen.Notifications.route) },
                        onDocuments = { navController.navigate(Screen.Documents.route) },
                    )
                }
            }
        }
    }
}
