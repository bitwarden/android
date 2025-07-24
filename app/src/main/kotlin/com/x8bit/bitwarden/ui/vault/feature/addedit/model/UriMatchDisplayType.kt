package com.x8bit.bitwarden.ui.vault.feature.addedit.model

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

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
    DEFAULT(BitwardenString.default_text.asText()),

    /**
     * The URIs match if their top-level and second-level domains match.
     */
    BASE_DOMAIN(BitwardenString.base_domain.asText()),

    /**
     * The URIs match if their hostnames (and ports if specified) match.
     */
    HOST(BitwardenString.host.asText()),

    /**
     * The URIs match if the "test" URI starts with the known URI.
     */
    STARTS_WITH(BitwardenString.starts_with.asText()),

    /**
     * The URIs match if the "test" URI matches the known URI according to a specified regular
     * expression for the item.
     */
    REGULAR_EXPRESSION(BitwardenString.reg_ex.asText()),

    /**
     * The URIs match if they are exactly the same.
     */
    EXACT(BitwardenString.exact.asText()),

    /**
     * The URIs should never match.
     */
    NEVER(BitwardenString.never.asText()),
}
