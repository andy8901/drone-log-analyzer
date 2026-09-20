package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InvoiceItemDto(
    val id: String,
    val description: String,
    val quantity: Double,
    val unitPrice: Double,
    val amount: Double,
)

@Serializable
data class PaymentDto(
    val id: String,
    val amount: Double,
    val paymentDate: String,
    val paymentMethod: String? = null,
    val referenceNumber: String? = null,
)

@Serializable
data class InvoiceDto(
    val id: String,
    val invoiceNumber: String,
    val droneId: String? = null,
    val droneName: String? = null,
    val invoiceDate: String,
    val productService: String,
    val subtotalAmount: Double = 0.0,
    val gstAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentStatus: String,
    val warrantyType: String = "non_warranty",
    val pdfUrl: String? = null,
    val invoiceItems: List<InvoiceItemDto> = emptyList(),
    val payments: List<PaymentDto> = emptyList(),
)
