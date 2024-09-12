package com.x8bit.bitwarden.data.autofill.accessibility.processor

import android.content.Context
import android.os.PowerManager
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManager
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParser
import com.x8bit.bitwarden.data.autofill.accessibility.util.fillTextField
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
    override fun processAccessibilityEvent(rootAccessibilityNodeInfo: AccessibilityNodeInfo?) {
        val rootNode = rootAccessibilityNodeInfo ?: return
        // Ignore the event when the phone is inactive
        if (!powerManager.isInteractive) return
        // We skip if the package is not supported
        if (rootNode.shouldSkipPackage) return
        // We skip any package that is a launcher
        if (launcherPackageNameManager.launcherPackages.any { it == rootNode.packageName }) return

        // Only process the event if the tile was clicked
        val accessibilityAction = accessibilityAutofillManager.accessibilityAction ?: return
        accessibilityAutofillManager.accessibilityAction = null

        when (accessibilityAction) {
            is AccessibilityAction.AttemptFill -> {
                handleAttemptFill(rootNode = rootNode, attemptFill = accessibilityAction)
            }

            AccessibilityAction.AttemptParseUri -> handleAttemptParseUri(rootNode = rootNode)
        }
    }

    private fun handleAttemptParseUri(rootNode: AccessibilityNodeInfo) {
        accessibilityParser
            .parseForUriOrPackageName(rootNode = rootNode)
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
        val fields = accessibilityParser.parseForFillableFields(rootNode = rootNode)
        fields.usernameFields.forEach { usernameField ->
            usernameField.fillTextField(value = loginView.username)
        }
        fields.passwordFields.forEach { passwordField ->
            passwordField.fillTextField(value = loginView.password)
        }
    }
}
