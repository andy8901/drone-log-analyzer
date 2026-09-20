package com.neosky.servicesupport.domain.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.domain.model.SearchResults

interface SearchRepository {
    suspend fun search(query: String): NetworkResult<SearchResults>
}
