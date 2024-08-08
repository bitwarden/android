package com.x8bit.bitwarden.data.autofill.fido2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Enum class indicating the type of user verification requested by the relying party.
 */
@Serializable
enum class UserVerificationRequirement {
    /**
     * User verification should not be performed.
     */
    @SerialName("discouraged")
    DISCOURAGED,

    /**
     * User verification is preferred, if supported by the device or application.
     */
    @SerialName("preferred")
    PREFERRED,

    /**
     * User verification is required. If is cannot be performed the registration process
     * should be terminated.
     */
    @SerialName("required")
    REQUIRED,
}
