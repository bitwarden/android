package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.data.manager.appstate.AppStateManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.AutofillPromptStatus
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
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

    /**
     * No "not checked yet" state is needed here. Autofill is checked when `MainActivity` is
     * created, before anything is drawn. The browser check can land slightly later, but until
     * then the placeholder reports no browsers as available, which maps to
     * [AutofillPromptStatus.On] and shows no prompt.
     */
    override val autofillPromptStatusFlow: StateFlow<AutofillPromptStatus> =
        combine(
            autofillEnabledManager.isAutofillEnabledStateFlow,
            browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatusFlow,
        ) { isAutofillEnabled, browserStatus ->
            toAutofillPromptStatus(
                isAutofillEnabled = isAutofillEnabled,
                browserStatus = browserStatus,
            )
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = toAutofillPromptStatus(
                    isAutofillEnabled = autofillEnabledManager.isAutofillEnabled,
                    browserStatus = browserThirdPartyAutofillEnabledManager
                        .browserThirdPartyAutofillStatus,
                ),
            )

    /**
     * The foreground flow is only here to re-run this block. The snooze expires with the
     * passage of time, which nothing else emits for, so each return to the app re-checks it
     * against the clock.
     */
    override val vaultAutofillPromptFlow: StateFlow<AutofillPromptStatus?> =
        combine(
            autofillPromptStatusFlow,
            settingsDiskSource.isAutofillBannerOptedOutFlow,
            settingsDiskSource.autofillBannerReshowTimeFlow,
            appStateManager.appForegroundStateFlow,
        ) { status, isOptedOut, reshowTime, _ ->
            val isSnoozed = reshowTime?.isAfter(clock.instant()) == true
            if (isOptedOut == true || isSnoozed) {
                null
            } else {
                when (status) {
                    AutofillPromptStatus.AutofillOff,
                    is AutofillPromptStatus.BrowserOff,
                        -> status

                    AutofillPromptStatus.On -> null
                }
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

/**
 * Turning on system autofill comes first, so [AutofillPromptStatus.AutofillOff] takes priority
 * over [AutofillPromptStatus.BrowserOff].
 */
private fun toAutofillPromptStatus(
    isAutofillEnabled: Boolean,
    browserStatus: BrowserThirdPartyAutofillStatus,
): AutofillPromptStatus {
    if (!isAutofillEnabled) return AutofillPromptStatus.AutofillOff
    return browserStatus
        .defaultBrowserPackage
        ?.takeIf { browserStatus.isDefaultBrowserAvailableAndDisabled }
        ?.let { AutofillPromptStatus.BrowserOff(browserPackage = it) }
        ?: AutofillPromptStatus.On
}
