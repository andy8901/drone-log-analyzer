package com.neosky.servicesupport.core.network

/**
 * A uniform wrapper around every network-driven operation's outcome, so ViewModels never
 * touch Retrofit/OkHttp exceptions directly.
 */
sealed class NetworkResult<out T> {
    data class Success<T>(val data: T) : NetworkResult<T>()
    data class Error(val apiException: ApiException) : NetworkResult<Nothing>()
    data object Loading : NetworkResult<Nothing>()
}

inline fun <T> NetworkResult<T>.onSuccess(action: (T) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}

inline fun <T> NetworkResult<T>.onError(action: (ApiException) -> Unit): NetworkResult<T> {
    if (this is NetworkResult.Error) action(apiException)
    return this
}

fun <T> NetworkResult<T>.getOrNull(): T? = (this as? NetworkResult.Success)?.data
