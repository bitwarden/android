@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.data.autofill.util

import android.view.ViewStructure.HtmlInfo

/**
 * Whether this [HtmlInfo] represents a password field.
 */
fun HtmlInfo?.isPasswordField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_PASSWORD_HINTS)

/**
 * Whether this [HtmlInfo] represents a username field.
 */
fun HtmlInfo?.isUsernameField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_USERNAME_HINTS)

/**
 * Whether this [HtmlInfo] represents a cardholder name field.
 */
fun HtmlInfo?.isCardholderNameField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARDHOLDER_NAME_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card number field.
 */
fun HtmlInfo?.isCardNumberField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_NUMBER_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card expiration month field.
 */
fun HtmlInfo?.isCardExpirationMonthField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_EXP_MONTH_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card expiration year field.
 */
fun HtmlInfo?.isCardExpirationYearField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_EXP_YEAR_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card expiration date field.
 */
fun HtmlInfo?.isCardExpirationDateField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_EXP_DATE_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card security code field.
 */
fun HtmlInfo?.isCardSecurityCodeField(): Boolean = isInputField &&
    hints().containsAnyPatterns(SUPPORTED_RAW_CARD_SECURITY_CODE_HINT_PATTERNS)

/**
 * Whether this [HtmlInfo] represents a card brand field.
 */
fun HtmlInfo?.isCardBrandField(): Boolean = isInputField &&
    hints().containsAnyTerms(SUPPORTED_RAW_CARD_BRAND_HINTS)

/**
 * Attributes that can be used as hints to determine the type of data the associated node expects.
 *
 * This function is untestable as [HtmlInfo] contains [android.util.Pair] which requires
 * instrumentation testing.
 *
 * @see IGNORED_RAW_HINTS
 * @see SUPPORTED_HTML_ATTRIBUTE_HINTS
 */
fun HtmlInfo?.hints(): List<String> = this
    ?.let { htmlInfo ->
        htmlInfo
            .attributes
            // Filter out attributes with null values or values that match ignored raw hints
            ?.filter { attribute ->
                attribute.second != null &&
                    !attribute.second.containsAnyTerms(IGNORED_RAW_HINTS)
            }
            // Filter attributes that match supported HTML attribute hints
            ?.filter { attribute ->
                attribute.first.containsAnyTerms(
                    terms = SUPPORTED_HTML_ATTRIBUTE_HINTS,
                    ignoreCase = true,
                )
            }
            .orEmpty()
            .mapNotNull { it.second }
    }
    .orEmpty()

/**
 * Whether this [HtmlInfo] represents an input field.
 */
val HtmlInfo?.isInputField: Boolean get() = this?.tag == "input"

/**
 * Checks if the list of strings contains any of the specified patterns.
 */
private fun List<String>.containsAnyPatterns(patterns: List<Regex>): Boolean = this
    .any { string -> patterns.any { pattern -> string.matches(pattern) } }

/**
 * Checks if the list of strings contains any of the specified terms.
 */
private fun List<String>.containsAnyTerms(terms: List<String>): Boolean =
    this.any { string ->
        string
            .toLowerCaseAndStripNonAlpha()
            .containsAnyTerms(terms)
    }

/**
 * The supported attribute keys whose value can represent an autofill hint.
 */
private val SUPPORTED_HTML_ATTRIBUTE_HINTS: List<String> = listOf(
    "name",
    "label",
    "type",
    "hint",
    "autofill",
)
