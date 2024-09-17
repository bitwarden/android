package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import com.x8bit.bitwarden.data.autofill.accessibility.util.isAccessibilityServiceEnabled
import com.x8bit.bitwarden.data.platform.manager.AppForegroundManager
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The default implementation of the [AccessibilityActivityManager].
 */
class AccessibilityActivityManagerImpl(
    private val context: Context,
    private val accessibilityEnabledManager: AccessibilityEnabledManager,
    appForegroundManager: AppForegroundManager,
    lifecycleScope: LifecycleCoroutineScope,
) : AccessibilityActivityManager {
    init {
        appForegroundManager
            .appForegroundStateFlow
            .onEach {
                accessibilityEnabledManager.isAccessibilityEnabled =
                    context.isAccessibilityServiceEnabled
            }
            .launchIn(lifecycleScope)
    }
}
