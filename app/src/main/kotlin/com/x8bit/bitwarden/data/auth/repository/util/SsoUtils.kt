package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Base64

private const val SSO_HOST: String = "sso-callback"
const val SSO_URI: String = "bitwarden://$SSO_HOST"

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
): String {
    val redirectUri = URLEncoder.encode(SSO_URI, "UTF-8")
    val encodedOrganizationIdentifier = URLEncoder.encode(organizationIdentifier, "UTF-8")
    val encodedToken = URLEncoder.encode(token, "UTF-8")

    val codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest
            .getInstance("SHA-256")
            .digest(codeVerifier.toByteArray()),
    )

    return "$identityBaseUrl/connect/authorize" +
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
    val localData = data
    return if (action == Intent.ACTION_VIEW && localData?.host == SSO_HOST) {
        val state = localData.getQueryParameter("state")
        val code = localData.getQueryParameter("code")
        if (code != null) {
            SsoCallbackResult.Success(
                state = state,
                code = code,
            )
        } else {
            SsoCallbackResult.MissingCode
        }
    } else {
        null
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
