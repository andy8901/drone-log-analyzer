package com.neosky.servicesupport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val type: String,
    val title: String,
    val body: String,
    val relatedEntityType: String?,
    val relatedEntityId: String?,
    val isRead: Boolean,
    val createdAt: String,
)
