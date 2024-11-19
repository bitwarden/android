package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.view.accessibility.AccessibilityManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The default implementation of [AccessibilityEnabledManager].
 */
class AccessibilityEnabledManagerImpl(
    accessibilityManager: AccessibilityManager,
) : AccessibilityEnabledManager {
    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(value = false)

    init {
        accessibilityManager.addAccessibilityStateChangeListener(
            AccessibilityManager.AccessibilityStateChangeListener { isEnabled ->
                mutableIsAccessibilityEnabledStateFlow.value = isEnabled
            },
        )
    }

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()
}
