package com.x8bit.bitwarden.data.auth.repository.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The policy information decoded from the `data` parameter of the [SyncResponseJson.Policy] object.
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

    /**
     * Represents a policy enforcing rules on the password generator.
     *
     * @property overridePasswordType The default type of password to be generated.
     * @property minLength The minimum length of the password.
     * @property useUpper Whether the password requires upper case letters.
     * @property useLower Whether the password requires lower case letters.
     * @property useNumbers Whether the password requires numbers.
     * @property useSpecial Whether the password requires special characters.
     * @property minNumbers The minimum number of digits in the password.
     * @property minSpecial The minimum number of special characters in the password.
     * @property minNumberWords The minimum number of words in a passphrase.
     * @property capitalize Whether to capitalize the first character of each word in a passphrase.
     * @property includeNumber Whether to include a number at the end of a passphrase.
     */
    @Serializable
    data class PasswordGenerator(
        @SerialName("overridePasswordType")
        val overridePasswordType: String?,

        @SerialName("minLength")
        val minLength: Int?,

        @SerialName("useUpper")
        val useUpper: Boolean?,

        @SerialName("useLower")
        val useLower: Boolean?,

        @SerialName("useNumbers")
        val useNumbers: Boolean?,

        @SerialName("useSpecial")
        val useSpecial: Boolean?,

        @SerialName("minNumbers")
        val minNumbers: Int?,

        @SerialName("minSpecial")
        val minSpecial: Int?,

        @SerialName("minNumberWords")
        val minNumberWords: Int?,

        @SerialName("capitalize")
        val capitalize: Boolean?,

        @SerialName("includeNumber")
        val includeNumber: Boolean?,
    ) : PolicyInformation() {
        @Suppress("UndocumentedPublicClass")
        companion object {
            const val TYPE_PASSWORD: String = "password"
            const val TYPE_PASSPHRASE: String = "passphrase"
        }
    }

    /**
     * Represents a policy enforcing rules on the user's add & edit send options.
     *
     * @property shouldDisableHideEmail Indicates whether the user should have the ability to hide
     * their email address from send recipients.
     */
    @Serializable
    data class SendOptions(
        @SerialName("disableHideEmail")
        val shouldDisableHideEmail: Boolean?,
    ) : PolicyInformation()

    /**
     * Represents a policy enforcing rules on the user's vault timeout settings.
     */
    @Serializable
    data class VaultTimeout(
        @SerialName("minutes")
        val minutes: Int?,

        @SerialName("action")
        val action: String?,
    ) : PolicyInformation()
}
