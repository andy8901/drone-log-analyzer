package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class SearchResultsDto(
    val drones: List<DroneDto> = emptyList(),
    val tickets: List<TicketDto> = emptyList(),
    val invoices: List<InvoiceDto> = emptyList(),
    val serviceRecords: List<ServiceRecordDto> = emptyList(),
)
