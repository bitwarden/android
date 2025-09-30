package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents the type of collection assigned to user(s) or group(s).
 */
@Serializable(CollectionTypeSerializer::class)
enum class CollectionTypeJson {
    /**
     * Default collection type. Can be assigned by an organization to user(s) or group(s).
     */
    @SerialName("0")
    SHARED_COLLECTION,

    /**
     * Default collection assigned to a user for an organization that has
     * OrganizationDataOwnership (formerly PersonalOwnership) policy enabled.
     */
    @SerialName("1")
    DEFAULT_USER_COLLECTION,
}

@Keep
private class CollectionTypeSerializer : BaseEnumeratedIntSerializer<CollectionTypeJson>(
    className = "CollectionTypeJson",
    values = CollectionTypeJson.entries.toTypedArray(),
)
