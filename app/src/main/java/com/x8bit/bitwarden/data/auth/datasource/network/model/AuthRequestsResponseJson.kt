package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Response body for authentication requests used for Login with device.
 *
 * @property authRequests The list of auth requests.
 */
@Serializable
data class AuthRequestsResponseJson(
    @SerialName("data") val authRequests: List<AuthRequest>,
) {
    /**
     * Response body for an authentication request.
     *
     * @param id The id of this auth request.
     * @param publicKey The user's public key.
     * @param platform The platform from which this request was sent.
     * @param ipAddress The IP address of the device from which this request was sent.
     * @param key The key of this auth request.
     * @param masterPasswordHash The hash for this user's master password.
     * @param creationDate The date & time on which this request was created.
     * @param responseDate The date & time on which this request was responded to.
     * @param requestApproved Whether this request was approved.
     * @param originUrl The origin URL of this auth request.
     */
    @Serializable
    data class AuthRequest(
        @SerialName("id")
        val id: String,

        @SerialName("publicKey")
        val publicKey: String,

        @SerialName("requestDeviceType")
        val platform: String,

        @SerialName("requestIpAddress")
        val ipAddress: String,

        @SerialName("key")
        val key: String?,

        @SerialName("masterPasswordHash")
        val masterPasswordHash: String?,

        @SerialName("creationDate")
        @Contextual
        val creationDate: ZonedDateTime,

        @SerialName("responseDate")
        @Contextual
        val responseDate: ZonedDateTime?,

        @SerialName("requestApproved")
        val requestApproved: Boolean?,

        @SerialName("origin")
        val originUrl: String,
    )
}
