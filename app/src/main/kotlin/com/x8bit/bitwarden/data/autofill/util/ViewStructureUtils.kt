package com.x8bit.bitwarden.data.autofill.util

/**
 * The set of raw autofill hints that should be ignored.
 */
val IGNORED_RAW_HINTS: List<String> = listOf(
    "search",
    "find",
    "recipient",
    "edit",
)

/**
 * The supported password autofill hints.
 */
val SUPPORTED_RAW_PASSWORD_HINTS: List<String> = listOf(
    "password",
    "pswd",
)

/**
 * The supported raw autofill hints.
 */
val SUPPORTED_RAW_USERNAME_HINTS: List<String> = listOf(
    "email",
    "phone",
    "username",
)

/**
 * Matches common patterns for cardholder name hints.
 * - `\b(?i)`: Case-insensitive word boundary to ensure we match whole words.
 * - `(?:credit[\\s_-])?`: Optionally matches "credit" followed by a space, underscore, or hyphen.
 * - `(?:cc|card)[\\s_-](?:name|cardholder).*`: Matches "cc" or "card" followed by a space,
 * underscore, or hyphen, then "name" or "cardholder", and finally any characters. This covers
 * variations like "cc name", "card_cardholder", "credit-card-name something else".
 * - `|`: OR operator, allowing for an alternative pattern.
 * - `name[\\s_-]on[\\s_-]card`: Matches "name" followed by a space, underscore, or hyphen, then
 * "on", another space, underscore, or hyphen, and finally "card". This covers phrases like "name on
 * card" or "name_on_card".
 * - `\b`: Word boundary to ensure we match whole words.
 */
val SUPPORTED_RAW_CARDHOLDER_NAME_HINT_PATTERNS: List<Regex> = listOf(
    "\\b(?i)(?:credit[\\s_-])?(?:cc|card)[\\s_-](?:name|cardholder).*\\b".toRegex(),
    "\\b(?i)name[\\s_-]on[\\s_-]card\\b".toRegex(),
)

/**
 * Matches common patterns for card number hints.
 * - `\b(?i)`: Case-insensitive word boundary to ensure we match whole words.
 * - `(?:credit[\\s_-])?`: Optionally matches "credit" followed by a space, underscore, or hyphen.
 * - `(?:cc|card)`: Matches "cc" or "card".
 * - `[\\s_-]number`: Matches "number" preceded by a space, underscore, or hyphen.
 * - `\b`: Word boundary to ensure we match whole words.
 */
val SUPPORTED_RAW_CARD_NUMBER_HINT_PATTERNS: List<Regex> = listOf(
    "\\b(?i)(?:credit[\\s_-])?(?:cc|card)[\\s_-]number\\b".toRegex(),
)

/**
 * Matches common patterns for card expiration month hints.
 * - `\b(?i)`: Case-insensitive word boundary to ensure we match whole words.
 * - `(?:credit[\\s_-])?`: Optionally matches "credit" followed by a space, underscore, or hyphen.
 * - `(?:cc|card)`: Matches "cc" or "card".
 * - `[\\s_-]exp[\\s_-]month`: Matches "exp" followed by a space, underscore, or hyphen, then
 * "month".
 * - `\b`: Word boundary to ensure we match whole words.
 *
 * Examples:
 * - "credit card exp month"
 * - "cc_exp_month"
 * - "card-exp-month"
 */
val SUPPORTED_RAW_CARD_EXP_MONTH_HINT_PATTERNS: List<Regex> = listOf(
    "\\b(?i)(?:credit[\\s_-])?(?:(cc|card)[\\s_-])?(?:exp|expiration|expiry)[\\s_-]month\\b"
        .toRegex(),
)

/**
 * Matches common patterns for card expiration year hints.
 * - `\b(?i)`: Case-insensitive word boundary to ensure we match whole words.
 * - `(?:credit[\\s_-])?`: Optionally matches "credit" followed by a space, underscore, or hyphen.
 * - `(?:cc|card)`: Matches "cc" or "card".
 * - `[\\s_-]exp[\\s_-]year`: Matches "exp" followed by a space, underscore, or hyphen, then "year".
 * - `\b`: Word boundary to ensure we match whole words.
 *
 * Similar to [SUPPORTED_RAW_CARD_EXP_MONTH_HINT_PATTERNS], but for "year" instead of "month".
 * @see SUPPORTED_RAW_CARD_EXP_MONTH_HINT_PATTERNS
 */
val SUPPORTED_RAW_CARD_EXP_YEAR_HINT_PATTERNS: List<Regex> = listOf(
    "\\b(?i)(?:credit[\\s_-])?(?:(cc|card)[\\s_-])?(?:exp|expiration|expiry)[\\s_-]year\\b"
        .toRegex(),
)

/**
 * Matches common patterns for card expiration date hints.
 * - `\b(?i)`: Case-insensitive word boundary to ensure we match whole words.
 * - `(?:credit[\\s_-])?`: Optionally matches "credit" followed by a space, underscore, or hyphen.
 * - `(?:cc|card)`: Matches "cc" or "card".
 * - `[\\s_-]exp[\\s_-]date`: Matches "exp" followed by a space, underscore, or hyphen, then "date".
 * - `.*`: Matches any characters following "date" (e.g., "MM/YY", "month/year").
 * - `\b`: Word boundary to ensure we match whole words.
 *
 * Examples:
 * - "credit card exp date"
 * - "cc_exp_date_mm_yy"
 * - "card-exp-date month/year"
 */
val SUPPORTED_RAW_CARD_EXP_DATE_HINT_PATTERNS: List<Regex> = listOf(
    "\\b(?i)(?:credit[\\s_-])?(?:(cc|card)[\\s_-])?(?:exp|expiration|expiry)[\\s_-]date\\b"
        .toRegex(),
)

/**
 * Matches common patterns for card security code hints.
 * - `\b(?i)`: Case-insensitive word boundary to ensure we match whole words.
 * - `(?:credit[\\s_-])?`: Optionally matches "credit" followed by a space, underscore, or hyphen.
 * - The first pattern `(?:cc|card[\\s_-])(cvc|cvv)\b`:
 *    - `(?:cc|card[\\s_-])`: Matches "cc" or "card" followed by a space, underscore, or hyphen.
 *    - `(cvc|cvv)\b`: Matches "cvc" or "cvv" followed by a word boundary.
 * - The second pattern `(?:cc|card)(?:[\\s_-]verification)?([\\s_-]code)\b`:
 *    - `(?:cc|card)`: Matches "cc" or "card".
 *    - `(?:[\\s_-]verification)?`: Optionally matches "verification" preceded by a space,
 *    underscore, or hyphen.
 * - `([\\s_-]code)\b`: Matches "code" preceded by a space, underscore, or hyphen, and
 * followed by a word boundary.
 *
 * Examples:
 * - "credit card cvc"
 * - "cc_verification_code"
 * - "card-code"
 */
val SUPPORTED_RAW_CARD_SECURITY_CODE_HINT_PATTERNS: List<Regex> = listOf(
    "\\b(?i)(?:credit[\\s_-])?(?:cc|card[\\s_-])?(cvc|cvv)2?\\b".toRegex(),
    "\\b(?i)(?:credit[\\s_-])?(?:cc|card)(?:[\\s_-](?:verification|security))?([\\s_-]code)\\b"
        .toRegex(),
)

/**
 * The supported card brand autofill hints.
 */
val SUPPORTED_RAW_CARD_BRAND_HINTS: List<String> = listOf(
    "cctype",
    "creditcardtype",
    "cardtype",
    "cardbrand",
    "creditcardbrand",
    "ccbrand",
)
