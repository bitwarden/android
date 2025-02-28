package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import com.x8bit.bitwarden.data.autofill.accessibility.util.isAccessibilityServiceEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeAccessibilityEnabledManager(
    private val context: Context,
) : AccessibilityEnabledManager {

    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(value = false)

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()

    override fun refreshAccessibilityEnabledFromSettings() {
        mutableIsAccessibilityEnabledStateFlow.value = context.isAccessibilityServiceEnabled
    }

    var isAccessibilityEnabled: Boolean
        get() = mutableIsAccessibilityEnabledStateFlow.value
        set(value) {
            mutableIsAccessibilityEnabledStateFlow.value = value
        }
}
