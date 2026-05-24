package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
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
     * Activate the autofill in the browser extension. Currently unused in mobile.
     */
    @SerialName("11")
    ACTIVATE_AUTOFILL,

    /**
     * Automatically logs members into apps using single sign-on.
     */
    @SerialName("12")
    AUTOMATIC_APP_LOG_IN,

    /**
     * Removes members' access to the free Bitwarden Families sponsorship benefit.
     */
    @SerialName("13")
    FREE_FAMILIES_SPONSORSHIP_POLICY,

    /**
     * Hides the setting to "Unlock with Pin".
     */
    @SerialName("14")
    REMOVE_UNLOCK_WITH_PIN,

    /**
     * Restricts the types of items that can be shown in the vault.
     */
    @SerialName("15")
    RESTRICT_ITEM_TYPES,

    /**
     * Sets the default URI match detection strategy for autofill.
     */
    @SerialName("16")
    URI_MATCH_DEFAULTS,

    /**
     * Sets the default behavior for the autotype feature.
     */
    @SerialName("17")
    AUTOTYPE_DEFAULT_SETTING,

    /**
     * Automatically confirms invited users into the organization.
     */
    @SerialName("18")
    AUTOMATIC_USER_CONFIRMATION,

    /**
     * Blocks account creation for users with email addresses on claimed domains.
     */
    @SerialName("19")
    BLOCK_CLAIMED_DOMAIN_ACCOUNT_CREATION,

    /**
     * Displays an organization-configured banner message to members in their vault.
     */
    @SerialName("20")
    ORGANIZATION_USER_NOTIFICATION,

    /**
     * Configures Send-related behavior: disabling Sends, email visibility, access controls,
     * Send types, and deletion.
     *
     * Supersedes [`DisableSend`](Self::DisableSend) and [`SendOptions`](Self::SendOptions) when
     * the `pm-31885-send-controls` feature flag is active on the server.
     */
    @SerialName("21")
    SEND_CONTROLS,
}

@Keep
private class PolicyTypeSerializer : BaseEnumeratedIntSerializer<PolicyTypeJson>(
    className = "PolicyTypeJson",
    values = PolicyTypeJson.entries.toTypedArray(),
)
