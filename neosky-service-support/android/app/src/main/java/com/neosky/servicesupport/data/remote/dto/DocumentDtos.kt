package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DocumentDto(
    val id: String,
    val documentType: String,
    val customerId: String? = null,
    val droneId: String? = null,
    val droneName: String? = null,
    val ticketId: String? = null,
    val ticketNumber: String? = null,
    val serviceRecordId: String? = null,
    val fileName: String,
    val fileUrl: String,
    val createdAt: String,
)
