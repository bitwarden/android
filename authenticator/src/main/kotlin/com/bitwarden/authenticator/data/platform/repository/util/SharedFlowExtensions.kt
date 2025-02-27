package com.bitwarden.authenticator.data.platform.repository.util

import kotlinx.coroutines.flow.MutableSharedFlow

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
