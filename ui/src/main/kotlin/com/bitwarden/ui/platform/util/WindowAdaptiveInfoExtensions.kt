package com.bitwarden.ui.platform.util

import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.bitwarden.ui.platform.model.WindowSize

/**
 * Remembers the [WindowSize] class for the window corresponding to the current window metrics.
 */
@Composable
fun rememberWindowSize(
    windowAdaptiveInfo: WindowAdaptiveInfo = currentWindowAdaptiveInfo(),
): WindowSize {
    return remember(key1 = windowAdaptiveInfo.windowSizeClass) {
        windowAdaptiveInfo.getWindowSize()
    }
}

/**
 * Retrieves the [WindowSize] class for the window corresponding to the current window metrics.
 */
fun WindowAdaptiveInfo.getWindowSize(): WindowSize {
    // Currently the app only operates with the Compact and Medium sizes in
    // mind, but we can add support for others in the future here.
    return if (this.windowSizeClass.isWidthAtLeastBreakpoint(widthDpBreakpoint = 600)) {
        WindowSize.Medium
    } else {
        WindowSize.Compact
    }
}
