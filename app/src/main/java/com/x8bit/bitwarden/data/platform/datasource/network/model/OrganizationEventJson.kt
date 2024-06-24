package com.x8bit.bitwarden.data.platform.datasource.network.model

import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEventType
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
