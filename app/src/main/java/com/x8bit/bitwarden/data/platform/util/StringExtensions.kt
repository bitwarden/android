package com.x8bit.bitwarden.data.platform.util

import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
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
 * Whether this [String] starts with an http or https protocol.
 */
fun String.hasHttpProtocol(): Boolean =
    this.startsWith(prefix = "http://") || this.startsWith(prefix = "https://")

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
fun String.getDomainOrNull(resourceCacheManager: ResourceCacheManager): String? =
    this
        .toUriOrNull()
        ?.addSchemeToUriIfNecessary()
        ?.parseDomainOrNull(resourceCacheManager = resourceCacheManager)

/**
 * Returns `true` if the [String] uri has a port, `false` otherwise.
 */
@OmitFromCoverage
fun String.hasPort(): Boolean {
    val uri = this
        .toUriOrNull()
        ?.addSchemeToUriIfNecessary()
        ?: return false
    return uri.port != -1
}

/**
 * Extract the host from this [String] if possible, otherwise return null.
 */
@OmitFromCoverage
fun String.getHostOrNull(): String? = this.toUriOrNull()
    ?.addSchemeToUriIfNecessary()
    ?.host

/**
 * Extract the host with optional port from this [String] if possible, otherwise return null.
 */
@OmitFromCoverage
fun String.getHostWithPortOrNull(): String? {
    val uri = this
        .toUriOrNull()
        ?.addSchemeToUriIfNecessary()
        ?: return null
    return uri.host?.let { host ->
        val port = uri.port
        if (port != -1) {
            "$host:$port"
        } else {
            host
        }
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
