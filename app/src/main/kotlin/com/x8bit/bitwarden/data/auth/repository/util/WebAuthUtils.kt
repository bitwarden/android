package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64

private val BITWARDEN_HOSTS: List<String> = listOf("bitwarden.com", "bitwarden.eu", "bitwarden.pw")
private const val APP_LINK_SCHEME: String = "https"
private const val DEEPLINK_SCHEME: String = "bitwarden"
private const val CALLBACK: String = "webauthn-callback"

/**
 * Retrieves an [WebAuthResult] from an [Intent]. There are three possible cases.
 *
 * - `null`: Intent is not an web auth key callback.
 * - [WebAuthResult.Success]: Intent is the web auth key callback with correct data.
 * - [WebAuthResult.Failure]: Intent is the web auth key callback with incorrect data.
 */
fun Intent.getWebAuthResultOrNull(): WebAuthResult? {
    if (action != Intent.ACTION_VIEW) return null
    val localData = data ?: return null
    return when (localData.scheme) {
        DEEPLINK_SCHEME -> {
            if (localData.host == CALLBACK) {
                localData.getWebAuthResult()
            } else {
                null
            }
        }

        APP_LINK_SCHEME -> {
            if (localData.host in BITWARDEN_HOSTS && localData.path == "/$CALLBACK") {
                localData.getWebAuthResult()
            } else {
                null
            }
        }

        else -> null
    }
}

/**
 * Retrieves an [WebAuthResult] from an [AuthTabIntent.AuthResult]. There are two possible cases.
 *
 * - [WebAuthResult.Success]: The URI is the web auth key callback with correct data.
 * - [WebAuthResult.Failure]: The URI is the web auth key callback with incorrect data or a failure
 * has occurred.
 */
@OmitFromCoverage
fun AuthTabIntent.AuthResult.getWebAuthResult(): WebAuthResult =
    when (this.resultCode) {
        AuthTabIntent.RESULT_OK -> this.resultUri.getWebAuthResult()
        AuthTabIntent.RESULT_CANCELED -> WebAuthResult.Failure(message = null)
        AuthTabIntent.RESULT_UNKNOWN_CODE -> WebAuthResult.Failure(message = null)
        AuthTabIntent.RESULT_VERIFICATION_FAILED -> WebAuthResult.Failure(message = null)
        AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> WebAuthResult.Failure(message = null)
        else -> WebAuthResult.Failure(message = null)
    }

private fun Uri?.getWebAuthResult(): WebAuthResult =
    this
        ?.getQueryParameter("data")
        ?.let { WebAuthResult.Success(token = it) }
        ?: WebAuthResult.Failure(message = this?.getQueryParameter("error"))

/**
 * Generates a [Uri] to display a web authn challenge for Bitwarden authentication.
 */
@Suppress("LongParameterList")
fun generateUriForWebAuth(
    baseUrl: String,
    callbackScheme: String,
    data: JsonObject,
    headerText: String,
    buttonText: String,
    returnButtonText: String,
): Uri {
    val json = buildJsonObject {
        put(key = "data", value = data.toString())
        put(key = "headerText", value = headerText)
        put(key = "btnText", value = buttonText)
        put(key = "btnReturnText", value = returnButtonText)
        put(key = "mobile", value = true)
    }
    val base64Data = Base64
        .getEncoder()
        .encodeToString(json.toString().toByteArray(Charsets.UTF_8))
    val url = baseUrl +
        "/webauthn-mobile-connector.html" +
        "?data=$base64Data" +
        "&client=mobile" +
        "&v=2" +
        "&deeplinkScheme=$callbackScheme"
    return url.toUri()
}

/**
 * Sealed class representing the result of web auth callback token extraction.
 */
sealed class WebAuthResult {
    /**
     * Represents a token present in the web auth callback.
     */
    data class Success(val token: String) : WebAuthResult()

    /**
     * Represents a failure in the web auth callback.
     */
    data class Failure(val message: String?) : WebAuthResult()
}
