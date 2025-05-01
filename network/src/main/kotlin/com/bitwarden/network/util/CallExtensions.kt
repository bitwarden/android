package com.bitwarden.network.util

import com.bitwarden.network.core.NetworkResultCall
import com.bitwarden.network.model.NetworkResult
import retrofit2.Call

/**
 * Synchronously executes the [Call] and returns the [NetworkResult].
 */
internal inline fun <reified T : Any> Call<T>.executeForNetworkResult(): NetworkResult<T> =
    this
        .toNetworkResultCall()
        .executeForResult()

/**
 * Wraps the existing [Call] in a [NetworkResultCall].
 */
internal inline fun <reified T : Any> Call<T>.toNetworkResultCall(): NetworkResultCall<T> =
    NetworkResultCall(
        backingCall = this,
        successType = T::class.java,
    )
