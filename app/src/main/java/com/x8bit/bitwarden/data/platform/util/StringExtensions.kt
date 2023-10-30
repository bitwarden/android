package com.x8bit.bitwarden.data.platform.util

import androidx.compose.ui.text.AnnotatedString

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

/**
 * Returns the [String] as an [AnnotatedString].
 */
fun String.toAnnotatedString(): AnnotatedString = AnnotatedString(text = this)
