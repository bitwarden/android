package com.x8bit.bitwarden.data.platform.repository.model

/**
 * Represents a way to match a known URI against a "test" one.
 *
 * The [value] is used for consistent storage purposes.
 */
@Suppress("MagicNumber")
enum class UriMatchType(
    val value: Int,
) {
    /**
     * The URIs match if their top-level and second-level domains match.
     */
    DOMAIN(0),

    /**
     * The URIs match if their hostnames (and ports if specified) match.
     */
    HOST(1),

    /**
     * The URIs match if the "test" URI starts with the known URI.
     */
    STARTS_WITH(2),

    /**
     * The URIs match if they are exactly the same.
     */
    EXACT(3),

    /**
     * The URIs match if the "test" URI matches the known URI according to a specified regular
     * expression for the item.
     */
    REGULAR_EXPRESSION(4),

    /**
     * The URIs should never match.
     */
    NEVER(5),
}
