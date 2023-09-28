package com.x8bit.bitwarden.data.auth.datasource.network.util

import android.content.Intent
import android.net.Uri
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.Base64
import java.util.Locale

/**
 * Generates an [Intent] to display a CAPTCHA challenge for Bitwarden authentication.
 */
fun LoginResult.CaptchaRequired.generateIntentForCaptcha(): Intent {
    val json = buildJsonObject {
        put(key = "siteKey", value = captchaId)
        put(key = "locale", value = Locale.getDefault().toString())
        put(key = "callbackUri", value = "bitwarden://captcha-callback")
        put(key = "captchaRequiredText", value = "Captcha required")
    }
    val base64Data = Base64
        .getEncoder()
        .encodeToString(
            json
                .toString()
                .toByteArray(Charsets.UTF_8),
        )
    val parentParam = "bitwarden%3A%2F%2Fcaptcha-callback"
    val url = "https://vault.bitwarden.com/captcha-mobile-connector.html" +
        "?data=$base64Data&parent=$parentParam&v=1"
    return Intent(Intent.ACTION_VIEW, Uri.parse(url))
}
