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
            body != null -> body.asSuccess()
            successType == Unit::class.java -> (Unit as T).asSuccess()
            else -> IllegalStateException("Unexpected null body!").asFailure()
        }
    }

    override fun enqueue(callback: Callback<Result<T>>): Unit = backingCall.enqueue(
        object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                val body = response.body()
                val result: Result<T> = if (!response.isSuccessful) {
                    HttpException(response).asFailure()
                } else {
                    createResult(body)
                }
                callback.onResponse(this@ResultCall, success(result))
            }

            override fun onFailure(call: Call<T>, t: Throwable) {
                val result: Result<T> = t.asFailure()
                callback.onResponse(this@ResultCall, success(result))
            }
        },
    )

    /**
     * Synchronously send the request and return its response as a [Result].
     */
    fun executeForResult(): Result<T> = requireNotNull(execute().body())

    @Suppress("ReturnCount", "TooGenericExceptionCaught")
    override fun execute(): Response<Result<T>> {
        val response = try {
            backingCall.execute()
        } catch (ioException: IOException) {
            return success(ioException.asFailure())
        } catch (runtimeException: RuntimeException) {
            return success(runtimeException.asFailure())
        }

        return success(
            if (!response.isSuccessful) {
                HttpException(response).asFailure()
            } else {
                createResult(response.body())
            },
        )
    }

    override fun isCanceled(): Boolean = backingCall.isCanceled

    override fun isExecuted(): Boolean = backingCall.isExecuted

    override fun request(): Request = backingCall.request()

    override fun timeout(): Timeout = backingCall.timeout()
}
