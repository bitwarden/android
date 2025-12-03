package com.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Creates a throttled click handler that prevents rapid successive clicks.
 *
 * @param coroutineScope The coroutine scope for launching click handlers.
 * @param delayMs The minimum time in milliseconds between clicks.
 * @param onClick The action to perform when clicked.
 * @return A throttled click handler function.
 */
@Composable
fun throttledClick(
    coroutineScope: CoroutineScope = rememberCoroutineScope(),
    delayMs: Long = 300,
    onClick: () -> Unit,
): () -> Unit {
    var isEnabled by remember { mutableStateOf(value = true) }
    return {
        coroutineScope.launch {
            if (isEnabled) {
                isEnabled = false
                onClick()
                delay(timeMillis = delayMs)
                isEnabled = true
            }
        }
    }
}
