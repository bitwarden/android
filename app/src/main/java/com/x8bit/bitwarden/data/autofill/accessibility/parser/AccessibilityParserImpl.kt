package com.x8bit.bitwarden.data.autofill.accessibility.parser

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.autofill.accessibility.model.FillableFields
import com.x8bit.bitwarden.data.autofill.accessibility.util.getSupportedBrowserOrNull
import com.x8bit.bitwarden.data.autofill.accessibility.util.toUriOrNull
import com.x8bit.bitwarden.data.platform.util.hasHttpProtocol

/**
 * The default implementation for the [AccessibilityParser].
 */
class AccessibilityParserImpl : AccessibilityParser {
    override fun parseForFillableFields(rootNode: AccessibilityNodeInfo): FillableFields {
        // TODO: Parse for username and password fields (PM-11486)
        return FillableFields(
            usernameFields = listOf(),
            passwordFields = listOf(),
        )
    }

    override fun parseForUriOrPackageName(rootNode: AccessibilityNodeInfo): Uri? {
        val packageName = rootNode.packageName.toString()
        val browser = packageName
            .getSupportedBrowserOrNull()
            ?: return "androidapp://$packageName".toUri()
        return browser
            .possibleUrlFieldIds
            .flatMap { viewId ->
                rootNode
                    .findAccessibilityNodeInfosByViewId("$packageName:id/$viewId")
                    .map { accessibilityNodeInfo ->
                        browser
                            .urlExtractor(accessibilityNodeInfo.text.toString())
                            ?.trim()
                            ?.let { rawUrl ->
                                if (rawUrl.contains(other = ".") && !rawUrl.hasHttpProtocol()) {
                                    "https://$rawUrl"
                                } else {
                                    rawUrl
                                }
                            }
                    }
            }
            .firstOrNull()
            ?.toUriOrNull()
    }
}
