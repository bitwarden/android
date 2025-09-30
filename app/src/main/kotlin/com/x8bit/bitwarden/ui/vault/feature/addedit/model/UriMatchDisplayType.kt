package com.x8bit.bitwarden.ui.vault.feature.addedit.model

/**
 * The options displayed to the user when choosing a match type
 * for their URI.
 */
@Suppress("MagicNumber")
enum class UriMatchDisplayType {
    /**
     * the default option for when the user has not chosen one.
     */
    DEFAULT,

    /**
     * The URIs match if their top-level and second-level domains match.
     */
    BASE_DOMAIN,

    /**
     * The URIs match if their hostnames (and ports if specified) match.
     */
    HOST,

    /**
     * The URIs match if the "test" URI starts with the known URI.
     */
    STARTS_WITH,

    /**
     * The URIs match if the "test" URI matches the known URI according to a specified regular
     * expression for the item.
     */
    REGULAR_EXPRESSION,

    /**
     * The URIs match if they are exactly the same.
     */
    EXACT,

    /**
     * The URIs should never match.
     */
    NEVER,
}
