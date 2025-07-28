package com.x8bit.bitwarden.data.autofill.accessibility.processor

import android.content.Context
import android.os.PowerManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityAutofillManager
import com.x8bit.bitwarden.data.autofill.accessibility.manager.LauncherPackageNameManager
import com.x8bit.bitwarden.data.autofill.accessibility.model.AccessibilityAction
import com.x8bit.bitwarden.data.autofill.accessibility.parser.AccessibilityParser
import com.x8bit.bitwarden.data.autofill.accessibility.util.fillTextField
import com.x8bit.bitwarden.data.autofill.accessibility.util.isSystemPackage
import com.x8bit.bitwarden.data.autofill.accessibility.util.shouldSkipPackage
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.createAutofillSelectionIntent
import timber.log.Timber

/**
 * The default implementation of the [BitwardenAccessibilityProcessor].
 */
class BitwardenAccessibilityProcessorImpl(
    private val context: Context,
    private val accessibilityParser: AccessibilityParser,
    private val accessibilityAutofillManager: AccessibilityAutofillManager,
    private val launcherPackageNameManager: LauncherPackageNameManager,
    private val powerManager: PowerManager,
    private val toastManager: ToastManager,
) : BitwardenAccessibilityProcessor {
    override fun processAccessibilityEvent(
        event: AccessibilityEvent,
        rootAccessibilityNodeInfoProvider: () -> AccessibilityNodeInfo?,
    ) {
        // Only process the event if the tile was clicked
        val accessibilityAction = accessibilityAutofillManager.accessibilityAction ?: return

        // Prevent clearing the action until we receive a processable event in case unprocessable
        // events are still being received from the device. This can happen on slower devices or if
        // screen transitions are still being performed.
        val eventNode = event
            .getValidNode(rootAccessibilityNodeInfoProvider = rootAccessibilityNodeInfoProvider)
            ?: return

        // Clear the action since we are now acting on a supported node.
        accessibilityAutofillManager.accessibilityAction = null
        when (accessibilityAction) {
            is AccessibilityAction.AttemptFill -> {
                handleAttemptFill(rootNode = eventNode, attemptFill = accessibilityAction)
            }

            AccessibilityAction.AttemptParseUri -> handleAttemptParseUri(rootNode = eventNode)
        }
    }

    private fun AccessibilityEvent.getValidNode(
        rootAccessibilityNodeInfoProvider: () -> AccessibilityNodeInfo?,
    ): AccessibilityNodeInfo? {
        val eventNode = this
            .source
            ?: run {
                Timber.w("Accessibility event source is null, attempting root node")
                // We only call for the root node once, after verifying that the there is an action
                // to be filled. This is because it is a significant performance hit.
                // Additionally, we do not use the root node if it does not have the same package
                // as the triggering event.
                rootAccessibilityNodeInfoProvider()?.takeIf { it.packageName == this.packageName }
            }
            ?: run {
                Timber.w("Root node was also null, skipping this event")
                return null
            }

        // Ignore the event when the phone is inactive.
        if (!powerManager.isInteractive) return null
        // We skip if the system package.
        if (eventNode.isSystemPackage) {
            Timber.d("Skipping autofill for system package ${eventNode.packageName}.")
            return null
        }
        // We skip any package that is explicitly blocked.
        if (eventNode.shouldSkipPackage) {
            Timber.d("Skipping autofill on block-listed package ${eventNode.packageName}.")
            return null
        }
        // We skip any package that is a launcher.
        if (launcherPackageNameManager.launcherPackages.any { it == eventNode.packageName }) {
            Timber.d("Skipping autofill on launcher package ${eventNode.packageName}.")
            return null
        }

        return eventNode
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
                toastManager.show(
                    messageId = BitwardenString.autofill_tile_uri_not_found,
                    duration = Toast.LENGTH_LONG,
                )
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
