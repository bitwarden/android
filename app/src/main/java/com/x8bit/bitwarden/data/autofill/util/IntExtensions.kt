package com.x8bit.bitwarden.data.autofill.util

import android.app.PendingIntent
import android.os.Build
import android.text.InputType
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Whether this [Int] is a password [InputType].
 */
val Int.isPasswordInputType: Boolean
    get() {
        // The legacy xamarin app mentions that multi-line input types are coming through with
        // TYPE_TEXT_VARIATION_PASSWORD flags. We have no other context to this.
        val isMultiline = this.hasFlag(InputType.TYPE_TEXT_VARIATION_PASSWORD) &&
            this.hasFlag(InputType.TYPE_TEXT_FLAG_MULTI_LINE)

        val isPasswordInputType = this.hasFlag(InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
            this.hasFlag(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD) ||
            this.hasFlag(InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)

        return !isMultiline && isPasswordInputType
    }

/**
 * Whether this [Int] is a username [InputType].
 */
val Int.isUsernameInputType: Boolean
    get() = this.hasFlag(InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS)

/**
 * Whether this [Int] contains [flag].
 */
private fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag

/**
 * Starting from an initial pending intent flag. (ex: [PendingIntent.FLAG_CANCEL_CURRENT])
 */
@OmitFromCoverage
fun Int.toPendingIntentMutabilityFlag(): Int =
    // Mutable flag was added on API level 31
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        this or PendingIntent.FLAG_MUTABLE
    } else {
        this
    }
