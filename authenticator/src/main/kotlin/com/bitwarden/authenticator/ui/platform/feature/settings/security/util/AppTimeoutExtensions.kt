package com.bitwarden.authenticator.ui.platform.feature.settings.security.util

import com.bitwarden.authenticator.data.platform.manager.lock.model.AppTimeout
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Provides a human-readable display label for the given [AppTimeout.Type].
 */
val AppTimeout.Type.displayLabel: Text
    get() = when (this) {
        AppTimeout.Type.IMMEDIATELY -> BitwardenString.immediately
        AppTimeout.Type.ONE_MINUTE -> BitwardenString.one_minute
        AppTimeout.Type.FIVE_MINUTES -> BitwardenString.five_minutes
        AppTimeout.Type.FIFTEEN_MINUTES -> BitwardenString.fifteen_minutes
        AppTimeout.Type.THIRTY_MINUTES -> BitwardenString.thirty_minutes
        AppTimeout.Type.ONE_HOUR -> BitwardenString.one_hour
        AppTimeout.Type.FOUR_HOURS -> BitwardenString.four_hours
        AppTimeout.Type.ON_APP_RESTART -> BitwardenString.on_restart
        AppTimeout.Type.NEVER -> BitwardenString.never
    }
        .asText()
