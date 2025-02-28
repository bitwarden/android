package com.x8bit.bitwarden.data.autofill.accessibility.manager

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAccessibilityEnabledManager : AccessibilityEnabledManager {

    var isAccessibilityEnabled: Boolean = false

    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(
        value = isAccessibilityEnabled,
    )

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()

    override fun refreshAccessibilityEnabledFromSettings() {
        mutableIsAccessibilityEnabledStateFlow.value = isAccessibilityEnabled
    }
}
