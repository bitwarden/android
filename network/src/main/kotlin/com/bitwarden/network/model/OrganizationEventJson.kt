package com.bitwarden.network.model

import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.ZonedDateTime

/**
 * Represents an individual organization event including the type and time.
 */
@Serializable
data class OrganizationEventJson(
    @SerialName("type") val type: OrganizationEventType,
    @SerialName("cipherId") val cipherId: String?,
    @SerialName("date") @Contextual val date: ZonedDateTime,
)
