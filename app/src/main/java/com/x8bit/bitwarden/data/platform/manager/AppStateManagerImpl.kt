package com.x8bit.bitwarden.data.platform.manager

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.x8bit.bitwarden.data.autofill.util.createdForAutofill
import com.x8bit.bitwarden.data.platform.manager.model.AppCreationState
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Primary implementation of [AppStateManager].
 */
class AppStateManagerImpl(
    application: Application,
    processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : AppStateManager {
    private val mutableAppCreationStateFlow =
        MutableStateFlow<AppCreationState>(AppCreationState.Destroyed)
    private val mutableAppForegroundStateFlow = MutableStateFlow(AppForegroundState.BACKGROUNDED)

    override val appCreatedStateFlow: StateFlow<AppCreationState>
        get() = mutableAppCreationStateFlow.asStateFlow()

    override val appForegroundStateFlow: StateFlow<AppForegroundState>
        get() = mutableAppForegroundStateFlow.asStateFlow()

    init {
        application.registerActivityLifecycleCallbacks(AppCreationCallback())
        processLifecycleOwner.lifecycle.addObserver(AppForegroundObserver())
    }

    private inner class AppForegroundObserver : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            mutableAppForegroundStateFlow.value = AppForegroundState.FOREGROUNDED
        }

        override fun onStop(owner: LifecycleOwner) {
            mutableAppForegroundStateFlow.value = AppForegroundState.BACKGROUNDED
        }
    }

    private inner class AppCreationCallback : Application.ActivityLifecycleCallbacks {
        private var activityCount: Int = 0

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            activityCount++
            // Always be in a created state if we have an activity
            mutableAppCreationStateFlow.value = AppCreationState.Created(
                isAutoFill = activity.createdForAutofill,
            )
        }

        override fun onActivityDestroyed(activity: Activity) {
            activityCount--
            if (activityCount == 0 && !activity.isChangingConfigurations) {
                mutableAppCreationStateFlow.value = AppCreationState.Destroyed
            }
        }

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    }
}
