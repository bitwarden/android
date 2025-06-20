package com.x8bit.bitwarden.data.platform.repository.util

import java.net.URLEncoder

private const val OTPAUTH_PREFIX = "otpauth://totp/"
private const val STEAM_PREFIX = "steam://"

/**
 * Utility for ensuring that a given TOTP string is a properly formatted otpauth:// or steam:// URI.
 * If the input TOTP is already a valid URI, it is returned as-is.
 * If the TOTP is manually entered and does not follow the URI format,
 * this function reconstructs it using the provided issuer and username.
 *
 * Uses this as a guide for format
 * https://github.com/google/google-authenticator/wiki/Key-Uri-Format
 *
 * Replace spaces (+) with %20, and encode the label and issuer (per the above link)
 * https://datatracker.ietf.org/doc/html/rfc5234
 * */
fun String?.sanitizeTotpUri(
    issuer: String?,
    username: String?,
): String? {
    if (this.isNullOrBlank()) return null

    return if (this.startsWith(OTPAUTH_PREFIX) || this.startsWith(STEAM_PREFIX)) {
        // ✅ Already a valid TOTP or Steam URI, return as-is.
        this
    } else {
        // ❌ Manually entered secret, reconstruct as otpauth://totp/ URI.

        // Trim spaces from issuer and username
        val trimmedIssuer = issuer
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val trimmedUsername = username
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        // Determine raw label correctly (avoid empty `:` issue)
        val rawLabel = if (trimmedIssuer != null && trimmedUsername != null) {
            "$trimmedIssuer:$trimmedUsername"
        } else {
            trimmedUsername
        }

        // Encode label only if it's not empty
        val encodedLabel = rawLabel
            ?.let {
                URLEncoder
                    .encode(it, "UTF-8")
                    .replace("+", "%20")
            }
            .orEmpty()

        // Encode issuer separately for the query parameter
        val encodedIssuer = trimmedIssuer?.let {
            URLEncoder
                .encode(it, "UTF-8")
                .replace("+", "%20")
        }

        // Construct the issuer query parameter.
        val issuerParameter = encodedIssuer
            ?.let { "&issuer=$it" }
            .orEmpty()

        // Remove spaces from the manually entered secret
        val sanitizedSecret = this.filterNot { it.isWhitespace() }

        // Construct final TOTP URI
        "$OTPAUTH_PREFIX$encodedLabel?secret=$sanitizedSecret$issuerParameter"
    }
}
