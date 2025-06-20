package com.x8bit.bitwarden.data.platform.util

import java.util.regex.PatternSyntaxException

/**
 * Attempts to create a [Regex] and returns `null` if the [pattern] is not valid.
 */
fun regexOrNull(
    pattern: String,
    option: RegexOption,
): Regex? =
    try {
        Regex(pattern, option)
    } catch (e: PatternSyntaxException) {
        null
    }
