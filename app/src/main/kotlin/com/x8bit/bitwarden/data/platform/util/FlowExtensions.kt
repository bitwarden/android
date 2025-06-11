package com.x8bit.bitwarden.data.platform.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
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
