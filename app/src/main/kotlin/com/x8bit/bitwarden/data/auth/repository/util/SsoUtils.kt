package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import android.os.Parcelable
import androidx.browser.auth.AuthTabIntent
import androidx.core.net.toUri
import com.bitwarden.annotation.OmitFromCoverage
import kotlinx.parcelize.Parcelize
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64

private const val BITWARDEN_EU_HOST: String = "bitwarden.eu"
private const val BITWARDEN_US_HOST: String = "bitwarden.com"
private const val APP_LINK_SCHEME: String = "https"
private const val DEEPLINK_SCHEME: String = "bitwarden"
private const val CALLBACK: String = "sso-callback"

const val SSO_URI: String = "bitwarden://$CALLBACK"

/**
 * Generates a URI for the SSO custom tab.
 *
 * @param identityBaseUrl The base URl for the identity service.
 * @param organizationIdentifier The SSO organization identifier.
 * @param token The prevalidated SSO token.
 * @param state Random state used to verify the validity of the response.
 * @param codeVerifier A random string used to generate the code challenge.
 */
fun generateUriForSso(
    identityBaseUrl: String,
    organizationIdentifier: String,
    token: String,
    state: String,
    codeVerifier: String,
): Uri {
    val redirectUri = URLEncoder.encode(SSO_URI, "UTF-8")
    val encodedOrganizationIdentifier = URLEncoder.encode(organizationIdentifier, "UTF-8")
    val encodedToken = URLEncoder.encode(token, "UTF-8")

    val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest
            .getInstance("SHA-256")
            .digest(codeVerifier.toByteArray()),
    )

    val uri = "$identityBaseUrl/connect/authorize" +
        "?client_id=mobile" +
        "&redirect_uri=$redirectUri" +
        "&response_type=code" +
        "&scope=api%20offline_access" +
        "&state=$state" +
        "&code_challenge=$codeChallenge" +
        "&code_challenge_method=S256" +
        "&response_mode=query" +
        "&domain_hint=$encodedOrganizationIdentifier" +
        "&ssoToken=$encodedToken"
    return uri.toUri()
}

/**
 * Retrieves an [SsoCallbackResult] from an Intent. There are three possible cases.
 *
 * - `null`: Intent is not an SSO callback, or data is null.
 *
 * - [SsoCallbackResult.MissingCode]: Intent is the SSO callback, but it's missing the needed code.
 *
 * - [SsoCallbackResult.Success]: Intent is the SSO callback with required data.
 */
fun Intent.getSsoCallbackResult(): SsoCallbackResult? {
    if (action != Intent.ACTION_VIEW) return null
    val localData = data ?: return null
    return when (localData.scheme) {
        DEEPLINK_SCHEME -> {
            if (localData.host == CALLBACK) {
                localData.getSsoCallbackResult()
            } else {
                null
            }
        }

        APP_LINK_SCHEME -> {
            if ((localData.host == BITWARDEN_US_HOST || localData.host == BITWARDEN_EU_HOST) &&
                localData.path == "/$CALLBACK"
            ) {
                localData.getSsoCallbackResult()
            } else {
                null
            }
        }

        else -> null
    }
}

/**
 * Retrieves an [SsoCallbackResult] from an [AuthTabIntent.AuthResult]. There are two possible
 * cases.
 *
 * - [SsoCallbackResult.MissingCode]: The code is missing.
 * - [SsoCallbackResult.Success]: The relevant data is present.
 */
@OmitFromCoverage
fun AuthTabIntent.AuthResult.getSsoCallbackResult(): SsoCallbackResult =
    when (this.resultCode) {
        AuthTabIntent.RESULT_OK -> this.resultUri.getSsoCallbackResult()
        AuthTabIntent.RESULT_CANCELED -> SsoCallbackResult.MissingCode
        AuthTabIntent.RESULT_UNKNOWN_CODE -> SsoCallbackResult.MissingCode
        AuthTabIntent.RESULT_VERIFICATION_FAILED -> SsoCallbackResult.MissingCode
        AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> SsoCallbackResult.MissingCode
        else -> SsoCallbackResult.MissingCode
    }

private fun Uri?.getSsoCallbackResult(): SsoCallbackResult {
    val state = this?.getQueryParameter("state")
    val code = this?.getQueryParameter("code")
    return if (code != null) {
        SsoCallbackResult.Success(state = state, code = code)
    } else {
        SsoCallbackResult.MissingCode
    }
}

/**
 * Sealed class representing the result of an SSO callback data extraction.
 */
sealed class SsoCallbackResult : Parcelable {
    /**
     * Represents an SSO callback object with a missing code value.
     */
    @Parcelize
    data object MissingCode : SsoCallbackResult()

    /**
     * Represents an SSO callback object with the necessary [state] and [code]. `state` being
     * present doesn't guarantee it is correct, and should be checked against the known state before
     * being used.
     */
    @Parcelize
    data class Success(
        val state: String?,
        val code: String,
    ) : SsoCallbackResult()
}
