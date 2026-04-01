package com.x8bit.bitwarden.data.billing.manager

import android.content.Context
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * F-Droid implementation of [PlayBillingManager]. Always returns `true` since
 * F-Droid users are eligible for the Premium upgrade flow.
 */
@OmitFromCoverage
@Suppress("UnusedParameter")
class PlayBillingManagerImpl(
    context: Context,
    dispatcherManager: DispatcherManager,
) : PlayBillingManager {

    override val isInAppBillingSupportedFlow: StateFlow<Boolean> =
        MutableStateFlow(true)
}
