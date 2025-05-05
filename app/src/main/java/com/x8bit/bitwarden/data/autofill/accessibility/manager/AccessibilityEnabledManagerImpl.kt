package com.x8bit.bitwarden.data.autofill.accessibility.manager

import android.content.Context
import com.x8bit.bitwarden.data.autofill.accessibility.util.isAccessibilityServiceEnabled
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The default implementation of [AccessibilityEnabledManager].
 */
class AccessibilityEnabledManagerImpl(
    private val context: Context,
) : AccessibilityEnabledManager {
    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(
        value = context.isAccessibilityServiceEnabled,
    )

    init {
        mutableIsAccessibilityEnabledStateFlow.value = context.isAccessibilityServiceEnabled
    }

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()

    override fun refreshAccessibilityEnabledFromSettings() {
        mutableIsAccessibilityEnabledStateFlow.value = context.isAccessibilityServiceEnabled
    }
}
