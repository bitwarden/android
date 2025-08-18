package com.bitwarden.core.data.repository.util

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first

/**
 * Creates a [MutableSharedFlow] with a buffer of [Int.MAX_VALUE] and the given [replay] count.
 */
fun <T> bufferedMutableSharedFlow(
    replay: Int = 0,
): MutableSharedFlow<T> =
    MutableSharedFlow(
        replay = replay,
        extraBufferCapacity = Int.MAX_VALUE,
    )

/**
 * Emits a [value] to this shared flow, suspending until there is at least one subscriber.
 */
suspend fun <T> MutableSharedFlow<T>.emitWhenSubscribedTo(value: T) {
    // We have subscribers, so emit now.
    if (subscriptionCount.value > 0) {
        emit(value = value)
        return
    }
    // We are going to wait until there is at least one subscriber, then emit.
    subscriptionCount.first { it > 0 }
    emit(value = value)
}
