package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Invoice

interface InvoiceRepository {
    suspend fun getInvoices(status: String? = null, page: Int = 1): NetworkResult<List<Invoice>>

    suspend fun getInvoice(invoiceId: String): NetworkResult<Invoice>

    /** Downloads the invoice PDF bytes; the caller (UseCase/ViewModel) persists them via MediaStore. */
    suspend fun downloadInvoicePdf(invoiceId: String): NetworkResult<ByteArray>
}
