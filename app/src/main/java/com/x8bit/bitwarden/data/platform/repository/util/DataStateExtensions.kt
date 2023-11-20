package com.x8bit.bitwarden.data.platform.repository.util

import com.x8bit.bitwarden.data.platform.repository.model.DataState

/**
 * Maps the data inside a [DataState] with the given [transform].
 */
inline fun <T : Any?, R : Any?> DataState<T>.map(
    transform: (T) -> R,
): DataState<R> = when (this) {
    is DataState.Loaded -> DataState.Loaded(transform(data))
    is DataState.Loading -> DataState.Loading
    is DataState.Pending -> DataState.Pending(transform(data))
    is DataState.Error -> DataState.Error(error, data?.let(transform))
    is DataState.NoNetwork -> DataState.NoNetwork(data?.let(transform))
}
