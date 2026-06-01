package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Validates the URI based on specific criteria.
 *
 * The validation checks include:
 * - The URI should start with "https://", "http://", or "androidapp://".
 * - The URI should not end immediately after the scheme part.
 * - The URI should not be a duplicate of any existing URIs in the provided list.
 * - The URI should match a specific valid pattern
 *
 * This function will return the error message or null if there is no error.
 */
fun String.validateUri(existingUris: List<String>): Text? {

    // Check if URI starts with allowed schemes.
    if (
        !startsWith("https://") &&
        !startsWith("http://") &&
        !startsWith("androidapp://")
    ) {
        return BitwardenString.invalid_format_use_https_http_or_android_app.asText()
    }

    // Check for specific invalid patterns.
    if (!isValidPattern()) {
        return BitwardenString.invalid_uri.asText()
    }

    // Check for duplicates.
    if (this in existingUris) {
        return BitwardenString.the_urix_is_already_blocked.asText(this)
    }

    // Return null to indicate no errors.
    return null
}

/**
 * Checks if the string matches a specific URI pattern.
 */
fun String.isValidPattern(): Boolean {
    val pattern = "^(https?|androidapp)://([A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*)(/.*)?$".toRegex()
    return matches(pattern)
}
