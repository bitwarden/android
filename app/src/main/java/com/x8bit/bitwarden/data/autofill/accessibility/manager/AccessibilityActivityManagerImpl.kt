package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.x8bit.bitwarden.data.autofill.accessibility.util.isAccessibilityServiceEnabled
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The default implementation of the [AccessibilityActivityManager].
 */
class AccessibilityActivityManagerImpl(
    private val context: Context,
    private val accessibilityEnabledManager: AccessibilityEnabledManager,
    appStateManager: AppStateManager,
    lifecycleScope: LifecycleCoroutineScope,
) : AccessibilityActivityManager {
    init {
        appStateManager
            .appForegroundStateFlow
            .onEach {
                accessibilityEnabledManager.isAccessibilityEnabled =
                    context.isAccessibilityServiceEnabled
            }
            .launchIn(lifecycleScope)
    }
}
