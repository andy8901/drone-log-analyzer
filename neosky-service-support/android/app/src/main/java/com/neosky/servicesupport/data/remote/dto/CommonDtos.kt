package com.neosky.servicesupport.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Generic pagination envelope: `{ "items": [...], "page": 1, "page_size": 20, "total": 42, "total_pages": 3 }`
 * (docs/API_SPEC.md § Conventions). The Retrofit kotlinx-serialization converter resolves the
 * concrete `T` serializer from the call site's declared generic return type.
 */
@Serializable
data class PageDto<T>(
    val items: List<T>,
    val page: Int,
    val pageSize: Int,
    val total: Int,
    val totalPages: Int,
)

@Serializable
data class MessageResponseDto(val message: String)
