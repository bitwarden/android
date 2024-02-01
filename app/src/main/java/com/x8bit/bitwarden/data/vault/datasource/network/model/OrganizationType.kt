package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a user's role in an organization.
 */
@Serializable(OrganizationTypeSerializer::class)
enum class OrganizationType {
    /**
     * The user is an owner of the organization.
     */
    @SerialName("0")
    OWNER,

    /**
     * The user is an admin in the organization.
     */
    @SerialName("1")
    ADMIN,

    /**
     * The user is an ordinary user in the organization.
     */
    @SerialName("2")
    USER,

    /**
     * The user is a manager in the organization.
     */
    @SerialName("3")
    MANAGER,

    /**
     * The user has a custom role in the organization.
     */
    @SerialName("4")
    CUSTOM,
}

@Keep
private class OrganizationTypeSerializer : BaseEnumeratedIntSerializer<OrganizationType>(
    OrganizationType.entries.toTypedArray(),
)
