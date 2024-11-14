package com.x8bit.bitwarden.data.platform.datasource.network.core

import com.x8bit.bitwarden.data.platform.datasource.network.model.NetworkResult
import okhttp3.Request
import okio.IOException
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import timber.log.Timber
import java.lang.reflect.Type

/**
 * The integer code value for a "No Content" response.
 */
private const val NO_CONTENT_RESPONSE_CODE: Int = 204

/**
 * A [Call] for wrapping a network request into a [NetworkResult].
 */
@Suppress("TooManyFunctions")
class NetworkResultCall<T>(
    private val backingCall: Call<T>,
    private val successType: Type,
) : Call<NetworkResult<T>> {
    override fun cancel(): Unit = backingCall.cancel()

    override fun clone(): Call<NetworkResult<T>> = NetworkResultCall(backingCall, successType)

    override fun enqueue(callback: Callback<NetworkResult<T>>): Unit = backingCall.enqueue(
        object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                callback.onResponse(
                    this@NetworkResultCall,
                    Response.success(response.toNetworkResult()),
                )
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                callback.onResponse(this@NetworkResultCall, Response.success(t.toFailure()))
            }
        },
    )

    @Suppress("TooGenericExceptionCaught")
    override fun execute(): Response<NetworkResult<T>> =
        try {
            Response.success(backingCall.execute().toNetworkResult())
        } catch (ioException: IOException) {
            Response.success(ioException.toFailure())
        } catch (runtimeException: RuntimeException) {
            Response.success(runtimeException.toFailure())
        }

    override fun isCanceled(): Boolean = backingCall.isCanceled

    override fun isExecuted(): Boolean = backingCall.isExecuted

    override fun request(): Request = backingCall.request()

    override fun timeout(): Timeout = backingCall.timeout()

    /**
     * Synchronously send the request and return its response as a [NetworkResult].
     */
    fun executeForResult(): NetworkResult<T> = requireNotNull(execute().body())

    private fun Throwable.toFailure(): NetworkResult<T> {
        // We rebuild the URL without query params, we do not want to log those
        val url = backingCall.request().url.toUrl().run { "$protocol://$authority$path" }
        Timber.w(this, "Network Error: $url")
        return NetworkResult.Failure(this)
    }

    private fun Response<T>.toNetworkResult(): NetworkResult<T> =
        if (!this.isSuccessful) {
            HttpException(this).toFailure()
        } else {
            val body = this.body()
            @Suppress("UNCHECKED_CAST")
            when {
                // We got a nonnull T as the body, just return it.
                body != null -> NetworkResult.Success(body)
                // We expected the body to be null since the successType is Unit, just return Unit.
                successType == Unit::class.java -> NetworkResult.Success(Unit as T)
                // We allow null for 204's, just return null.
                this.code() == NO_CONTENT_RESPONSE_CODE -> NetworkResult.Success(null as T)
                // All other null bodies result in an error.
                else -> IllegalStateException("Unexpected null body!").toFailure()
            }
        }
}
