package com.x8bit.bitwarden.ui.vault.feature.vault.util

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.AccountSummary

/**
 * Given the [AccountSummary], returns the first two "initials" found when looking at the
 * [AccountSummary.name].
 *
 * Ex:
 * - "First Last" -> "FL"
 * - "First Second Last" -> "FS"
 */
val AccountSummary.initials: String
    get() = this
        .name
        .split(" ")
        .take(2)
        .joinToString(separator = "") { it.first().toString() }

/**
 * Drawable resource to display for the given [AccountSummary].
 */
@get:DrawableRes
val AccountSummary.iconRes: Int
    get() = when (this.status) {
        AccountSummary.Status.ACTIVE -> R.drawable.ic_check_mark
        AccountSummary.Status.LOCKED -> R.drawable.ic_locked
        AccountSummary.Status.UNLOCKED -> R.drawable.ic_unlocked
    }

/**
 * String resource of a supporting text to display (or `null`) for the given [AccountSummary].
 */
@get:StringRes
val AccountSummary.supportingTextResOrNull: Int?
    get() = when (this.status) {
        AccountSummary.Status.ACTIVE -> null
        AccountSummary.Status.LOCKED -> R.string.account_locked
        AccountSummary.Status.UNLOCKED -> R.string.account_unlocked
    }
