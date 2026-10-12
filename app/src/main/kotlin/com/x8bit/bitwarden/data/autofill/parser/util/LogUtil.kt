package com.x8bit.bitwarden.data.autofill.parser.util

import android.app.assist.AssistStructure
import androidx.annotation.VisibleForTesting
import com.bitwarden.annotation.OmitFromCoverage
import com.x8bit.bitwarden.data.autofill.util.website
import timber.log.Timber

/**
 * Indicates whether Autofill verbose logging is enabled or not. This should be `false` unless
 * you are actively debugging.
 */
@VisibleForTesting
internal const val ENABLE_ASSIST_STRUCTURE_LOGS: Boolean = false

/**
 * Logs data about a [AssistStructure.WindowNode] that is about to be parsed.
 */
@OmitFromCoverage
internal fun AssistStructure.WindowNode.logWindowNodeStart() {
    if (!ENABLE_ASSIST_STRUCTURE_LOGS) return
    Timber
        .tag(tag = "BitwardenAutofill")
        .v(
            message = buildString {
                append("START :: windowNode: ")
                append("${this@logWindowNodeStart.title} -- ")
                append("${this@logWindowNodeStart.displayId}")
            },
        )
}

/**
 * Logs data about a [AssistStructure.WindowNode] that has just been parsed.
 */
@OmitFromCoverage
internal fun AssistStructure.WindowNode.logWindowNodeEnd() {
    if (!ENABLE_ASSIST_STRUCTURE_LOGS) return
    Timber
        .tag(tag = "BitwardenAutofill")
        .v(
            message = buildString {
                append("END :: windowNode: ")
                append("${this@logWindowNodeEnd.title} -- ")
                append("${this@logWindowNodeEnd.displayId}")
            },
        )
}

/**
 * Logs data about a [AssistStructure.ViewNode] that is currently being parsed.
 */
@OmitFromCoverage
internal fun AssistStructure.ViewNode.logViewNode(depth: Int) {
    if (!ENABLE_ASSIST_STRUCTURE_LOGS) return
    Timber
        .tag(tag = "BitwardenAutofill")
        .v(
            message = buildString {
                append("${" ".repeat(n = depth)} ViewNode: ")
                append("${this@logViewNode.website} -- ")
                append("${this@logViewNode.className} -- ")
                append("${this@logViewNode.idEntry} -- ")
                append("${this@logViewNode.idPackage} -- ")
                append("${this@logViewNode.text} -- ")
            },
        )
}
