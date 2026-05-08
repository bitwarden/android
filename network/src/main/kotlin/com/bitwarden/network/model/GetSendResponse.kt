package com.bitwarden.network.model

/**
 * Models response body from fetching a Send.
 */
sealed class GetSendResponse {
    /**
     * Models response of a successful Get Send request.
     */
    data class Success(
        val send: SyncResponseJson.Send,
    ) : GetSendResponse()

    /**
     * Models the response of an unfindable Get Send request.
     */
    data class NotFound(
        val throwable: Throwable,
    ) : GetSendResponse()
}
