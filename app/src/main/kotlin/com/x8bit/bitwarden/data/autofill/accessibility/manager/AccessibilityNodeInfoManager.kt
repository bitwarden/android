package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.net.Uri
import android.view.accessibility.AccessibilityNodeInfo

/**
 * The default maximum recursive depth that the
 * [AccessibilityNodeInfoManager.findAccessibilityNodeInfoList] will go.
 */
const val DEFAULT_MAX_RECURSION_DEPTH: Int = 100

/**
 * A manager for finding fields that match particular characteristics.
 */
interface AccessibilityNodeInfoManager {
    /**
     * A helper function for retrieving the appropriate nodes based on the given [predicate].
     *
     * This function is recursive but will stop recurring if the depth it reaches is greater than
     * the [maxRecursionDepth].
     */
    fun findAccessibilityNodeInfoList(
        rootNode: AccessibilityNodeInfo,
        maxRecursionDepth: Int = DEFAULT_MAX_RECURSION_DEPTH,
        predicate: (AccessibilityNodeInfo) -> Boolean,
    ): List<AccessibilityNodeInfo>

    /**
     * Determines which [AccessibilityNodeInfo] is a username field.
     */
    fun findUsernameAccessibilityNodeInfo(
        uri: Uri,
        allNodes: List<AccessibilityNodeInfo>,
        passwordNodes: List<AccessibilityNodeInfo>,
    ): AccessibilityNodeInfo?
}
