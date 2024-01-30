package com.x8bit.bitwarden.data.autofill.manager

import android.view.autofill.AutofillManager
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Primary implementation of [AutofillActivityManager].
 */
class AutofillActivityManagerImpl(
    private val autofillManager: AutofillManager,
    private val appForegroundManager: AppForegroundManager,
    private val autofillEnabledManager: AutofillEnabledManager,
    private val dispatcherManager: DispatcherManager,
) : AutofillActivityManager {
    private val isAutofillEnabledAndSupported: Boolean
        get() = autofillManager.isEnabled &&
            autofillManager.hasEnabledAutofillServices() &&
            autofillManager.isAutofillSupported

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    init {
        appForegroundManager
            .appForegroundStateFlow
            .onEach {
                autofillEnabledManager.isAutofillEnabled = isAutofillEnabledAndSupported
            }
            .launchIn(unconfinedScope)
    }
}
