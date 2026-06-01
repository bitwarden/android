package com.x8bit.bitwarden.data.platform.repository.model

/**
 * A local data state used for handling local data in the repository layer.
 */
sealed class LocalDataState<out T> {

    /**
     * Data that is being wrapped by [LocalDataState].
     */
    abstract val data: T?

    /**
     * Loading state representing the absence of data.
     */
    data object Loading : LocalDataState<Nothing>() {
        override val data: Nothing? get() = null
    }

    /**
     * Loaded state representing the availability of data.
     */
    data class Loaded<T>(
        override val data: T,
    ) : LocalDataState<T>()

    /**
     * Error state that may or may not have data available.
     */
    data class Error<T>(
        val error: Throwable,
        override val data: T? = null,
    ) : LocalDataState<T>()
}
