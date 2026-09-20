package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DroneComponentDto(
    val id: String,
    val droneId: String,
    val componentType: String,
    val name: String,
    val serialNumber: String? = null,
    val cycleCount: Int = 0,
    val installedDate: String? = null,
    val status: String = "active",
)

@Serializable
data class DroneDto(
    val id: String,
    val droneName: String,
    val model: String,
    val serialNumber: String,
    val uin: String? = null,
    val purchaseDate: String? = null,
    val deliveryDate: String? = null,
    val invoiceNumber: String? = null,
    val invoiceDate: String? = null,
    val warrantyStartDate: String? = null,
    val warrantyEndDate: String? = null,
    val totalFlightHours: Double = 0.0,
    val totalFlights: Int = 0,
    val lastFlightAt: String? = null,
    val lastServiceAt: String? = null,
    val nextMaintenanceDueHours: Double? = null,
    val nextMaintenanceDueDate: String? = null,
    val firmwareVersion: String? = null,
    val status: String,
    val components: List<DroneComponentDto> = emptyList(),
)
