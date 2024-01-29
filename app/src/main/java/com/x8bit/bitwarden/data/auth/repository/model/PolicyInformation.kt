package com.x8bit.bitwarden.data.auth.repository.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The policy information decoded from the [data] parameter of the [SyncResponseJson.Policy] object.
 */
@Serializable
sealed class PolicyInformation {
    /**
     * Represents a policy enforcing rules on the user's master password.
     *
     * @property minLength The minimum length of the password.
     * @property minComplexity The minimum complexity of the password.
     * @property requireUpper Whether the password requires upper case letters.
     * @property requireLower Whether the password requires lower case letters.
     * @property requireNumbers Whether the password requires numbers.
     * @property requireSpecial Whether the password requires special characters.
     * @property enforceOnLogin Whether the password should be enforced on login.
     */
    @Serializable
    data class MasterPassword(
        @SerialName("minLength")
        val minLength: Int?,

        @SerialName("minComplexity")
        val minComplexity: Int?,

        @SerialName("requireUpper")
        val requireUpper: Boolean?,

        @SerialName("requireLower")
        val requireLower: Boolean?,

        @SerialName("requireNumbers")
        val requireNumbers: Boolean?,

        @SerialName("requireSpecial")
        val requireSpecial: Boolean?,

        @SerialName("enforceOnLogin")
        val enforceOnLogin: Boolean?,
    ) : PolicyInformation()
}
