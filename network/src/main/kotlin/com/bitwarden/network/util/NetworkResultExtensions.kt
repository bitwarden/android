package com.bitwarden.network.util

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.NetworkResult

/**
 * Converts the [NetworkResult] to a [Result].
 */
internal fun <T> NetworkResult<T>.toResult(): Result<T> =
    when (this) {
        is NetworkResult.Failure -> this.throwable.asFailure()
        is NetworkResult.Success -> this.value.asSuccess()
    }
