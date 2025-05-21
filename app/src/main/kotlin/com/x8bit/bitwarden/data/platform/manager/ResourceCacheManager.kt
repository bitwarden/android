package com.x8bit.bitwarden.data.platform.manager

/**
 * A manager for caching resources that are large and could be performance impacting to load
 * multiple times.
 */
interface ResourceCacheManager {
    /**
     * Retrieves the exception suffix list used for matching a cipher against a domain.
     */
    val domainExceptionSuffixes: List<String>

    /**
     * Retrieves the normal suffix list used for matching a cipher against a domain.
     */
    val domainNormalSuffixes: List<String>

    /**
     * Retrieves the wild card suffix list used for matching a cipher against a domain.
     */
    val domainWildCardSuffixes: List<String>
}
