package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Hold the information necessary to resend the email with the
 * new device verification code.
 *
 * @property email The user's email address.
 * @property passwordHash The master password hash
 */
@Serializable
data class ResendNewDeviceOtpRequestJson(
    @SerialName("Email")
    val email: String,

    @SerialName("MasterPasswordHash")
    val passwordHash: String?,
)
