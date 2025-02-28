package com.bitwarden.authenticator.data.platform.datasource.network.core

import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * A [CallAdapter.Factory] for wrapping network requests into [kotlin.Result].
 */
class ResultCallAdapterFactory : CallAdapter.Factory() {
    override fun get(
        returnType: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit,
    ): CallAdapter<*, *>? {
        check(returnType is ParameterizedType) { "$returnType must be parameterized" }
        val containerType = getParameterUpperBound(0, returnType)

        if (getRawType(containerType) != Result::class.java) return null
        check(containerType is ParameterizedType) { "$containerType must be parameterized" }

        val requestType = getParameterUpperBound(0, containerType)

        return if (getRawType(returnType) == Call::class.java) {
            ResultCallAdapter<Any>(successType = requestType)
        } else {
            null
        }
    }
}
