package com.neosky.servicesupport.data.repository

import com.neosky.servicesupport.core.network.NetworkResult
import com.neosky.servicesupport.core.network.safeApiCall
import com.neosky.servicesupport.data.remote.ApiService
import com.neosky.servicesupport.domain.model.SearchResults
import com.neosky.servicesupport.domain.repository.SearchRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
) : SearchRepository {
    override suspend fun search(query: String): NetworkResult<SearchResults> {
        val result = safeApiCall { apiService.search(query) }
        return when (result) {
            is NetworkResult.Success -> NetworkResult.Success(result.data.toDomain())
            is NetworkResult.Error -> result
            NetworkResult.Loading -> result
        }
    }
}
