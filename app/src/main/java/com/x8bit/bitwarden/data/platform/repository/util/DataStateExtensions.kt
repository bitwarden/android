package com.x8bit.bitwarden.data.platform.repository.util

import com.x8bit.bitwarden.data.platform.repository.model.DataState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.flow.update

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

/**
 * Emits all values of a [DataState] [Flow] until it emits a [DataState.Loaded].
 */
fun <T : Any?> Flow<DataState<T>>.takeUntilLoaded(): Flow<DataState<T>> = transformWhile {
    emit(it)
    it !is DataState.Loaded
}

/**
 * Updates the [DataState] to [DataState.Pending] if there is data present or [DataState.Loading]
 * if no data is present.
 */
fun <T : Any?> MutableStateFlow<DataState<T>>.updateToPendingOrLoading() {
    update { dataState ->
        dataState.data
            ?.let { data -> DataState.Pending(data = data) }
            ?: DataState.Loading
    }
}
