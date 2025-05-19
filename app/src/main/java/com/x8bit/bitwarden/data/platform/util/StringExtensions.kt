package com.x8bit.bitwarden.data.platform.util

import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.platform.manager.ResourceCacheManager
import java.net.URI
import java.net.URISyntaxException

/**
 * The protocol for and Android app URI.
 */
private const val ANDROID_APP_PROTOCOL: String = "androidapp://"

/**
 * Regex to match an IP address (IPv4) with an optional port within the valid range (0-65535).
 *
 * This regex matches the following:
 * - An optional "http://" or "https://" prefix.
 * - A standard IPv4 address (four groups of digits separated by dots).
 * - An optional colon (:) followed by a port number (digits).
 * - The port number, if present, must be within the range of 0-65535.
 *
 * Example valid strings:
 * - 192.168.1.1
 * - 10.0.0.5:8080
 * - 255.255.255.0:9000
 * - 0.0.0.0
 * - 1.1.1.1:0
 * - 1.1.1.1:65535
 * - https://192.168.1.1
 * - https://10.0.0.5:8080
 * - http://255.255.255.0:9000
 * - http://10.0.0.5:8080
 *
 * Example invalid strings:
 * - 256.1.1.1 (invalid IP component)
 * - 10.1.1 (missing IP components)
 * - 10.1.1.1:abc (non-numeric port)
 * - 10.1.1.1:-1 (invalid port)
 * - 10.1.1.1: (missing port)
 * - 1.1.1.1:65536 (invalid port)
 * - 1.1.1.1:99999 (invalid port)
 * - ://192.168.1.1 (invalid scheme)
 * - androidapp://192.168.1.1 (invalid scheme)
 * - file://usr/docs/file.txt (invalid scheme)
 */
@Suppress("MaxLineLength")
private val IP_ADDRESS_WITH_OPTIONAL_PORT =
    Regex("""^(https?://)?((25[0-5]|(2[0-4]|1\d|[1-9]|)\d)\.?\b){4}(:([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5]))?$""")

/**
 * Try creating a [URI] out of this [String]. If it fails, return null.
 */
fun String.toUriOrNull(): URI? =
    try {
        // URI cannot parse IP addresses without a scheme, so add one if necessary.
        if (this.isIpAddress() && !this.hasHttpProtocol()) {
            URI("https://$this")
        } else {
            URI(this)
        }
    } catch (_: URISyntaxException) {
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
 * Whether this [String] represents an IP address with an optional port.
 */
fun String.isIpAddress(): Boolean =
    this.matches(IP_ADDRESS_WITH_OPTIONAL_PORT)

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
