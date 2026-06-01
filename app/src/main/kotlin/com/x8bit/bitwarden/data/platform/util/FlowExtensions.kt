package com.x8bit.bitwarden.data.platform.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Returns the first element emitted by the [Flow] or `null` if the operation exceeds the given
 * timeout of [timeMillis].
 */
suspend fun <T> Flow<T>.firstWithTimeoutOrNull(
    timeMillis: Long,
): T? = withTimeoutOrNull(timeMillis = timeMillis) { first() }

/**
 * Returns the first element emitted by the [Flow] matching the given [predicate] or `null` if the
 * operation exceeds the given timeout of [timeMillis].
 */
suspend fun <T> Flow<T>.firstWithTimeoutOrNull(
    timeMillis: Long,
    predicate: suspend (T) -> Boolean,
): T? = withTimeoutOrNull(timeMillis = timeMillis) { first(predicate) }

/**
 * Emits successive (previous, current) pairs from the upstream flow, seeding the first emission's
 * `previous` with [initial] (defaulting to `null`).
 */
fun <T> Flow<T>.scanPairs(
    initial: T? = null,
): Flow<Pair<T?, T>> = flow {
    var previous: T? = initial
    collect { current ->
        emit(previous to current)
        previous = current
    }
}
