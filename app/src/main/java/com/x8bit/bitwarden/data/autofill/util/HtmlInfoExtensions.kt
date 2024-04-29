package com.x8bit.bitwarden.data.autofill.util

import android.view.ViewStructure.HtmlInfo
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Whether this [HtmlInfo] represents a password field.
 *
 * This function is untestable as [HtmlInfo] contains [android.util.Pair] which requires
 * instrumentation testing.
 */
@OmitFromCoverage
fun HtmlInfo?.isPasswordField(): Boolean =
    this
        ?.let { htmlInfo ->
            if (htmlInfo.isInputField) {
                htmlInfo
                    .attributes
                    ?.any {
                        it.first == "type" && it.second == "password"
                    }
            } else {
                false
            }
        }
        ?: false

/**
 * Whether this [HtmlInfo] represents a username field.
 *
 * This function is untestable as [HtmlInfo] contains [android.util.Pair] which requires
 * instrumentation testing.
 */
@OmitFromCoverage
fun HtmlInfo?.isUsernameField(): Boolean =
    this
        ?.let { htmlInfo ->
            if (htmlInfo.isInputField) {
                htmlInfo
                    .attributes
                    ?.any {
                        it.first == "type" && it.second == "email"
                    }
            } else {
                false
            }
        }
        ?: false

/**
 * Whether this [HtmlInfo] represents an input field.
 */
val HtmlInfo?.isInputField: Boolean get() = this?.tag == "input"
