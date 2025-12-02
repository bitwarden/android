package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import timber.log.Timber

/**
 * Default throttle delay in milliseconds to prevent accidental double-clicks.
 */
private const val DEFAULT_THROTTLE_DELAY_MS = 300L

/**
 * Creates a throttled click handler that prevents rapid successive clicks.
 *
 * @param onClick The action to perform when clicked.
 * @param throttleDelayMs The minimum time in milliseconds between clicks.
 * Defaults to [DEFAULT_THROTTLE_DELAY_MS].
 * @return A throttled click handler function.
 */
@Composable
fun throttledOnClick(
    onClick: () -> Unit,
    throttleDelayMs: Long = DEFAULT_THROTTLE_DELAY_MS,
): () -> Unit {
    var lastClickTime by remember { mutableLongStateOf(0L) }

    return remember(onClick, throttleDelayMs) {
        {
            val now = System.currentTimeMillis()
            Timber.d("throttledOnClick lastClickTime: $lastClickTime, now: $now")
            if (now - lastClickTime >= throttleDelayMs) {
                lastClickTime = now
                onClick()
            }
        }
    }
}
