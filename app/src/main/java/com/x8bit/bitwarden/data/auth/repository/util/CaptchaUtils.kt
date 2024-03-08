package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.net.URLEncoder
import java.util.Base64
import java.util.Locale

private const val CAPTCHA_HOST: String = "captcha-callback"
private const val CALLBACK_URI = "bitwarden://$CAPTCHA_HOST"

/**
 * Generates a [Uri] to display a CAPTCHA challenge for Bitwarden authentication.
 */
fun generateUriForCaptcha(captchaId: String): Uri {
    val json = buildJsonObject {
        put(key = "siteKey", value = captchaId)
        put(key = "locale", value = Locale.getDefault().toString())
        put(key = "callbackUri", value = CALLBACK_URI)
        put(key = "captchaRequiredText", value = "Captcha required")
    }
    val base64Data = Base64
        .getEncoder()
        .encodeToString(
            json
                .toString()
                .toByteArray(Charsets.UTF_8),
        )
    val parentParam = URLEncoder.encode(CALLBACK_URI, "UTF-8")
    val url = "https://vault.bitwarden.com/captcha-mobile-connector.html" +
        "?data=$base64Data&parent=$parentParam&v=1"
    return Uri.parse(url)
}

/**
 * Retrieves a [CaptchaCallbackTokenResult] from an Intent. There are three possible cases.
 *
 * - `null`: Intent is not a captcha callback, or data is null.
 *
 * - [CaptchaCallbackTokenResult.MissingToken]:
 * Intent is the captcha callback, but its missing a token value.
 *
 * - [CaptchaCallbackTokenResult.Success]:
 * Intent is the captcha callback, and it has a token.
 */
fun Intent.getCaptchaCallbackTokenResult(): CaptchaCallbackTokenResult? {
    val localData = data
    return if (
        action == Intent.ACTION_VIEW && localData != null && localData.host == CAPTCHA_HOST
    ) {
        localData.getQueryParameter("token")?.let {
            CaptchaCallbackTokenResult.Success(token = it)
        } ?: CaptchaCallbackTokenResult.MissingToken
    } else {
        null
    }
}

/**
 * Sealed class representing the result of captcha callback token extraction.
 */
sealed class CaptchaCallbackTokenResult {
    /**
     * Represents a missing token in the captcha callback.
     */
    data object MissingToken : CaptchaCallbackTokenResult()

    /**
     * Represents a token present in the captcha callback.
     */
    data class Success(val token: String) : CaptchaCallbackTokenResult()
}
