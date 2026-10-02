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
    // Seeded from the platform so the state is correct before the service connects; on platforms
    // that no longer report our own service this is false until the service reports itself.
    private val mutableIsAccessibilityEnabledStateFlow = MutableStateFlow(
        value = context.isAccessibilityServiceEnabled,
    )

    override val isAccessibilityEnabledStateFlow: StateFlow<Boolean>
        get() = mutableIsAccessibilityEnabledStateFlow.asStateFlow()

    override var isAccessibilityServiceConnected: Boolean
        get() = mutableIsAccessibilityEnabledStateFlow.value
        set(value) {
            mutableIsAccessibilityEnabledStateFlow.value = value
        }
}
