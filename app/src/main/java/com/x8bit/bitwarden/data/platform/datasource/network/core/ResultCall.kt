package com.x8bit.bitwarden.data.platform.datasource.network.core

import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import okhttp3.Request
import okio.IOException
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import java.lang.reflect.Type

/**
 * The integer code value for a "No Content" response.
 */
private const val NO_CONTENT_RESPONSE_CODE: Int = 204

/**
 * A [Call] for wrapping a network request into a [Result].
 */
class ResultCall<T>(
    private val backingCall: Call<T>,
    private val successType: Type,
) : Call<Result<T>> {
    override fun cancel(): Unit = backingCall.cancel()

    override fun clone(): Call<Result<T>> = ResultCall(backingCall, successType)

    override fun enqueue(callback: Callback<Result<T>>): Unit = backingCall.enqueue(
        object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                callback.onResponse(this@ResultCall, Response.success(response.toResult()))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                callback.onResponse(this@ResultCall, Response.success(t.asFailure()))
            }
        },
    )

    @Suppress("TooGenericExceptionCaught")
    override fun execute(): Response<Result<T>> =
        try {
            Response.success(backingCall.execute().toResult())
        } catch (ioException: IOException) {
            Response.success(ioException.asFailure())
        } catch (runtimeException: RuntimeException) {
            Response.success(runtimeException.asFailure())
        }

    override fun isCanceled(): Boolean = backingCall.isCanceled

    override fun isExecuted(): Boolean = backingCall.isExecuted

    override fun request(): Request = backingCall.request()

    override fun timeout(): Timeout = backingCall.timeout()

    /**
     * Synchronously send the request and return its response as a [Result].
     */
    fun executeForResult(): Result<T> = requireNotNull(execute().body())

    private fun Response<T>.toResult(): Result<T> =
        if (!this.isSuccessful) {
            HttpException(this).asFailure()
        } else {
            val body = this.body()
            @Suppress("UNCHECKED_CAST")
            when {
                // We got a nonnull T as the body, just return it.
                body != null -> body.asSuccess()
                // We expected the body to be null since the successType is Unit, just return Unit.
                successType == Unit::class.java -> (Unit as T).asSuccess()
                // We allow null for 204's, just return null.
                this.code() == NO_CONTENT_RESPONSE_CODE -> (null as T).asSuccess()
                // All other null bodies result in an error.
                else -> IllegalStateException("Unexpected null body!").asFailure()
            }
        }
}
