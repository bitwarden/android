package com.x8bit.bitwarden.e2e.seeder

import com.x8bit.bitwarden.e2e.config.E2eConfig
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import timber.log.Timber
import java.io.IOException
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Minimal SeederApi client: `POST /seed` to create data and `DELETE /seed/{playId}` to remove it.
 * Every request carries this client's [playId], so one call deletes everything it seeded.
 */
class SeederClient(private val config: E2eConfig) {
    /**
     * Identifies all data seeded by this client.
     */
    val playId: String = UUID.randomUUID().toString()

    /**
     * Seeds the scene [template] with [arguments] and returns the decoded response.
     */
    fun <A, R> seed(
        template: String,
        arguments: A,
        argumentsSerializer: KSerializer<A>,
        resultSerializer: KSerializer<R>,
    ): SeedResponse<R> {
        val body = buildJsonObject {
            put("template", template)
            put("arguments", json.encodeToJsonElement(argumentsSerializer, arguments))
        }
        val request = requestFor("${config.seederUrl}/seed")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()
        httpClient.newCall(request).execute().use { response ->
            val text = response.body.string()
            check(response.isSuccessful) {
                "Seeding $template failed: HTTP ${response.code} ${text.take(MAX_ERROR_BODY)}"
            }
            return json.decodeFromString(SeedResponse.serializer(resultSerializer), text)
        }
    }

    /**
     * Deletes everything seeded with [playId]. Logs instead of throwing, so cleanup never hides
     * the real test failure.
     */
    fun deleteSeededData() {
        val request = requestFor("${config.seederUrl}/seed/$playId").delete().build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Timber.w("Cleanup of play id $playId failed: HTTP ${response.code}")
                }
            }
        } catch (e: IOException) {
            Timber.w(e, "Cleanup of play id $playId failed")
        }
    }

    private fun requestFor(url: String): Request.Builder =
        Request.Builder()
            .url(url)
            .header("Authorization", "Basic ${config.seederToken}")
            .header("x-play-id", playId)
}

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

// Shared so tests don't each create their own connection pool. Seeding can be slow on a cold
// server, so the read timeout is longer than OkHttp's default.
private val httpClient = OkHttpClient.Builder()
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .build()

private val JSON_MEDIA_TYPE = "application/json".toMediaType()

private const val MAX_ERROR_BODY = 500
