package com.x8bit.bitwarden.data.platform.datasource.network.util

import com.x8bit.bitwarden.data.platform.datasource.network.model.BitwardenError
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Attempt to parse the error body to serializable type [T].
 *
 * Useful in service layer for parsing non-200 response bodies.
 *
 * If the receiver is not an [HttpException] or the error body cannot be parsed, null will be
 * returned.
 *
 * @param code HTTP code associated with the error. Only responses with this code will be attempted
 * to be parsed.
 * @param json [Json] serializer to use.
 */
inline fun <reified T> BitwardenError.parseErrorBodyOrNull(code: Int, json: Json): T? =
    (this as? BitwardenError.Http)
        ?.takeIf { it.code == code }
        ?.responseBodyString
        ?.let { responseBody ->
            try {
                json.decodeFromString(responseBody)
            } catch (_: Exception) {
                null
            }
        }
