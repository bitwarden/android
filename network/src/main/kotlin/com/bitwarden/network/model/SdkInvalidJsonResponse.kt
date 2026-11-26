package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The json body of a request the server rejected, for requests made by the SDK.
 *
 * The SDK reports every rejected API request the same way, so there is no request-specific
 * response model to decode the body into.
 *
 * @property message A general, user-displayable error message.
 * @property validationErrors A map where each value is a list of error messages for each key.
 */
@Serializable
data class SdkInvalidJsonResponse(
    @SerialName("message")
    override val message: String = "",

    @SerialName("validationErrors")
    override val validationErrors: Map<String, List<String>>? = null,
) : InvalidJsonResponse
