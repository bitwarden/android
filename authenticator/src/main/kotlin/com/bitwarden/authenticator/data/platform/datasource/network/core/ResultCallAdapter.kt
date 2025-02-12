package com.bitwarden.authenticator.data.platform.datasource.network.core

import retrofit2.Call
import retrofit2.CallAdapter
import java.lang.reflect.Type

/**
 * A [CallAdapter] for wrapping network requests into [kotlin.Result].
 */
class ResultCallAdapter<T>(
    private val successType: Type,
) : CallAdapter<T, Call<Result<T>>> {

    override fun responseType(): Type = successType
    override fun adapt(call: Call<T>): Call<Result<T>> = ResultCall(call, successType)
}
