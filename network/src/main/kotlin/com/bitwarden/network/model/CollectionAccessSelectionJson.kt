package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a group or user access selection for a collection.
 *
 * This model is used in both request and response contexts for
 * collection access management.
 *
 * @property id The ID of the group or user.
 * @property readOnly Whether the group or user has read-only access.
 * @property hidePasswords Whether passwords are hidden from the group or user.
 * @property manage Whether the group or user can manage the collection.
 */
@Serializable
data class CollectionAccessSelectionJson(
    @SerialName("id")
    val id: String,

    @SerialName("readOnly")
    val readOnly: Boolean,

    @SerialName("hidePasswords")
    val hidePasswords: Boolean,

    @SerialName("manage")
    val manage: Boolean,
)
