package com.x8bit.bitwarden.data.platform.util

import android.content.Context
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import java.net.URI
import java.net.URISyntaxException

/**
 * The protocol for and Android app URI.
 */
private const val ANDROID_APP_PROTOCOL: String = "androidapp://"

/**
 * Try creating a [URI] out of this [String]. If it fails, return null.
 */
fun String.toUriOrNull(): URI? =
    try {
        URI(this)
    } catch (e: URISyntaxException) {
        null
    }

/**
 * Whether this [String] represents an android app URI.
 */
fun String.isAndroidApp(): Boolean =
    this.startsWith(ANDROID_APP_PROTOCOL)

/**
 * Try and extract the web host from this [String] if it represents an Android app.
 */
fun String.getWebHostFromAndroidUriOrNull(): String? =
    if (this.isAndroidApp()) {
        val components = this
            .replace(ANDROID_APP_PROTOCOL, "")
            .split('.')

        if (components.size > 1) {
            "${components[1]}.${components[0]}"
        } else {
            null
        }
    } else {
        null
    }

/**
 * Extract the domain name from this [String] if possible, otherwise return null.
 */
fun String.getDomainOrNull(context: Context): String? =
    this
        .toUriOrNull()
        ?.parseDomainOrNull(context = context)

/**
 * Extract the host with port from this [String] if possible, otherwise return null.
 */
@OmitFromCoverage
fun String.getHostWithPortOrNull(): String? =
    this
        .toUriOrNull()
        ?.let { uri ->
            val host = uri.host
            val port = uri.port

            if (host != null && port != -1) {
                "$host:$port"
            } else {
                null
            }
        }

/**
 * Find the indices of the last occurrences of [substring] within this [String]. Return null if no
 * occurrences are found.
 */
fun String.findLastSubstringIndicesOrNull(substring: String): IntRange? {
    val lastIndex = this.lastIndexOf(substring)

    return if (lastIndex != -1) {
        val endIndex = lastIndex + substring.length - 1
        IntRange(lastIndex, endIndex)
    } else {
        null
    }
}
