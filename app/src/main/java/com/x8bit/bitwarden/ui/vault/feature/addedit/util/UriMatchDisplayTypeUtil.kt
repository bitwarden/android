package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriMatchDisplayType

/**
 * Method to convert the SDK match type for display to the user.
 */
fun UriMatchType?.toDisplayMatchType(): UriMatchDisplayType =
    when (this) {
        UriMatchType.DOMAIN -> UriMatchDisplayType.BASE_DOMAIN
        UriMatchType.EXACT -> UriMatchDisplayType.EXACT
        UriMatchType.HOST -> UriMatchDisplayType.HOST
        UriMatchType.NEVER -> UriMatchDisplayType.NEVER
        UriMatchType.REGULAR_EXPRESSION -> UriMatchDisplayType.REGULAR_EXPRESSION
        UriMatchType.STARTS_WITH -> UriMatchDisplayType.STARTS_WITH
        null -> UriMatchDisplayType.DEFAULT
    }

/**
 * Method to convert the match display type over to the SDK match type.
 */
fun UriMatchDisplayType.toUriMatchType(): UriMatchType? =
    when (this) {
        UriMatchDisplayType.DEFAULT -> null
        UriMatchDisplayType.BASE_DOMAIN -> UriMatchType.DOMAIN
        UriMatchDisplayType.HOST -> UriMatchType.HOST
        UriMatchDisplayType.STARTS_WITH -> UriMatchType.STARTS_WITH
        UriMatchDisplayType.REGULAR_EXPRESSION -> UriMatchType.REGULAR_EXPRESSION
        UriMatchDisplayType.EXACT -> UriMatchType.EXACT
        UriMatchDisplayType.NEVER -> UriMatchType.NEVER
    }
