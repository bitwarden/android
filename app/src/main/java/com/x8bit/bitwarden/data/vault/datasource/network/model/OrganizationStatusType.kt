package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a user's status in an organization.
 */
@Serializable(OrganizationStatusTypeSerializer::class)
enum class OrganizationStatusType {
    /**
     * The user has been invited to the organization.
     */
    @SerialName("0")
    INVITED,

    /**
     * The user has accepted the invite to the organization.
     */
    @SerialName("1")
    ACCEPTED,

    /**
     * The user has been confirmed in the organization.
     */
    @SerialName("2")
    CONFIRMED,
}

@Keep
private class OrganizationStatusTypeSerializer :
    BaseEnumeratedIntSerializer<OrganizationStatusType>(
        OrganizationStatusType.entries.toTypedArray(),
    )
