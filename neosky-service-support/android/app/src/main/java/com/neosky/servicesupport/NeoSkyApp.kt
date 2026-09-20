package com.neosky.servicesupport

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.neosky.servicesupport.core.datastore.TokenManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class NeoSkyApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var tokenManager: TokenManager

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        clearSessionIfNotRemembered()
    }

    /**
     * "Remember me" unchecked means the session should not survive the app being fully killed
     * and relaunched (a normal in-memory-process navigation away and back is unaffected, since
     * this only runs once per process cold start).
     */
    private fun clearSessionIfNotRemembered() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            if (tokenManager.isLoggedIn() && !tokenManager.rememberMe.value) {
                tokenManager.clearSession()
            }
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java)
        val channel = NotificationChannel(
            getString(R.string.default_notification_channel_id),
            "NeoSky Notifications",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Ticket updates, warranty and maintenance reminders, invoices."
        }
        manager.createNotificationChannel(channel)
    }
}
