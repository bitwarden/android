package com.x8bit.bitwarden.data.autofill.manager

import android.view.autofill.AutofillManager
import androidx.lifecycle.LifecycleCoroutineScope
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * Primary implementation of [AutofillActivityManager].
 */
class AutofillActivityManagerImpl(
    private val autofillManager: AutofillManager,
    private val autofillEnabledManager: AutofillEnabledManager,
    appStateManager: AppStateManager,
    lifecycleScope: LifecycleCoroutineScope,
) : AutofillActivityManager {
    private val isAutofillEnabledAndSupported: Boolean
        get() = autofillManager.isEnabled &&
            autofillManager.hasEnabledAutofillServices() &&
            autofillManager.isAutofillSupported

    init {
        appStateManager
            .appForegroundStateFlow
            .onEach { autofillEnabledManager.isAutofillEnabled = isAutofillEnabledAndSupported }
            .launchIn(lifecycleScope)
    }
}
