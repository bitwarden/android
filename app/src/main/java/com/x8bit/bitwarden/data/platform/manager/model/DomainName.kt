package com.x8bit.bitwarden.data.platform.manager.model

/**
 * A data class containing the result of parsing the URL.
 *
 * Example:
 * - URL: m.google.com
 * - domain: google.com
 * - secondLevelDomain: google
 * - subDomain: m
 * - topLevelDomain: com
 *
 * @property secondLevelDomain The second-level domain of the URL, if it exists.
 * @property subDomain The subdomain of the URL.
 * @property topLevelDomain The top-level domain (TLD) of the URL.
 */
data class DomainName(
    val secondLevelDomain: String?,
    val subDomain: String?,
    val topLevelDomain: String,
) {
    /**
     * The domain of the URL, constructed from the second-level and top-level domains.
     */
    val domain: String
        get() = "$secondLevelDomain.$topLevelDomain"
}
