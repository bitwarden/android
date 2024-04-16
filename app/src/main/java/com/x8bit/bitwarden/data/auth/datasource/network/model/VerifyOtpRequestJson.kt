package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for verifying a passcode.
 *
 * @param oneTimePasscode The one-time passcode to verify.
 */
@Serializable
data class VerifyOtpRequestJson(
    @SerialName("OTP")
    val oneTimePasscode: String,
)
