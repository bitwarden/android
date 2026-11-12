package com.x8bit.bitwarden.data.autofill.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.model.AutofillPromptStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Default implementation of [AutofillPromptManager].
 */
class AutofillPromptManagerImpl(
    autofillEnabledManager: AutofillEnabledManager,
    browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
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
}
