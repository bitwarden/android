package com.x8bit.bitwarden.data.platform.datasource.network.model

import retrofit2.HttpException
import java.io.IOException

/**
 * Represents different types of errors that can occur in the Bitwarden application.
 */
sealed class BitwardenError {
    /**
     * An abstract property that holds the underlying throwable that caused the error.
     */
    abstract val throwable: Throwable

    /**
     * Errors related to HTTP requests and responses.
     */
    data class Http(override val throwable: HttpException) : BitwardenError() {
        /**
         * The error code of the HTTP response.
         */
        val code: Int get() = throwable.code()

        /**
         * The response body of the HTTP response.
         */
        val responseBodyString: String? by lazy {
            throwable.response()?.errorBody()?.string()
        }
    }

    /**
     * Errors related to network.
     */
    data class Network(override val throwable: IOException) : BitwardenError()

    /**
     * Other types of errors not covered by any special cases.
     */
    data class Other(override val throwable: Throwable) : BitwardenError()
}

/**
 * Convert a [Throwable] into a [BitwardenError].
 */
fun Throwable.toBitwardenError(): BitwardenError {
    return when (this) {
        is IOException -> BitwardenError.Network(this)
        is HttpException -> BitwardenError.Http(this)
        else -> BitwardenError.Other(this)
    }
}
