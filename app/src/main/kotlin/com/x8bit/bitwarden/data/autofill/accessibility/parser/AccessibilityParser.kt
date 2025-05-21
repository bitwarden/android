package com.x8bit.bitwarden.data.autofill.accessibility.parser

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import com.x8bit.bitwarden.data.autofill.accessibility.model.FillableFields

/**
 * A tool for parsing accessibility data from the OS into domain models.
 */
interface AccessibilityParser {
    /**
     * Parses the fillable fields from [rootNode].
     */
    fun parseForFillableFields(rootNode: AccessibilityNodeInfo, uri: Uri): FillableFields

    /**
     * Parses the [Uri] from [rootNode] and returns a url, package name.
     */
    fun parseForUriOrPackageName(rootNode: AccessibilityNodeInfo): Uri?
}
