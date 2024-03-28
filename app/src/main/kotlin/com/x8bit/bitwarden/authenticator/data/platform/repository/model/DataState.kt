package com.x8bit.bitwarden.authenticator.data.platform.repository.model

/**
 * A data state that can be used as a template for data in the repository layer.
 */
sealed class DataState<out T> {

    /**
     * Data that is being wrapped by [DataState].
     */
    abstract val data: T?

    /**
     * Loading state that has no data is available.
     */
    data object Loading : DataState<Nothing>() {
        override val data: Nothing? get() = null
    }

    /**
     * Loaded state that has data available.
     */
    data class Loaded<T>(
        override val data: T,
    ) : DataState<T>()

    /**
     * Pending state that has data available.
     */
    data class Pending<T>(
        override val data: T,
    ) : DataState<T>()

    /**
     * Error state that may have data available.
     */
    data class Error<T>(
        val error: Throwable,
        override val data: T? = null,
    ) : DataState<T>()

    /**
     * No network state that may have data is available.
     */
    data class NoNetwork<T>(
        override val data: T? = null,
    ) : DataState<T>()
}
