package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A faked implementation of [AppStateManager]
 */
class FakeAppStateManager : AppStateManager {
    private val mutableAppForegroundStateFlow = MutableStateFlow(AppForegroundState.BACKGROUNDED)

    override val appForegroundStateFlow: StateFlow<AppForegroundState>
        get() = mutableAppForegroundStateFlow.asStateFlow()

    /**
     * The current [AppForegroundState] tracked by the [appForegroundStateFlow].
     */
    var appForegroundState: AppForegroundState
        get() = mutableAppForegroundStateFlow.value
        set(value) {
            mutableAppForegroundStateFlow.value = value
        }
}
