package com.x8bit.bitwarden.data.autofill.util

/**
 * Check whether this string contains any of these [terms].
 *
 * @param terms The terms that should be searched for in this [String].
 * @param ignoreCase Whether the comparison should be sensitive to casing.
 */
fun String.containsAnyTerms(
    terms: List<String>,
    ignoreCase: Boolean = true,
): Boolean =
    terms.any {
        this.contains(
            other = it,
            ignoreCase = ignoreCase,
        )
    }

/**
 * Check whether this string matches any of these [expressions].
 */
fun String.matchesAnyExpressions(
    expressions: List<Regex>,
): Boolean =
    expressions.any {
        this.matches(regex = it)
    }

/**
 * Convert this [String] to lowercase and remove all non-alpha characters.
 */
fun String.toLowerCaseAndStripNonAlpha(): String = this
    .lowercase()
    .replace(Regex("[^a-z]"), "")
