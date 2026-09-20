package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.core.network.safeApiCallUnit
import com.neosky.servicesupport.data.local.dao.NotificationDao
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.Notification
import com.neosky.servicesupport.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val notificationDao: NotificationDao,
) : NotificationRepository {

    override fun observeNotifications(): Flow<List<Notification>> =
        notificationDao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun refreshNotifications(unreadOnly: Boolean): NetworkResult<List<Notification>> {
        val result = safeApiCall { apiService.getNotifications(unreadOnly = unreadOnly) }
        return when (result) {
            is NetworkResult.Success -> {
                val dtos = result.data.items
                notificationDao.upsertAll(dtos.map { it.toEntity() })
                NetworkResult.Success(dtos.map { it.toDomain() })
            }
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun markAsRead(notificationId: String): NetworkResult<Unit> {
        val result = safeApiCallUnit { apiService.markNotificationRead(notificationId) }
        if (result is NetworkResult.Success) notificationDao.markRead(notificationId)
        return result
    }

    override suspend fun markAllAsRead(): NetworkResult<Unit> {
        val result = safeApiCallUnit { apiService.markAllNotificationsRead() }
        if (result is NetworkResult.Success) notificationDao.markAllRead()
        return result
    }

    override fun observeUnreadCount(): Flow<Int> = notificationDao.observeUnreadCount()
}
