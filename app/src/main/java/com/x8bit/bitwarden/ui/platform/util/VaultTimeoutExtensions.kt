package com.x8bit.bitwarden.ui.platform.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Provides a human-readable display label for the given [VaultTimeout.Type].
 */
val VaultTimeout.Type.displayLabel: Text
    get() = when (this) {
        VaultTimeout.Type.IMMEDIATELY -> R.string.immediately
        VaultTimeout.Type.ONE_MINUTE -> R.string.one_minute
        VaultTimeout.Type.FIVE_MINUTES -> R.string.five_minutes
        VaultTimeout.Type.THIRTY_MINUTES -> R.string.thirty_minutes
        VaultTimeout.Type.ONE_HOUR -> R.string.one_hour
        VaultTimeout.Type.FOUR_HOURS -> R.string.four_hours
        VaultTimeout.Type.ON_APP_RESTART -> R.string.on_restart
        VaultTimeout.Type.NEVER -> R.string.never
        VaultTimeout.Type.CUSTOM -> R.string.custom
    }
        .asText()
