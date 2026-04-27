package com.bitwarden.network.core

import com.bitwarden.network.model.NetworkResult
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

/**
 * A [retrofit2.CallAdapter] for wrapping network requests into [NetworkResult].
 */
internal class NetworkResultCallAdapter<T>(
    private val successType: Type,
) : CallAdapter<T, Call<NetworkResult<T>>> {

    override fun responseType(): Type = successType
    override fun adapt(call: Call<T>): Call<NetworkResult<T>> = NetworkResultCall(call, successType)
}
