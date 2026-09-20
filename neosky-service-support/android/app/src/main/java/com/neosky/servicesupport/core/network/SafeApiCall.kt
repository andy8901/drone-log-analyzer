package com.neosky.servicesupport.core.network

import kotlinx.serialization.json.Json
import kotlinx.serialization.Serializable
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.net.SocketTimeoutException

@Serializable
private data class ErrorBody(val error: ErrorDetail? = null)

@Serializable
private data class ErrorDetail(
    val code: String = ApiException.CODE_UNKNOWN,
    val message: String = "Something went wrong",
    val fields: Map<String, String> = emptyMap(),
)

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Wraps a Retrofit suspend call, mapping every failure mode into the app's single
 * [ApiException] shape (parsed from the API's `{error:{code,message,fields}}` envelope where
 * available) and every success into [NetworkResult.Success].
 */
suspend fun <T> safeApiCall(apiCall: suspend () -> T): NetworkResult<T> {
    return try {
        NetworkResult.Success(apiCall())
    } catch (e: HttpException) {
        NetworkResult.Error(e.toApiException())
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error(ApiException.timeout(e))
    } catch (e: IOException) {
        NetworkResult.Error(ApiException.networkError(e))
    } catch (e: Throwable) {
        NetworkResult.Error(ApiException.unknown(e))
    }
}

/** For endpoints that reply with an empty body on success (204, or 200 with no payload). */
suspend fun safeApiCallUnit(apiCall: suspend () -> Response<Unit>): NetworkResult<Unit> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            NetworkResult.Success(Unit)
        } else {
            NetworkResult.Error(response.toApiException())
        }
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error(ApiException.timeout(e))
    } catch (e: IOException) {
        NetworkResult.Error(ApiException.networkError(e))
    } catch (e: Throwable) {
        NetworkResult.Error(ApiException.unknown(e))
    }
}

/** For endpoints returning a raw [Response] (e.g. PDF/binary downloads) where headers/code matter. */
suspend fun <T> safeApiCallForResponse(apiCall: suspend () -> Response<T>): NetworkResult<T> {
    return try {
        val response = apiCall()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) NetworkResult.Success(body) else NetworkResult.Error(ApiException.unknown())
        } else {
            NetworkResult.Error(response.toApiException())
        }
    } catch (e: SocketTimeoutException) {
        NetworkResult.Error(ApiException.timeout(e))
    } catch (e: IOException) {
        NetworkResult.Error(ApiException.networkError(e))
    } catch (e: Throwable) {
        NetworkResult.Error(ApiException.unknown(e))
    }
}

private fun HttpException.toApiException(): ApiException {
    val errorBodyString = response()?.errorBody()?.string()
    val parsed = parseErrorBody(errorBodyString)
    return ApiException(
        httpCode = code(),
        code = parsed?.code ?: ApiException.CODE_UNKNOWN,
        message = parsed?.message ?: message(),
        fieldErrors = parsed?.fields ?: emptyMap(),
        cause = this,
    )
}

private fun <T> Response<T>.toApiException(): ApiException {
    val errorBodyString = errorBody()?.string()
    val parsed = parseErrorBody(errorBodyString)
    return ApiException(
        httpCode = code(),
        code = parsed?.code ?: ApiException.CODE_UNKNOWN,
        message = parsed?.message ?: message(),
        fieldErrors = parsed?.fields ?: emptyMap(),
    )
}

private fun parseErrorBody(body: String?): ErrorDetail? {
    if (body.isNullOrBlank()) return null
    return try {
        errorJson.decodeFromString(ErrorBody.serializer(), body).error
    } catch (t: Throwable) {
        null
    }
}
