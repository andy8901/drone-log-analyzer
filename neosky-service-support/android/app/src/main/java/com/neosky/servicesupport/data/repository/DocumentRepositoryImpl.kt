package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.core.network.safeApiCallForResponse
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.Document
import com.neosky.servicesupport.domain.repository.DocumentRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : DocumentRepository {

    override suspend fun getDocuments(droneId: String?, ticketId: String?, type: String?): NetworkResult<List<Document>> {
        val result = safeApiCall { apiService.getDocuments(droneId, ticketId, type) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.map { it.toDomain() })
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }

    override suspend fun downloadDocument(documentId: String): NetworkResult<ByteArray> =
        toByteArrayResult(safeApiCallForResponse { apiService.downloadDocument(documentId) })
}
