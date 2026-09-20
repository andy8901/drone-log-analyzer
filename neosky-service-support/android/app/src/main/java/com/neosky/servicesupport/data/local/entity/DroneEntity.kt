package com.neosky.servicesupport.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Read-through cache of a customer's own drones, for offline viewing of Drones/Dashboard screens. */
@Entity(tableName = "drones")
data class DroneEntity(
    @PrimaryKey val id: String,
    val droneName: String,
    val model: String,
    val serialNumber: String,
    val uin: String?,
    val purchaseDate: String?,
    val deliveryDate: String?,
    val invoiceNumber: String?,
    val invoiceDate: String?,
    val warrantyStartDate: String?,
    val warrantyEndDate: String?,
    val totalFlightHours: Double,
    val totalFlights: Int,
    val lastFlightAt: String?,
    val lastServiceAt: String?,
    val nextMaintenanceDueHours: Double?,
    val nextMaintenanceDueDate: String?,
    val firmwareVersion: String?,
    val status: String,
    /** JSON-encoded List<DroneComponent> — see [com.neosky.servicesupport.data.local.Converters]. */
    val componentsJson: String = "[]",
    val cachedAt: Long = System.currentTimeMillis(),
)
