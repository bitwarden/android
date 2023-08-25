package com.x8bit.bitwarden.data.datasource.network

import okhttp3.Request
import okio.Timeout
import retrofit2.Call
import retrofit2.Callback
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Response.success
import java.lang.reflect.Type

/**
 * A [Call] for wrapping a network request into a [Result].
 */
class ResultCall<T>(
    private val backingCall: Call<T>,
    private val successType: Type,
) : Call<Result<T>> {
    override fun cancel(): Unit = backingCall.cancel()

    override fun clone(): Call<Result<T>> = ResultCall(backingCall, successType)

    @Suppress("UNCHECKED_CAST")
    private fun createResult(body: T?): Result<T> {
        return when {
            body != null -> Result.success(body)
            successType == Unit::class.java -> Result.success(Unit as T)
            else -> Result.failure(IllegalStateException("Unexpected null body!"))
        }
    }

    override fun enqueue(callback: Callback<Result<T>>): Unit = backingCall.enqueue(
        object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val body = response.body()
                val result: Result<T> = if (!response.isSuccessful) {
                    Result.failure(HttpException(response))
                } else {
                    createResult(body)
                }
                callback.onResponse(this@ResultCall, success(result))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                val result: Result<T> = Result.failure(t)
                callback.onResponse(this@ResultCall, success(result))
            }
        },
    )

    override fun execute(): Response<Result<T>> = throw UnsupportedOperationException(
        "This call can't be executed synchronously",
    )

    override fun isCanceled(): Boolean = backingCall.isCanceled

    override fun isExecuted(): Boolean = backingCall.isExecuted

    override fun request(): Request = backingCall.request()

    override fun timeout(): Timeout = backingCall.timeout()
}
