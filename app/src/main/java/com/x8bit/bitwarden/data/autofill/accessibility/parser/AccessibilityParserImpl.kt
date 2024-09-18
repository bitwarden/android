package com.x8bit.bitwarden.data.autofill.accessibility.parser

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityNodeInfoManager
import com.x8bit.bitwarden.data.autofill.accessibility.model.FillableFields
import com.x8bit.bitwarden.data.autofill.accessibility.util.getSupportedBrowserOrNull
import com.x8bit.bitwarden.data.autofill.accessibility.util.isEditText
import com.x8bit.bitwarden.data.autofill.accessibility.util.toUriOrNull
import com.x8bit.bitwarden.data.platform.util.hasHttpProtocol

/**
 * The default implementation for the [AccessibilityParser].
 */
class AccessibilityParserImpl(
    private val accessibilityNodeInfoManager: AccessibilityNodeInfoManager,
) : AccessibilityParser {
    override fun parseForFillableFields(
        rootNode: AccessibilityNodeInfo,
        uri: Uri,
    ): FillableFields {
        val nodes = accessibilityNodeInfoManager
            .findAccessibilityNodeInfoList(rootNode = rootNode) {
                it.isEditText || it.isPassword
            }
        val passwordNodes = nodes.filter { it.isPassword }
        return FillableFields(
            usernameField = accessibilityNodeInfoManager.findUsernameAccessibilityNodeInfo(
                uri = uri,
                allNodes = nodes,
                passwordNodes = passwordNodes,
            ),
            passwordFields = passwordNodes,
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
