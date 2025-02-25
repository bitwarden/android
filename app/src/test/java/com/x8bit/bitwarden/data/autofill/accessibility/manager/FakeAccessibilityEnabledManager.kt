package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAccessibilityEnabledManager : AccessibilityEnabledManager {

    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(value = false)

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()

    override fun updateAccessibilityEnabledStateFlow(isEnabled: Boolean) {
        mutableIsAccessibilityEnabledStateFlow.value = isEnabled
    }

    override fun refreshAccessibilityEnabledFromSettings() {
        // Not used in FakeAccessibilityEnabledManager tests
    }

    var isAccessibilityEnabled: Boolean
        get() = mutableIsAccessibilityEnabledStateFlow.value
        set(value) {
            mutableIsAccessibilityEnabledStateFlow.value = value
        }
}
