package com.x8bit.bitwarden.data.platform.manager.util

import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import com.x8bit.bitwarden.data.platform.manager.model.AppCreationState
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A faked implementation of [AppStateManager]
 */
class FakeAppStateManager : AppStateManager {
    private val mutableAppCreationStateFlow =
        MutableStateFlow<AppCreationState>(AppCreationState.Destroyed)
    private val mutableAppForegroundStateFlow = MutableStateFlow(AppForegroundState.BACKGROUNDED)

    override val appCreatedStateFlow: StateFlow<AppCreationState>
        get() = mutableAppCreationStateFlow.asStateFlow()

    override val appForegroundStateFlow: StateFlow<AppForegroundState>
        get() = mutableAppForegroundStateFlow.asStateFlow()

    /**
     * The current [AppCreationState] tracked by the [appCreatedStateFlow].
     */
    var appCreationState: AppCreationState
        get() = mutableAppCreationStateFlow.value
        set(value) {
            mutableAppCreationStateFlow.value = value
        }

    /**
     * The current [AppForegroundState] tracked by the [appForegroundStateFlow].
     */
    var appForegroundState: AppForegroundState
        get() = mutableAppForegroundStateFlow.value
        set(value) {
            mutableAppForegroundStateFlow.value = value
        }
}
