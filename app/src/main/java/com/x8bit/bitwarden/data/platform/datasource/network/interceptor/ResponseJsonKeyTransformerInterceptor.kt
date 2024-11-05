package com.x8bit.bitwarden.data.platform.datasource.network.interceptor

import com.x8bit.bitwarden.data.platform.util.parseToJsonElementOrNull
import com.x8bit.bitwarden.data.platform.util.transformKeysToCamelCase
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody

/**
 * An OkHttp interceptor that transforms the JSON response body by converting all JSON object keys
 * to camel case.
 *
 * This interceptor is useful for handling inconsistencies in JSON responses where key casing might
 * vary.
 */
class ResponseJsonKeyTransformerInterceptor(private val json: Json) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        val responseBody = response.body
            ?: return response

        val transformedJsonResponseBody = json
            .parseToJsonElementOrNull(responseBody.string())
            ?.transformKeysToCamelCase()
            ?.toString()
            ?.toResponseBody(response.body?.contentType())
            ?: return response

        return response.newBuilder()
            .body(transformedJsonResponseBody)
            .build()
    }
}
