package com.x8bit.bitwarden.data.platform.manager.log

import com.x8bit.bitwarden.data.autofill.accessibility.manager.AccessibilityEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.autofill.browser.util.toBrowserAutoFillSettingsOptions

/**
 * The default implementation for the [SettingsLogManager].
 */
internal class SettingsLogManagerImpl(
    private val settingsRepository: SettingsRepository,
    private val logsManager: LogsManager,
    private val autofillEnabledManager: AutofillEnabledManager,
    private val accessibilityEnabledManager: AccessibilityEnabledManager,
    private val browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
) : SettingsLogManager {
    override val data: String
        get() = StringBuilder()
            .appendLine("=== Bitwarden Settings ===")
            .appendLine("Account Security:")
            .appendLine("  $biometricsString")
            .appendLine("  $pinString")
            .apply { pinMpOnRestartString?.let { appendLine("    $it") } }
            .appendLine("  $authSyncString")
            .appendLine("  $sessionTimeoutString")
            .appendLine("  $sessionTimeoutActionString")
            .appendLine("Autofill:")
            .appendLine("  $autofillServiceString")
            .apply { autofillTypeString?.let { appendLine("    $it") } }
            .apply {
                browserStrings.takeUnless { it.isEmpty() }?.let { list ->
                    appendLine("  Browser integration:")
                    list.forEach { appendLine("    $it") }
                }
            }
            .appendLine("  $accessibilityEnabledString")
            .appendLine("  $autofillAssistString")
            .appendLine("  $copyTotpString")
            .appendLine("  $askToAddItemString")
            .appendLine("  $defaultUriMatchString")
            .appendLine("Appearance:")
            .appendLine("  $languageString")
            .appendLine("  $themeString")
            .appendLine("  $dynamicColorsString")
            .appendLine("  $iconLoadingString")
            .appendLine("Other:")
            .appendLine("  $syncOnRefreshString")
            .appendLine("  $clearClipboardString")
            .appendLine("  $screenCaptureString")
            .appendLine("About:")
            .appendLine("  $crashLogsString")
            .appendLine("  $flightRecorderString")
            .toString()

    private val biometricsString: String
        get() = if (settingsRepository.isUnlockWithBiometricsEnabled) {
            "Unlock with Biometrics: Enabled"
        } else {
            "Unlock with Biometrics: Disabled"
        }

    private val pinString: String
        get() = if (settingsRepository.isUnlockWithPinEnabled) {
            "Unlock with PIN: Enabled"
        } else {
            "Unlock with PIN: Disabled"
        }

    private val pinMpOnRestartString: String?
        get() = if (settingsRepository.isUnlockWithPinEnabled) {
            if (settingsRepository.isPasswordOnRestartRequiredWithPin) {
                "Requires MP on restart: Enabled"
            } else {
                "Requires MP on restart: Disabled"
            }
        } else {
            null
        }

    private val authSyncString: String
        get() = if (settingsRepository.isAuthenticatorSyncEnabled) {
            "Allow authenticator sync: Enabled"
        } else {
            "Allow authenticator sync: Disabled"
        }

    private val sessionTimeoutString: String
        get() = settingsRepository.vaultTimeout.let { "Session timeout: $it" }

    private val sessionTimeoutActionString: String
        get() = settingsRepository.vaultTimeoutAction.let { "Session timeout action: $it" }

    private val autofillServiceString: String
        get() = if (autofillEnabledManager.isAutofillEnabled) {
            "Autofill Service: Enabled"
        } else {
            "Autofill Service: Disabled"
        }

    private val autofillTypeString: String?
        // Check for autofill service
        get() = if (autofillEnabledManager.isAutofillEnabled) {
            if (settingsRepository.isInlineAutofillEnabled) {
                "Display autofill suggestions: Inline"
            } else {
                "Display autofill suggestions: Popup"
            }
        } else {
            null
        }

    private val browserStrings: List<String>
        get() = browserThirdPartyAutofillEnabledManager
            .browserThirdPartyAutofillStatus
            .toBrowserAutoFillSettingsOptions()
            .map { "${it.browserPackage}: ${if (it.isEnabled) "On" else "Off"}" }

    private val accessibilityEnabledString: String
        get() = if (accessibilityEnabledManager.isAccessibilityEnabledStateFlow.value) {
            "Use accessibility: Enabled"
        } else {
            "Use accessibility: Disabled"
        }

    private val autofillAssistString: String
        get() = if (settingsRepository.isFillAssistEnabled) {
            "Autofill assist: Enabled"
        } else {
            "Autofill assist: Disabled"
        }

    private val copyTotpString: String
        get() = if (settingsRepository.isAutoCopyTotpDisabled) {
            "Copy TOTP automatically: Disabled"
        } else {
            "Copy TOTP automatically: Enabled"
        }

    private val askToAddItemString: String
        get() = if (settingsRepository.isAutofillSavePromptDisabled) {
            "Ask to add item: Disabled"
        } else {
            "Ask to add item: Enabled"
        }

    private val defaultUriMatchString: String
        get() = settingsRepository.defaultUriMatchType.let { "Default URI match detection: $it" }

    private val languageString: String
        get() = settingsRepository.appLanguage.let { "Language: $it" }

    private val themeString: String
        get() = settingsRepository.appTheme.let { "Theme: $it" }

    private val dynamicColorsString: String
        get() = if (settingsRepository.isDynamicColorsEnabled) {
            "Use dynamic colors: Enabled"
        } else {
            "Use dynamic colors: Disabled"
        }

    private val iconLoadingString: String
        get() = if (settingsRepository.isIconLoadingDisabled) {
            "Show website icons: Disabled"
        } else {
            "Show website icons: Enabled"
        }

    private val syncOnRefreshString: String
        get() = if (settingsRepository.getPullToRefreshEnabledFlow().value) {
            "Allow sync on refresh: Enabled"
        } else {
            "Allow sync on refresh: Disabled"
        }

    private val clearClipboardString: String
        get() = settingsRepository.clearClipboardFrequency.let { "Clear clipboard: $it" }

    private val screenCaptureString: String
        get() = if (settingsRepository.isScreenCaptureAllowed) {
            "Allow screen capture: Enabled"
        } else {
            "Allow screen capture: Disabled"
        }

    private val crashLogsString: String
        get() = if (logsManager.isEnabled) {
            "Submit crash logs: Enabled"
        } else {
            "Submit crash logs: Disabled"
        }

    private val flightRecorderString: String
        get() = if (settingsRepository.flightRecorderData.hasActiveFlightRecorderData) {
            "Flight recorder: On"
        } else {
            "Flight recorder: Off"
        }
}
