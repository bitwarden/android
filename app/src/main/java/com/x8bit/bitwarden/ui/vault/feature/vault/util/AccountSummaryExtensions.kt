package com.x8bit.bitwarden.ui.vault.feature.vault.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary
import java.util.Locale

/**
 * Given the [AccountSummary], returns the first two "initials" found when looking at the
 * [AccountSummary.name].
 *
 * Ex:
 * - "First Last" -> "FL"
 * - "First Second Last" -> "FS"
 * - "First" -> "FI"
 * - name is `null`, email is "test@bitwarden.com" -> "TE"
 */
val AccountSummary.initials: String
    get() {
        val names = this.name.orEmpty().split(" ").filter { it.isNotBlank() }
        return if (names.size >= 2) {
            names
                .take(2)
                .joinToString(separator = "") { it.first().toString() }
        } else {
            (this.name ?: this.email).take(2)
        }
            .uppercase(Locale.getDefault())
    }

/**
 * Drawable resource to display for the given [AccountSummary].
 */
@get:DrawableRes
val AccountSummary.iconRes: Int
    get() = when (this.status) {
        AccountSummary.Status.ACTIVE -> R.drawable.ic_check_mark
        AccountSummary.Status.LOCKED -> R.drawable.ic_locked
        AccountSummary.Status.LOGGED_OUT -> R.drawable.ic_locked
        AccountSummary.Status.UNLOCKED -> R.drawable.ic_unlocked
    }

/**
 * Test tag to be used for the for the given [AccountSummary.iconRes].
 */
val AccountSummary.iconTestTag: String
    get() = when (this.status) {
        AccountSummary.Status.ACTIVE -> "ActiveVaultIcon"
        AccountSummary.Status.LOCKED,
        AccountSummary.Status.LOGGED_OUT,
        AccountSummary.Status.UNLOCKED,
            -> "InactiveVaultIcon"
    }

/**
 * String resource of a supporting text to display (or `null`) for the given [AccountSummary].
 */
@get:StringRes
val AccountSummary.supportingTextResOrNull: Int?
    get() = when (this.status) {
        AccountSummary.Status.ACTIVE -> null
        AccountSummary.Status.LOCKED -> R.string.account_locked
        AccountSummary.Status.LOGGED_OUT -> R.string.account_logged_out
        AccountSummary.Status.UNLOCKED -> R.string.account_unlocked
    }
