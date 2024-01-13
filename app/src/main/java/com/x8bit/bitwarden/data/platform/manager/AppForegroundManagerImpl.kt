package com.x8bit.bitwarden.data.platform.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Primary implementation of [AppForegroundManager].
 */
class AppForegroundManagerImpl(
    processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : AppForegroundManager {
    private val mutableAppForegroundStateFlow =
        MutableStateFlow(AppForegroundState.BACKGROUNDED)

    override val appForegroundStateFlow: StateFlow<AppForegroundState>
        get() = mutableAppForegroundStateFlow.asStateFlow()

    init {
        processLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) {
                    mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
                }

                override fun onStop(owner: LifecycleOwner) {
                    mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED
                }
            },
        )
    }
}
