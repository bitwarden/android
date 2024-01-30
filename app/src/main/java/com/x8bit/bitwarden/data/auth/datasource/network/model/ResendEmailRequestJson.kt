package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Hold the information necessary to resend the email with the
 * two-factor verification code.
 *
 * @property deviceIdentifier The device identifier.
 * @property email The user's email address.
 * @property passwordHash The master password hash, if the user is logging
 * in via the master password.
 * @property ssoToken The sso token, if the user is logging in via single sign on.
 */
@Serializable
data class ResendEmailRequestJson(
    @SerialName("DeviceIdentifier")
    val deviceIdentifier: String,

    @SerialName("Email")
    val email: String,

    @SerialName("MasterPasswordHash")
    val passwordHash: String?,

    @SerialName("SsoEmail2FaSessionToken")
    val ssoToken: String?,
)
