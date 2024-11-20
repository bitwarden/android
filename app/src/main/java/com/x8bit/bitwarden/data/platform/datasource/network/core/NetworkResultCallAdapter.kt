package com.x8bit.bitwarden.data.platform.datasource.network.core

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

/**
 * A [CallAdapter] for wrapping network requests into [NetworkResult].
 */
class NetworkResultCallAdapter<T>(
    private val successType: Type,
) : CallAdapter<T, Call<NetworkResult<T>>> {

    override fun responseType(): Type = successType
    override fun adapt(call: Call<T>): Call<NetworkResult<T>> = NetworkResultCall(call, successType)
}
