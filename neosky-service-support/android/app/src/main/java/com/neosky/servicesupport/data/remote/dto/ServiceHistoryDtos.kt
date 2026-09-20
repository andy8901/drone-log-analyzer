package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ServiceRecordDto(
    val id: String,
    val droneId: String,
    val ticketId: String? = null,
    val serviceDate: String,
    val issueSummary: String? = null,
    val actionTaken: String? = null,
    val performedByName: String? = null,
    val status: String = "completed",
)
