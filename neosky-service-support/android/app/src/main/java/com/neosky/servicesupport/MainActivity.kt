package com.neosky.servicesupport

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.MutableState
import com.neosky.servicesupport.fcm.NeoSkyFirebaseMessagingService
import com.neosky.servicesupport.fcm.NotificationDeepLink
import com.neosky.servicesupport.presentation.navigation.NeoSkyNavHost
import com.neosky.servicesupport.presentation.theme.NeoSkyServiceSupportTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private lateinit var latestIntentState: MutableState<Intent?>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NeoSkyServiceSupportTheme {
                val currentUser by mainViewModel.currentUser.collectAsState()
                val isOffline by mainViewModel.isOffline.collectAsState()
                val latestIntent = remember { mutableStateOf(intent) }.also { latestIntentState = it }
                var pendingDeepLinkRoute by remember(latestIntent.value) {
                    mutableStateOf(deepLinkRouteFromIntent(latestIntent.value))
                }

                NeoSkyNavHost(
                    isLoggedIn = currentUser != null,
                    isOffline = isOffline,
                    pendingDeepLinkRoute = pendingDeepLinkRoute,
                    onDeepLinkConsumed = { pendingDeepLinkRoute = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (::latestIntentState.isInitialized) {
            latestIntentState.value = intent
        }
    }

    private fun deepLinkRouteFromIntent(intent: Intent?): String? {
        val type = intent?.getStringExtra(NeoSkyFirebaseMessagingService.EXTRA_NOTIFICATION_TYPE) ?: return null
        val relatedEntityId = intent.getStringExtra(NeoSkyFirebaseMessagingService.EXTRA_RELATED_ENTITY_ID)
        return NotificationDeepLink.routeFor(type, relatedEntityId)
    }
}
