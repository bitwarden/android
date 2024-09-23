package com.x8bit.bitwarden.data.autofill.accessibility.util

import android.view.accessibility.AccessibilityNodeInfo
import android.widget.EditText
import androidx.core.os.bundleOf
import com.x8bit.bitwarden.data.autofill.accessibility.model.KnownUsernameField
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

private const val PACKAGE_NAME_BITWARDEN_PREFIX: String = "com.x8bit.bitwarden"
private const val PACKAGE_NAME_SYSTEM_UI: String = "com.android.systemui"
private const val PACKAGE_NAME_LAUNCHER_PARTIAL: String = "launcher"
private val PACKAGE_NAME_BLOCK_LIST: List<String> = listOf(
    "com.google.android.googlequicksearchbox",
    "com.google.android.apps.nexuslauncher",
    "com.google.android.launcher",
    "com.computer.desktop.ui.launcher",
    "com.launcher.notelauncher",
    "com.anddoes.launcher",
    "com.actionlauncher.playstore",
    "ch.deletescape.lawnchair.plah",
    "com.microsoft.launcher",
    "com.teslacoilsw.launcher",
    "com.teslacoilsw.launcher.prime",
    "is.shortcut",
    "me.craftsapp.nlauncher",
    "com.ss.squarehome2",
    "com.treydev.pns",
)

/**
 * Returns true if the event is for an unsupported package.
 */
val AccessibilityNodeInfo.shouldSkipPackage: Boolean
    get() {
        val packageName = this.packageName.takeUnless { it.isNullOrBlank() } ?: return true
        if (packageName.startsWith(prefix = PACKAGE_NAME_BITWARDEN_PREFIX)) return true
        if (packageName.contains(other = PACKAGE_NAME_LAUNCHER_PARTIAL, ignoreCase = true)) {
            return true
        }
        return PACKAGE_NAME_BLOCK_LIST.contains(packageName)
    }

/**
 * Returns true if the event is from the system UI package.
 */
val AccessibilityNodeInfo.isSystemPackage: Boolean
    get() = this.packageName == PACKAGE_NAME_SYSTEM_UI

/**
 * Fills the [AccessibilityNodeInfo] text field with the [value] provided.
 */
@OmitFromCoverage
fun AccessibilityNodeInfo.fillTextField(value: String?) {
    performAction(
        AccessibilityNodeInfo.ACTION_SET_TEXT,
        bundleOf(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE to value),
    )
}

/**
 * Determines if the [AccessibilityNodeInfo] is an instance of an EditText.
 */
val AccessibilityNodeInfo.isEditText: Boolean
    get() = className
        ?.let {
            try {
                Class.forName(it.toString())
            } catch (e: ClassNotFoundException) {
                null
            }
        }
        ?.let { EditText::class.java.isAssignableFrom(it) }
        ?: (className?.contains(other = "EditText") == true)

/**
 * Determines if the [AccessibilityNodeInfo] is a username field.
 */
fun AccessibilityNodeInfo.isUsername(
    knownUsernameField: KnownUsernameField,
    uriPath: String,
): Boolean {
    knownUsernameField.accessOptions.map { options ->
        if (options.matchingStrategy.matches(uriPath, options.matchValue)) {
            options
                .usernameViewIds
                .firstOrNull { viewId -> viewId == this@isUsername.viewIdResourceName }
                ?.let { return true }
        }
    }
    return false
}
