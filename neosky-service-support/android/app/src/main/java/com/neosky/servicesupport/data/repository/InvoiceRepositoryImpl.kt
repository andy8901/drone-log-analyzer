package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.ApiException
import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.core.network.safeApiCallForResponse
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.Invoice
import com.neosky.servicesupport.domain.repository.InvoiceRepository
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InvoiceRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : InvoiceRepository {

    override suspend fun getInvoices(status: String?, page: Int): NetworkResult<List<Invoice>> {
        val result = safeApiCall { apiService.getInvoices(status, page) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.items.map { it.toDomain() })
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun getInvoice(invoiceId: String): NetworkResult<Invoice> {
        val result = safeApiCall { apiService.getInvoice(invoiceId) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun downloadInvoicePdf(invoiceId: String): NetworkResult<ByteArray> =
        toByteArrayResult(safeApiCallForResponse { apiService.getInvoicePdf(invoiceId) })
}

/** Shared by Invoice/Document repositories: reads a [ResponseBody] into memory once, safely. */
internal fun toByteArrayResult(result: NetworkResult<ResponseBody>): NetworkResult<ByteArray> = when (result) {
    is NetworkResult.Success -> try {
        NetworkResult.Success(result.data.bytes())
    } catch (t: Throwable) {
        NetworkResult.Error(ApiException.unknown(t))
    }
    is NetworkResult.Error -> result
    NetworkResult.Loading -> result
}
