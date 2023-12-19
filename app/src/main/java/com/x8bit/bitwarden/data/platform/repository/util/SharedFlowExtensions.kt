package com.x8bit.bitwarden.data.platform.repository.util

import kotlinx.coroutines.flow.MutableSharedFlow

/**
 * Creates a [MutableSharedFlow] with a buffer of [Int.MAX_VALUE].
 */
fun <T> bufferedMutableSharedFlow(): MutableSharedFlow<T> =
    MutableSharedFlow(extraBufferCapacity = Int.MAX_VALUE)
