package com.x8bit.bitwarden.data.platform.datasource.network.model

import androidx.annotation.Keep

/**
 * A wrapper class for a network result for type [T]. If the network request is successful, the
 * response will be a [Success] containing the data. If the network request is a failure, the
 * response will be a [Failure] containing the [Throwable].
 */
@Keep
sealed class NetworkResult<out T> {
    /**
     * A successful network result with the relevant [T] data.
     */
    data class Success<T>(val value: T) : NetworkResult<T>()

    /**
     * A failed network result with the relevant [throwable] error.
     */
    data class Failure(val throwable: Throwable) : NetworkResult<Nothing>()
}
