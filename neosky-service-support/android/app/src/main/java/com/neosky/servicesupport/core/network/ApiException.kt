package com.neosky.servicesupport.core.network

/**
 * Normalized representation of the backend's single error shape:
 * `{ "error": { "code": "VALIDATION_ERROR", "message": "...", "fields": { ... } } }`
 * (see docs/API_SPEC.md § Conventions).
 */
data class ApiException(
    val httpCode: Int,
    val code: String,
    override val message: String,
    val fieldErrors: Map<String, String> = emptyMap(),
    override val cause: Throwable? = null,
) : Exception(message, cause) {

    val isUnauthorized: Boolean get() = httpCode == 401
    val isNotFound: Boolean get() = httpCode == 404
    val isNetworkError: Boolean get() = code == CODE_NETWORK_ERROR
    val isTimeout: Boolean get() = code == CODE_TIMEOUT

    companion object {
        const val CODE_NETWORK_ERROR = "NETWORK_ERROR"
        const val CODE_TIMEOUT = "TIMEOUT"
        const val CODE_UNKNOWN = "UNKNOWN_ERROR"
        const val CODE_SERIALIZATION = "SERIALIZATION_ERROR"

        fun networkError(cause: Throwable? = null) = ApiException(
            httpCode = -1,
            code = CODE_NETWORK_ERROR,
            message = "No internet connection. Please check your network and try again.",
            cause = cause,
        )

        fun timeout(cause: Throwable? = null) = ApiException(
            httpCode = -1,
            code = CODE_TIMEOUT,
            message = "The request timed out. Please try again.",
            cause = cause,
        )

        fun unknown(cause: Throwable? = null) = ApiException(
            httpCode = -1,
            code = CODE_UNKNOWN,
            message = cause?.message ?: "Something went wrong. Please try again.",
            cause = cause,
        )
    }
}
