package com.x8bit.bitwarden.data.platform.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Launch a new coroutine that runs [block] and will safely timeout and invoke [timeoutBlock] after
 * a duration of length [timeoutDuration] in milliseconds is elapsed.
 */
fun CoroutineScope.launchWithTimeout(
    timeoutBlock: () -> Unit,
    timeoutDuration: Long,
    block: suspend CoroutineScope.() -> Unit,
): Job =
    launch {
        try {
            withTimeout(timeoutDuration, block)
        } catch (e: TimeoutCancellationException) {
            timeoutBlock()
        }
    }
