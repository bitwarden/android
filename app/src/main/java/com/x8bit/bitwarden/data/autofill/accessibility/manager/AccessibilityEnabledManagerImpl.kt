package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The default implementation of [AccessibilityEnabledManager].
 */
class AccessibilityEnabledManagerImpl : AccessibilityEnabledManager {
    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(value = false)

    override var isAccessibilityEnabled: Boolean
        get() = mutableIsAccessibilityEnabledStateFlow.value
        set(value) {
            mutableIsAccessibilityEnabledStateFlow.value = value
        }

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()
}
