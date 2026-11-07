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
    context: Context,
) : AccessibilityEnabledManager {
    // Seeded from the platform until the service reports its connection state. If the platform
    // does not report our service, this stays false until the service connects.
    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(
        value = context.isAccessibilityServiceEnabled,
    )

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()

    override fun setAccessibilityServiceConnected(isConnected: Boolean) {
        mutableIsAccessibilityEnabledStateFlow.value = isConnected
    }
}
