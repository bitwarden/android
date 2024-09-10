package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.net.Uri
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import java.net.URISyntaxException

/**
 * Attempts to parse a [Uri] from a string and returns null if an error occurs.
 */
@OmitFromCoverage
fun String.toUriOrNull(): Uri? =
    try {
        Uri.parse(this)
    } catch (e: URISyntaxException) {
        null
    }
