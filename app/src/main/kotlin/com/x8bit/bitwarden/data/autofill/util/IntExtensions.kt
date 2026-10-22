package com.x8bit.bitwarden.data.autofill.util

import android.text.InputType

/**
 * Whether this [Int] is a password [InputType].
 *
 * The variation bits of an [InputType] form an enumeration, not a set of flags, so the class and
 * variation are compared after masking, as the framework's `TextView.isPasswordInputType` does.
 * `TYPE_TEXT_VARIATION_VISIBLE_PASSWORD` is intentionally excluded (as the framework also does):
 * apps commonly use it for codes, recovery phrases, and other text that should not get IME
 * suggestions. Unlike the framework, numeric passwords (`TYPE_NUMBER_VARIATION_PASSWORD`) are not
 * matched.
 */
val Int.isPasswordInputType: Boolean
    get() {
        // Multi-line fields are never treated as passwords; the legacy xamarin app saw multi-line
        // input types coming through with TYPE_TEXT_VARIATION_PASSWORD.
        if (this.hasFlag(InputType.TYPE_TEXT_FLAG_MULTI_LINE)) return false

        val classAndVariation =
            this and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_VARIATION)
        return classAndVariation ==
            (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD) ||
            classAndVariation ==
            (InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD)
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
