package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response body from the SSO prevalidate request.
 */
@Serializable
sealed class PrevalidateSsoResponseJson {
    /**
     * Models json body of a successful response.
     */
    @Serializable
    data class Success(
        @SerialName("token") val token: String?,
    ) : PrevalidateSsoResponseJson()

    /**
     * Models json body of an error response.
     */
    @Serializable
    data class Error(
        @SerialName("message") val message: String?,
    ) : PrevalidateSsoResponseJson()
}
