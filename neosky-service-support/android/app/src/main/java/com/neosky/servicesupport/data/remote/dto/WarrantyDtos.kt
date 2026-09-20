package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class WarrantyClaimDto(
    val id: String,
    val claimNumber: String,
    val ticketId: String,
    val description: String? = null,
    val status: String,
    val claimedAt: String,
    val resolvedAt: String? = null,
)

@Serializable
data class WarrantyDto(
    val droneId: String,
    val startDate: String,
    val endDate: String,
    val daysRemaining: Int,
    val status: String,
    val coveredItems: List<String> = emptyList(),
    val excludedItems: List<String> = emptyList(),
    val claims: List<WarrantyClaimDto> = emptyList(),
)
