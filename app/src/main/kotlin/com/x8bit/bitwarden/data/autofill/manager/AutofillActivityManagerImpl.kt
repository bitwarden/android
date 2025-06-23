package com.x8bit.bitwarden.data.autofill.manager

import android.view.autofill.AutofillManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Primary implementation of [AutofillActivityManager].
 */
class AutofillActivityManagerImpl(
    private val autofillManager: AutofillManager,
    private val browserThirdPartyAutofillManager: BrowserThirdPartyAutofillManager,
    autofillEnabledManager: AutofillEnabledManager,
    appStateManager: AppStateManager,
    lifecycleScope: LifecycleCoroutineScope,
    browserThirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
) : AutofillActivityManager {
    private val isAutofillEnabledAndSupported: Boolean
        get() = autofillManager.isEnabled &&
            autofillManager.hasEnabledAutofillServices() &&
            autofillManager.isAutofillSupported

    private val browserAutofillStatus: BrowserThirdPartyAutofillStatus
        get() = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = browserThirdPartyAutofillManager.stableBraveAutofillStatus,
            chromeStableStatusData = browserThirdPartyAutofillManager.stableChromeAutofillStatus,
            chromeBetaChannelStatusData = browserThirdPartyAutofillManager.betaChromeAutofillStatus,
        )

    init {
        appStateManager
            .appForegroundStateFlow
            .onEach {
                autofillEnabledManager.isAutofillEnabled = isAutofillEnabledAndSupported
                browserThirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatus =
                    browserAutofillStatus
            }
            .launchIn(lifecycleScope)
    }
}
