package com.x8bit.bitwarden.data.autofill.manager

import android.view.autofill.AutofillManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.bitwarden.data.manager.appstate.AppStateManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillManager
import com.x8bit.bitwarden.data.autofill.model.browser.BrowserThirdPartyAutofillStatus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

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
    private val autofillManagerIsEnabled: Boolean
        get() = try {
            autofillManager.isEnabled
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            Timber.e(e, "autofillManager.isEnabled failed")
            false
        }

    private val autofillManagerHasEnabledAutofillServices: Boolean
        get() = try {
            autofillManager.hasEnabledAutofillServices()
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            Timber.e(e, "autofillManager.hasEnabledAutofillServices() failed")
            false
        }

    private val autofillManagerIsAutofillSupported: Boolean
        get() = try {
            autofillManager.isAutofillSupported
        } catch (@Suppress("TooGenericExceptionCaught") e: RuntimeException) {
            Timber.e(e, "autofillManager.isAutofillSupported() failed")
            false
        }

    private val isAutofillEnabledAndSupported: Boolean
        get() = autofillManagerIsEnabled &&
            autofillManagerHasEnabledAutofillServices &&
            autofillManagerIsAutofillSupported

    private val browserAutofillStatus: BrowserThirdPartyAutofillStatus
        get() = BrowserThirdPartyAutofillStatus(
            braveStableStatusData = browserThirdPartyAutofillManager.stableBraveAutofillStatus,
            chromeStableStatusData = browserThirdPartyAutofillManager.stableChromeAutofillStatus,
            chromeBetaChannelStatusData = browserThirdPartyAutofillManager.betaChromeAutofillStatus,
            vivaldiStableChannelStatusData = browserThirdPartyAutofillManager
                .stableVivaldiAutofillStatus,
            defaultBrowserPackageName = browserThirdPartyAutofillManager
                .defaultBrowserPackageName,
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
