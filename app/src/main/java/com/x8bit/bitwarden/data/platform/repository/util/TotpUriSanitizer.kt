package com.x8bit.bitwarden.data.platform.repository.util

import java.net.URLEncoder

/**
 * Utility for ensuring that a given TOTP string is a properly formatted otpauth:// URI.
 * If the input TOTP is already a valid URI, it is returned as-is.
 * If the TOTP is manually entered and does not follow the URI format,
 * this function reconstructs it using the provided issuer and email.
 *
 * Uses this as a guide for format
 * https://github.com/google/google-authenticator/wiki/Key-Uri-Format
 */
fun String?.sanitizeTotpUri(
    issuer: String?,
    email: String?,
): String? {
    if (this.isNullOrBlank()) return null

    return if (this.startsWith("otpauth://totp/")) {
        // ✅ Already a valid TOTP URI, return as-is.
        this
    } else {
        // ❌ Manually entered secret, reconstructing a proper otpauth:// URI.

        // Trim spaces from issuer and account name **before encoding**
        val trimmedIssuer = issuer?.trim()
        val trimmedEmail = email?.trim()

        // Determine raw label correctly (avoid empty `:` issue)
        val rawLabel = when {
            // Fully empty label
            trimmedIssuer.isNullOrBlank() && trimmedEmail.isNullOrBlank() -> ""
            // Both exist, add `:` between them
            trimmedIssuer != null && trimmedEmail != null -> "$trimmedIssuer:$trimmedEmail"
            // Only account name, no `:`
            trimmedEmail != null -> trimmedEmail
            // Only issuer exists, but we don't add a label
            else -> ""
        }

        // Encode label only if it's not empty
        val encodedLabel = rawLabel.takeIf { it.isNotEmpty() }?.let {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        } ?: ""

        //  Encode issuer separately for the query parameter
        val encodedIssuer = trimmedIssuer?.takeIf { it.isNotBlank() }?.let {
            URLEncoder.encode(it, "UTF-8").replace("+", "%20")
        }

        val issuerParameter = encodedIssuer?.let { "&issuer=$it" } ?: ""

        // Remove spaces from the manually entered secret
        val sanitizedSecret = this.replace("\\s".toRegex(), "")

        // Construct final TOTP URI
        "otpauth://totp/$encodedLabel?secret=$sanitizedSecret$issuerParameter"
    }
}
