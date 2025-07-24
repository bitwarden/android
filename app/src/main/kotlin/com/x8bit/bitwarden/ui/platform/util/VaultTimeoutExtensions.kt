package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeout

/**
 * Provides a human-readable display label for the given [VaultTimeout.Type].
 */
val VaultTimeout.Type.displayLabel: Text
    get() = when (this) {
        VaultTimeout.Type.IMMEDIATELY -> BitwardenString.immediately
        VaultTimeout.Type.ONE_MINUTE -> BitwardenString.one_minute
        VaultTimeout.Type.FIVE_MINUTES -> BitwardenString.five_minutes
        VaultTimeout.Type.FIFTEEN_MINUTES -> BitwardenString.fifteen_minutes
        VaultTimeout.Type.THIRTY_MINUTES -> BitwardenString.thirty_minutes
        VaultTimeout.Type.ONE_HOUR -> BitwardenString.one_hour
        VaultTimeout.Type.FOUR_HOURS -> BitwardenString.four_hours
        VaultTimeout.Type.ON_APP_RESTART -> BitwardenString.on_restart
        VaultTimeout.Type.NEVER -> BitwardenString.never
        VaultTimeout.Type.CUSTOM -> BitwardenString.custom
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
