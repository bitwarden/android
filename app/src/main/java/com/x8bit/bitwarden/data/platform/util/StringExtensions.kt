package com.x8bit.bitwarden.data.platform.util

/**
 * Returns the original [String] only if:
 *
 * - it is non-null
 * - it is not blank (where blank refers to empty strings of those containing only white space)
 *
 * Otherwise `null` is returned.
 */
fun String?.orNullIfBlank(): String? =
    this?.takeUnless { it.isBlank() }
