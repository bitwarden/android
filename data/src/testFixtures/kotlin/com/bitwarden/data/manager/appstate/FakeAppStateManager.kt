package com.bitwarden.data.manager.appstate

import com.bitwarden.data.manager.appstate.model.AppCreationState
import com.bitwarden.data.manager.appstate.model.AppForegroundState
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
