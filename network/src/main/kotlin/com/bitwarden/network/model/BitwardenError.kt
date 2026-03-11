package com.bitwarden.network.model

import com.bitwarden.network.exception.CookieRedirectException
import okhttp3.ResponseBody.Companion.toResponseBody
import retrofit2.HttpException
import retrofit2.Response
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
        // CookieRedirectException is a subclass of IOException thrown when SSO cookies
        // expire in a load-balanced environment. It must be checked before IOException to
        // avoid being classified as a generic Network error. We synthesize an Http error
        // with a JSON body so the exception's message propagates through the existing
        // parseErrorBodyOrNull pipeline used by service-layer recoverCatching blocks.
        is CookieRedirectException -> {
            BitwardenError.Http(
                throwable = HttpException(
                    Response.error<Any>(
                        HTTP_CODE_BAD_REQUEST,
                        """{"message": "${this.message}"}""".toResponseBody(),
                    ),
                ),
            )
        }

        is IOException -> BitwardenError.Network(this)
        is HttpException -> BitwardenError.Http(this)
        else -> BitwardenError.Other(this)
    }
}

private const val HTTP_CODE_BAD_REQUEST: Int = 400
