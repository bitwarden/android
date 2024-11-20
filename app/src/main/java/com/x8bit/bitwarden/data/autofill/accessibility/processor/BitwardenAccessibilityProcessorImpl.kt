package com.x8bit.bitwarden.data.autofill.accessibility.processor

import android.content.Context
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManager
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParser
import com.x8bit.bitwarden.data.autofill.accessibility.util.fillTextField
import com.x8bit.bitwarden.data.autofill.accessibility.util.isSystemPackage
import com.x8bit.bitwarden.data.autofill.accessibility.util.shouldSkipPackage
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.createAutofillSelectionIntent

/**
 * The default implementation of the [BitwardenAccessibilityProcessor].
 */
class BitwardenAccessibilityProcessorImpl(
    private val context: Context,
    private val accessibilityParser: AccessibilityParser,
    private val accessibilityAutofillManager: AccessibilityAutofillManager,
    private val launcherPackageNameManager: LauncherPackageNameManager,
    private val powerManager: PowerManager,
) : BitwardenAccessibilityProcessor {
    override fun processAccessibilityEvent(
        event: AccessibilityEvent,
        rootAccessibilityNodeInfoProvider: () -> AccessibilityNodeInfo?,
    ) {
        val eventNode = event.source ?: return
        // Ignore the event when the phone is inactive
        if (!powerManager.isInteractive) return
        // We skip if the system package
        if (eventNode.isSystemPackage) return
        // We skip any package that is unsupported
        if (eventNode.shouldSkipPackage) return
        // We skip any package that is a launcher
        if (launcherPackageNameManager.launcherPackages.any { it == eventNode.packageName }) {
            return
        }

        // Only process the event if the tile was clicked
        val accessibilityAction = accessibilityAutofillManager.accessibilityAction ?: return
        // We only call for the root node once after all other checks
        // have passed because it is significant performance hit
        if (rootAccessibilityNodeInfoProvider()?.packageName != event.packageName) return

        // Clear the action since we are now acting on it
        accessibilityAutofillManager.accessibilityAction = null

        when (accessibilityAction) {
            is AccessibilityAction.AttemptFill -> {
                handleAttemptFill(rootNode = eventNode, attemptFill = accessibilityAction)
            }

            AccessibilityAction.AttemptParseUri -> handleAttemptParseUri(rootNode = eventNode)
        }
    }

    private fun handleAttemptParseUri(rootNode: AccessibilityNodeInfo) {
        accessibilityParser
            .parseForUriOrPackageName(rootNode = rootNode)
            ?.takeIf {
                accessibilityParser
                    .parseForFillableFields(rootNode = rootNode, uri = it)
                    .hasFields
            }
            ?.let { uri ->
                context.startActivity(
                    createAutofillSelectionIntent(
                        context = context,
                        framework = AutofillSelectionData.Framework.ACCESSIBILITY,
                        type = AutofillSelectionData.Type.LOGIN,
                        uri = uri.toString(),
                    ),
                )
            }
            ?: run {
                Toast
                    .makeText(
                        context,
                        R.string.autofill_tile_uri_not_found,
                        Toast.LENGTH_LONG,
                    )
                    .show()
            }
    }

    private fun handleAttemptFill(
        rootNode: AccessibilityNodeInfo,
        attemptFill: AccessibilityAction.AttemptFill,
    ) {
        val loginView = attemptFill.cipherView.login ?: return
        val fields = accessibilityParser.parseForFillableFields(
            rootNode = rootNode,
            uri = attemptFill.uri,
        )
        fields.usernameField?.fillTextField(value = loginView.username)
        fields.passwordFields.forEach { passwordField ->
            passwordField.fillTextField(value = loginView.password)
        }
    }
}
