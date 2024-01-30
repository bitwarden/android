package com.x8bit.bitwarden.data.platform.datasource.network.util

import com.x8bit.bitwarden.data.platform.datasource.network.model.BitwardenError
import com.x8bit.bitwarden.data.platform.util.decodeFromStringOrNull
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
 * @param codes a list of HTTP codes associated with the error. Only responses with a matching code
 * will be attempted to be parsed.
 * @param json [Json] serializer to use.
 */
inline fun <reified T> BitwardenError.parseErrorBodyOrNull(codes: List<Int>, json: Json): T? =
    (this as? BitwardenError.Http)
        ?.takeIf { codes.any { it == this.code } }
        ?.responseBodyString
        ?.let { responseBody ->
            json.decodeFromStringOrNull(responseBody)
        }

/**
 * Helper for calling [parseErrorBodyOrNull] with a single code.
 */
inline fun <reified T> BitwardenError.parseErrorBodyOrNull(code: Int, json: Json): T? =
    parseErrorBodyOrNull(listOf(code), json)
