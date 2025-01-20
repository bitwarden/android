package com.x8bit.bitwarden.data.platform.manager.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.Lifecycle
import com.x8bit.bitwarden.data.platform.manager.model.AppResumeScreenData
import com.x8bit.bitwarden.ui.platform.base.util.LivecycleEventEffect
import com.x8bit.bitwarden.ui.platform.composition.LocalAppResumeStateManager

interface AppResumeStateManager {
    val appResumeState: State<AppResumeScreenData?>

    fun updateScreenData(data: AppResumeScreenData?)
}

class AppResumeStateManagerImpl : AppResumeStateManager {
    private val mutableAppResumeState = mutableStateOf<AppResumeScreenData?>(null)
    override val appResumeState: State<AppResumeScreenData?> = mutableAppResumeState

    override fun updateScreenData(data: AppResumeScreenData?) {
        mutableAppResumeState.value = data
    }
}

/**
 * Consumer
 *
 * onDataUpdate (call in central location: MainViewModel -> updates the data source through action
 * handling.
 */
@Composable
fun ObserveScreenDataEffect(onDataUpdate: (AppResumeScreenData?) -> Unit) {
    val appResumeStateManager = LocalAppResumeStateManager.current
    LaunchedEffect(appResumeStateManager.appResumeState.value) {
        onDataUpdate(appResumeStateManager.appResumeState.value)
    }
}

/**
 * Producer
 *
 * Add to screen where needed and pass in the necessary instance of [AppResumeScreenData]
 */
@Composable
fun RegisterScreenDataOnLifecycleEffect(appResumeStateProvider: () -> AppResumeScreenData) {
    val appResumeStateManager = LocalAppResumeStateManager.current
    LivecycleEventEffect { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> {
                appResumeStateManager.updateScreenData(data = appResumeStateProvider())
            }

            Lifecycle.Event.ON_STOP -> {
                appResumeStateManager.updateScreenData(data = null)
            }

            else -> Unit
        }
    }
}
