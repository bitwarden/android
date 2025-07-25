package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType

/**
 * Returns a human-readable display label for the given [UriMatchType].
 */
val UriMatchType.displayLabel: Text
    get() = when (this) {
        UriMatchType.DOMAIN -> BitwardenString.base_domain
        UriMatchType.HOST -> BitwardenString.host
        UriMatchType.STARTS_WITH -> BitwardenString.starts_with
        UriMatchType.REGULAR_EXPRESSION -> BitwardenString.reg_ex
        UriMatchType.EXACT -> BitwardenString.exact
        UriMatchType.NEVER -> BitwardenString.never
    }
        .asText()

/**
 * Convert this internal [UriMatchType] to the sdk model.
 */
fun UriMatchType.toSdkUriMatchType(): com.bitwarden.vault.UriMatchType =
    when (this) {
        UriMatchType.DOMAIN -> com.bitwarden.vault.UriMatchType.DOMAIN
        UriMatchType.EXACT -> com.bitwarden.vault.UriMatchType.EXACT
        UriMatchType.HOST -> com.bitwarden.vault.UriMatchType.HOST
        UriMatchType.NEVER -> com.bitwarden.vault.UriMatchType.NEVER
        UriMatchType.REGULAR_EXPRESSION -> com.bitwarden.vault.UriMatchType.REGULAR_EXPRESSION
        UriMatchType.STARTS_WITH -> com.bitwarden.vault.UriMatchType.STARTS_WITH
    }

/**
 * Checks if the [UriMatchType] is considered an advanced matching strategy.
 */
fun UriMatchType.isAdvancedMatching(): Boolean =
    when (this) {
        UriMatchType.REGULAR_EXPRESSION,
        UriMatchType.STARTS_WITH,
            -> true

        else -> false
    }
