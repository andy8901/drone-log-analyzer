package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.Document

interface DocumentRepository {
    suspend fun getDocuments(droneId: String? = null, ticketId: String? = null, type: String? = null): NetworkResult<List<Document>>

    suspend fun downloadDocument(documentId: String): NetworkResult<ByteArray>
}
