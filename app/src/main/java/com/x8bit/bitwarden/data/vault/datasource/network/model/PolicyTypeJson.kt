package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents different types of policies.
 */
@Serializable(PolicyTypeSerializer::class)
enum class PolicyTypeJson {
    /**
     * Requires users to have 2FA enabled.
     */
    @SerialName("0")
    TWO_FACTOR_AUTHENTICATION,

    /**
     * Sets minimum requirements for master password complexity.
     */
    @SerialName("1")
    MASTER_PASSWORD,

    /**
     * Sets minimum requirements/default type for generated passwords/passphrases.
     */
    @SerialName("2")
    PASSWORD_GENERATOR,

    /**
     * Allows users to only be apart of one organization.
     */
    @SerialName("3")
    ONLY_ORG,

    /**
     * Requires users to authenticate with SSO.
     */
    @SerialName("4")
    REQUIRE_SSO,

    /**
     * Disables personal vault ownership for adding/cloning items.
     */
    @SerialName("5")
    PERSONAL_OWNERSHIP,

    /**
     * Disables the ability to create and edit Sends.
     */
    @SerialName("6")
    DISABLE_SEND,

    /**
     * Sets restrictions or defaults for Bitwarden Sends.
     */
    @SerialName("7")
    SEND_OPTIONS,

    /**
     * Allows orgs to use reset password : also can enable auto-enrollment during invite flow.
     */
    @SerialName("8")
    RESET_PASSWORD,

    /**
     * Sets the maximum allowed vault timeout.
     */
    @SerialName("9")
    MAXIMUM_VAULT_TIMEOUT,

    /**
     * Disable personal vault export.
     */
    @SerialName("10")
    DISABLE_PERSONAL_VAULT_EXPORT,

    /**
     * Activate the auto-fill in the browser extension. Currently unused in mobile.
     */
    @SerialName("11")
    ACTIVATE_AUTOFILL,

    /**
     * Represents an unknown policy type.
     *
     * This is used for forward compatibility to handle new policy types that the client doesn't yet
     * understand.
     */
    @SerialName("-1")
    UNKNOWN,
}

@Keep
private class PolicyTypeSerializer : BaseEnumeratedIntSerializer<PolicyTypeJson>(
    values = PolicyTypeJson.entries.toTypedArray(),
    default = PolicyTypeJson.UNKNOWN,
)
