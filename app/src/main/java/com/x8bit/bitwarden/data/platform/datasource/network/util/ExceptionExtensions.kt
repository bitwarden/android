package com.x8bit.bitwarden.data.platform.datasource.network.util

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import retrofit2.HttpException

/**
 * Attempt to parse the error body to serializable type [T].
 *
 * Useful in service layer for parsing non-200 response bodies.
 *
 * If the receiver is not an [HttpException] or the error body cannot be parsed, the original
 * Throwable will be returned as a Result.failure.
 *
 * @param code HTTP code associated with the error. Only responses with this code will be attempted
 * to be parsed.
 * @param json [Json] serializer to use.
 */
@OptIn(ExperimentalSerializationApi::class)
inline fun <reified T> Throwable.parseErrorBodyAsResult(code: Int, json: Json): Result<T> =
    (this as? HttpException)
        ?.response()
        ?.takeIf { it.code() == code }
        ?.errorBody()
        ?.let { errorBody ->
            try {
                Result.success(json.decodeFromStream(errorBody.byteStream()))
            } catch (_: Exception) {
                Result.failure(this)
            }
        } ?: Result.failure(this)
