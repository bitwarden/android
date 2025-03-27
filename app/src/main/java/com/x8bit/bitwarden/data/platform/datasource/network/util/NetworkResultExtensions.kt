package com.x8bit.bitwarden.data.platform.datasource.network.util

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult

/**
 * Converts the [NetworkResult] to a [Result].
 */
fun <T> NetworkResult<T>.toResult(): Result<T> =
    when (this) {
        is NetworkResult.Failure -> this.throwable.asFailure()
        is NetworkResult.Success -> this.value.asSuccess()
    }
