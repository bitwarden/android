package com.x8bit.bitwarden.data.autofill.manager

import android.view.autofill.AutofillManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.x8bit.bitwarden.data.autofill.manager.chrome.ChromeThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.chrome.ChromeThirdPartyAutofillManager
import com.x8bit.bitwarden.data.autofill.model.chrome.ChromeThirdPartyAutofillStatus
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Primary implementation of [AutofillActivityManager].
 */
class AutofillActivityManagerImpl(
    private val autofillManager: AutofillManager,
    private val chromeThirdPartyAutofillManager: ChromeThirdPartyAutofillManager,
    autofillEnabledManager: AutofillEnabledManager,
    appStateManager: AppStateManager,
    lifecycleScope: LifecycleCoroutineScope,
    chromeThirdPartyAutofillEnabledManager: ChromeThirdPartyAutofillEnabledManager,
) : AutofillActivityManager {
    private val isAutofillEnabledAndSupported: Boolean
        get() = autofillManager.isEnabled &&
            autofillManager.hasEnabledAutofillServices() &&
            autofillManager.isAutofillSupported

    private val chromeAutofillStatus: ChromeThirdPartyAutofillStatus
        get() = ChromeThirdPartyAutofillStatus(
            stableStatusData = chromeThirdPartyAutofillManager.stableChromeAutofillStatus,
            betaChannelStatusData = chromeThirdPartyAutofillManager.betaChromeAutofillStatus,
        )

    init {
        appStateManager
            .appForegroundStateFlow
            .onEach {
                autofillEnabledManager.isAutofillEnabled = isAutofillEnabledAndSupported
                chromeThirdPartyAutofillEnabledManager.chromeThirdPartyAutofillStatus =
                    chromeAutofillStatus
            }
            .launchIn(lifecycleScope)
    }
}
