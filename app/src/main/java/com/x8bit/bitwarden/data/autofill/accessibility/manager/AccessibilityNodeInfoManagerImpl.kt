package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo
import com.x8bit.bitwarden.data.autofill.accessibility.util.getKnownUsernameFieldNull
import com.x8bit.bitwarden.data.autofill.accessibility.util.isUsername
import timber.log.Timber

private const val MAX_NODE_COUNT: Int = 100

/**
 * The default implementation for the [AccessibilityNodeInfoManager].
 */
class AccessibilityNodeInfoManagerImpl : AccessibilityNodeInfoManager {
    override fun findAccessibilityNodeInfoList(
        rootNode: AccessibilityNodeInfo,
        maxRecursionDepth: Int,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): List<AccessibilityNodeInfo> =
        findAccessibilityNodeInfoList(
            rootNode = rootNode,
            maxRecursionDepth = maxRecursionDepth,
            currentRecursionDepth = 0,
            predicate = predicate,
        )

    override fun findUsernameAccessibilityNodeInfo(
        uri: Uri,
        allNodes: List<AccessibilityNodeInfo>,
        passwordNodes: List<AccessibilityNodeInfo>,
    ): AccessibilityNodeInfo? {
        val uriPath = uri
            .path
            ?: return findMissingUsernameNodeInfo(
                allNodes = allNodes,
                passwordNodes = passwordNodes,
            )
        return uri
            .authority
            ?.removePrefix(prefix = "www.")
            ?.getKnownUsernameFieldNull()
            ?.let { usernameField ->
                allNodes.firstOrNull { node ->
                    node.isUsername(
                        uriPath = uriPath,
                        knownUsernameField = usernameField,
                    )
                }
            }
            ?: findMissingUsernameNodeInfo(allNodes = allNodes, passwordNodes = passwordNodes)
    }

    private fun findAccessibilityNodeInfoList(
        rootNode: AccessibilityNodeInfo,
        maxRecursionDepth: Int,
        currentRecursionDepth: Int,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): List<AccessibilityNodeInfo> {
        if (predicate(rootNode)) return listOf(rootNode)
        if (currentRecursionDepth >= maxRecursionDepth) return emptyList()
        val childNodeCount = rootNode.childCount - 1
        if (childNodeCount > MAX_NODE_COUNT) log(message = "Too many child iterations.")
        return (0..childNodeCount.coerceAtMost(maximumValue = MAX_NODE_COUNT)).flatMap {
            val childNode = rootNode.getChild(it) ?: return@flatMap emptyList()
            if (childNode.hashCode() == this.hashCode()) {
                log(message = "Child node is the same as parent for some reason.")
                emptyList()
            } else {
                findAccessibilityNodeInfoList(
                    rootNode = childNode,
                    maxRecursionDepth = maxRecursionDepth,
                    currentRecursionDepth = currentRecursionDepth + 1,
                    predicate = predicate,
                )
            }
        }
    }

    /**
     * Attempts to find a username [AccessibilityNodeInfo] if there isn't one already. This
     * functions by finding the first known password node and taking the node directly above it.
     */
    private fun findMissingUsernameNodeInfo(
        allNodes: List<AccessibilityNodeInfo>,
        passwordNodes: List<AccessibilityNodeInfo>,
    ): AccessibilityNodeInfo? =
        passwordNodes
            .firstOrNull()
            ?.let { allNodes.getOrNull(index = allNodes.indexOf(element = it) - 1) }

    private fun log(message: String) {
        Timber.i(message)
    }
}
