package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Returns a human-readable display label for the given [UriMatchType].
 */
val UriMatchType.displayLabel: Text
    get() = when (this) {
        UriMatchType.DOMAIN -> R.string.base_domain
        UriMatchType.HOST -> R.string.host
        UriMatchType.STARTS_WITH -> R.string.starts_with
        UriMatchType.REGULAR_EXPRESSION -> R.string.reg_ex
        UriMatchType.EXACT -> R.string.exact
        UriMatchType.NEVER -> R.string.never
    }
        .asText()
