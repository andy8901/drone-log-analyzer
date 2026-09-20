package com.neosky.servicesupport.fcm

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.neosky.servicesupport.MainActivity
import com.neosky.servicesupport.R
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.NotificationType
import com.neosky.servicesupport.domain.repository.CustomerRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject

/**
 * Handles device-token registration and incoming push notifications for ticket/warranty/
 * maintenance/invoice events (see docs/API_SPEC.md § 12 and the `notification_type` enum).
 */
@AndroidEntryPoint
class NeoSkyFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var customerRepository: CustomerRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            when (customerRepository.updateFcmToken(token)) {
                is NetworkResult.Success -> Unit
                is NetworkResult.Error -> Unit // Not logged in yet, or offline — the token is re-sent on next successful login/profile update.
                NetworkResult.Loading -> Unit
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val notificationType = NotificationType.fromWire(message.data["notification_type"])
        val title = message.notification?.title ?: message.data["title"] ?: "NeoSky Service & Support"
        val body = message.notification?.body ?: message.data["body"] ?: ""
        val relatedEntityId = message.data["related_entity_id"]

        showNotification(notificationType, title, body, relatedEntityId)
    }

    private fun showNotification(type: NotificationType, title: String, body: String, relatedEntityId: String?) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_NOTIFICATION_TYPE, type.wireValue)
            putExtra(EXTRA_RELATED_ENTITY_ID, relatedEntityId)
        }
        val pendingIntentFlags = PendingIntent.FLAG_UPDATE_CURRENT or
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        val pendingIntent = PendingIntent.getActivity(this, notificationIdCounter.incrementAndGet(), intent, pendingIntentFlags)

        val notification = NotificationCompat.Builder(this, getString(R.string.default_notification_channel_id))
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with a branded icon once design assets are supplied.
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(this).apply {
            // POST_NOTIFICATIONS is requested at runtime by the Compose UI on first launch (API 33+);
            // if it hasn't been granted yet this call is a no-op rather than a crash.
            runCatching { notify(notificationIdCounter.get(), notification) }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    companion object {
        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_RELATED_ENTITY_ID = "related_entity_id"
        private val notificationIdCounter = AtomicInteger(1000)
    }
}
