package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Notification
import kotlinx.coroutines.flow.Flow

interface NotificationRepository {
    fun observeNotifications(): Flow<List<Notification>>

    suspend fun refreshNotifications(unreadOnly: Boolean = false): NetworkResult<List<Notification>>

    suspend fun markAsRead(notificationId: String): NetworkResult<Unit>

    suspend fun markAllAsRead(): NetworkResult<Unit>

    /** Locally-known unread count, for a badge on the bottom nav / notification bell. */
    fun observeUnreadCount(): Flow<Int>
}
