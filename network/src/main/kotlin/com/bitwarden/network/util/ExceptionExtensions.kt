package com.bitwarden.network.util

import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.network.model.BitwardenError
import kotlinx.serialization.json.Json
import retrofit2.HttpException

/**
 * Returns the [NetworkErrorCode] for the given error if it is available.
 */
internal fun BitwardenError.getNetworkErrorCodeOrNull(): NetworkErrorCode? =
    (this as? BitwardenError.Http)?.let { httpError ->
        NetworkErrorCode.entries.firstOrNull { httpError.code == it.code }
    }

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
internal inline fun <reified T> BitwardenError.parseErrorBodyOrNull(
    codes: List<NetworkErrorCode>,
    json: Json,
): T? =
    (this as? BitwardenError.Http)
        ?.takeIf { codes.any { it.code == this.code } }
        ?.responseBodyString
        ?.let { responseBody ->
            json.decodeFromStringOrNull(responseBody)
        }

/**
 * Helper for calling [parseErrorBodyOrNull] with a single code.
 */
internal inline fun <reified T> BitwardenError.parseErrorBodyOrNull(
    code: NetworkErrorCode,
    json: Json,
): T? = parseErrorBodyOrNull(codes = listOf(code), json = json)
