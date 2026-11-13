package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.manager.appstate.AppStateManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.AutofillPromptStatus
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration

/**
 * How many hours the vault prompt stays hidden after the user dismisses it.
 */
private const val VAULT_PROMPT_SNOOZE_HOURS: Long = 24L

/**
 * Default implementation of [AutofillPromptManager].
 */
class AutofillPromptManagerImpl(
    autofillEnabledManager: AutofillEnabledManager,
    browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
    appStateManager: AppStateManager,
    private val settingsDiskSource: SettingsDiskSource,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : AutofillPromptManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    // Both values start as placeholders that read as "off", so stay Unknown until each has been
    // set at least once. This does not depend on the order the two values are set in.
    override val autofillPromptStatusFlow: StateFlow<AutofillPromptStatus> =
        combine(
            autofillEnabledManager.isAutofillEnabledCheckedFlow,
            browserThirdPartyAutofillEnabledManager.isBrowserThirdPartyAutofillStatusCheckedFlow,
            autofillEnabledManager.isAutofillEnabledStateFlow,
            browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatusFlow,
        ) { isAutofillChecked, isBrowserChecked, isAutofillEnabled, browserStatus ->
            when {
                !isAutofillChecked || !isBrowserChecked -> AutofillPromptStatus.Unknown
                !isAutofillEnabled -> AutofillPromptStatus.AutofillOff
                else -> {
                    browserStatus
                        .defaultBrowserPackage
                        ?.takeIf { browserStatus.isDefaultBrowserAvailableAndDisabled }
                        ?.let { AutofillPromptStatus.BrowserOff(browserPackage = it) }
                        ?: AutofillPromptStatus.On
                }
            }
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = AutofillPromptStatus.Unknown,
            )

    // The foreground flow is only here to re-run this block. The snooze expires with the
    // passage of time, which nothing else emits for, so each return to the app re-checks it
    // against the clock.
    override val vaultAutofillPromptFlow: StateFlow<AutofillPromptStatus?> =
        combine(
            autofillPromptStatusFlow,
            settingsDiskSource.isAutofillBannerOptedOutFlow,
            settingsDiskSource.autofillBannerReshowTimeFlow,
            appStateManager.appForegroundStateFlow,
        ) { status, isOptedOut, reshowTime, _ ->
            val isSnoozed = reshowTime?.isAfter(clock.instant()) == true
            when {
                isOptedOut == true || isSnoozed -> null
                status is AutofillPromptStatus.AutofillOff -> status
                status is AutofillPromptStatus.BrowserOff -> status
                else -> null
            }
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = null,
            )

    override fun snoozeVaultPrompt() {
        settingsDiskSource.autofillBannerReshowTime =
            clock.instant().plus(Duration.ofHours(VAULT_PROMPT_SNOOZE_HOURS))
    }

    override fun optOutOfVaultPrompt() {
        settingsDiskSource.isAutofillBannerOptedOut = true
    }
}
