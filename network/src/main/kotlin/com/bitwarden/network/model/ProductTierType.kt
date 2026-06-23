package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The subscription tier of an organization.
 */
@Serializable(with = ProductTierTypeSerializer::class)
enum class ProductTierType {
    /**
     * Free tier with limited features.
     */
    @SerialName("0")
    FREE,

    /**
     * Families plan for personal use.
     */
    @SerialName("1")
    FAMILIES,

    /**
     * Teams plan for small organizations.
     */
    @SerialName("2")
    TEAMS,

    /**
     * Enterprise plan with full features.
     */
    @SerialName("3")
    ENTERPRISE,

    /**
     * Starter tier for small teams.
     */
    @SerialName("4")
    TEAMS_STARTER,
}

@Keep
private class ProductTierTypeSerializer : BaseEnumeratedIntSerializer<ProductTierType>(
    className = "ProductTierType",
    values = ProductTierType.entries.toTypedArray(),
)
