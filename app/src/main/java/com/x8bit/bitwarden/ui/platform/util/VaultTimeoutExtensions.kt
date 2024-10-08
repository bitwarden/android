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
        VaultTimeout.Type.FIFTEEN_MINUTES -> R.string.fifteen_minutes
        VaultTimeout.Type.THIRTY_MINUTES -> R.string.thirty_minutes
        VaultTimeout.Type.ONE_HOUR -> R.string.one_hour
        VaultTimeout.Type.FOUR_HOURS -> R.string.four_hours
        VaultTimeout.Type.ON_APP_RESTART -> R.string.on_restart
        VaultTimeout.Type.NEVER -> R.string.never
        VaultTimeout.Type.CUSTOM -> R.string.custom
    }
        .asText()

/**
 * The value in minutes for the given [VaultTimeout.Type], used as a comparison
 * against the maximum timeout allowed by the organization's policy.
 */
@Suppress("MagicNumber")
val VaultTimeout.Type.minutes: Int
    get() = when (this) {
        VaultTimeout.Type.IMMEDIATELY -> 0
        VaultTimeout.Type.ONE_MINUTE -> 1
        VaultTimeout.Type.FIVE_MINUTES -> 5
        VaultTimeout.Type.FIFTEEN_MINUTES -> 15
        VaultTimeout.Type.THIRTY_MINUTES -> 30
        VaultTimeout.Type.ONE_HOUR -> 60
        VaultTimeout.Type.FOUR_HOURS -> 240

        VaultTimeout.Type.ON_APP_RESTART,
        VaultTimeout.Type.NEVER,
            -> Int.MAX_VALUE

        VaultTimeout.Type.CUSTOM -> 0
    }
