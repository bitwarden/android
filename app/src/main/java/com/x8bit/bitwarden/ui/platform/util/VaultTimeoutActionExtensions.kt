package com.x8bit.bitwarden.ui.platform.util

import com.bitwarden.core.ui.util.Text
import com.bitwarden.core.ui.util.asText
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.VaultTimeoutAction

/**
 * Provides a human-readable display label for the given [VaultTimeoutAction].
 */
val VaultTimeoutAction.displayLabel: Text
    get() = when (this) {
        VaultTimeoutAction.LOCK -> R.string.lock
        VaultTimeoutAction.LOGOUT -> R.string.log_out
    }
        .asText()
