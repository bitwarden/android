package com.x8bit.bitwarden.ui.vault.feature.addedit.model

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * The options displayed to the user when choosing a match type
 * for their URI.
 */
@Suppress("MagicNumber")
enum class UriMatchDisplayType(
    val text: Text,
) {
    /**
     * the default option for when the user has not chosen one.
     */
    DEFAULT(R.string.default_text.asText()),

    /**
     * The URIs match if their top-level and second-level domains match.
     */
    BASE_DOMAIN(R.string.base_domain.asText()),

    /**
     * The URIs match if their hostnames (and ports if specified) match.
     */
    HOST(R.string.host.asText()),

    /**
     * The URIs match if the "test" URI starts with the known URI.
     */
    STARTS_WITH(R.string.starts_with.asText()),

    /**
     * The URIs match if the "test" URI matches the known URI according to a specified regular
     * expression for the item.
     */
    REGULAR_EXPRESSION(R.string.reg_ex.asText()),

    /**
     * The URIs match if they are exactly the same.
     */
    EXACT(R.string.exact.asText()),

    /**
     * The URIs should never match.
     */
    NEVER(R.string.never.asText()),
}
