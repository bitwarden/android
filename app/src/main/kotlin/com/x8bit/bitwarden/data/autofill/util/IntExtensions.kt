package com.x8bit.bitwarden.data.autofill.util

import android.text.InputType

/**
 * Whether this [Int] is a password [InputType].
 */
val Int.isPasswordInputType: Boolean
    get() {
        // The variation is a multi-bit field, not a set of independent flags. Testing it with a
        // bit mask matches other variations that share the password bit (for example
        // TYPE_TEXT_VARIATION_WEB_EDIT_TEXT, which is what WebView text inputs report), so the
        // variation must be compared for equality.
        val variation = this and InputType.TYPE_MASK_VARIATION
        val isPasswordVariation = variation == InputType.TYPE_TEXT_VARIATION_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD ||
            variation == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD

        // The text variations are only meaningful with TYPE_CLASS_TEXT. Android itself only
        // masks input and treats a field as a password when the class is text. Some apps set
        // only the variation bits on a user ID field to turn off suggestions (for example
        // `TYPE_TEXT_FLAG_NO_SUGGESTIONS | TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`, which has no
        // class), and those fields are not passwords.
        val isTextClass = (this and InputType.TYPE_MASK_CLASS) == InputType.TYPE_CLASS_TEXT

        // The legacy xamarin app mentions that multi-line input types are coming through with
        // TYPE_TEXT_VARIATION_PASSWORD flags. We have no other context to this.
        val isMultiline = this.hasFlag(InputType.TYPE_TEXT_FLAG_MULTI_LINE)

        return isTextClass && isPasswordVariation && !isMultiline
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
