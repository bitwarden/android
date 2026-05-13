package com.bitwarden.network.model

/**
 * Models response body from fetching a Cipher.
 */
sealed class GetCipherResponse {
    /**
     * Models response of a successful Get Cipher request.
     */
    data class Success(
        val cipher: SyncResponseJson.Cipher,
    ) : GetCipherResponse()

    /**
     * Models the response of an unfindable Get Cipher request.
     */
    data class NotFound(
        val throwable: Throwable,
    ) : GetCipherResponse()
}
