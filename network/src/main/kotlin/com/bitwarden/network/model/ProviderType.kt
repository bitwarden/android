package com.bitwarden.network.model

import androidx.annotation.Keep
import com.bitwarden.core.data.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The type of provider.
 */
@Serializable(with = ProviderTypeSerializer::class)
enum class ProviderType {
    /**
     * Managed Service Provider - sells and manages its clients' Bitwarden organizations.
     */
    @SerialName("0")
    MSP,

    /**
     * Reseller partner - sells Bitwarden to its clients but does not have any administrative
     * access.
     */
    @SerialName("1")
    RESELLER,

    /**
     * Business unit provider - used to manage multiple organizations which form part of a single
     * large enterprise.
     */
    @SerialName("2")
    BUSINESS_UNIT,
}

@Keep
private class ProviderTypeSerializer : BaseEnumeratedIntSerializer<ProviderType>(
    className = "ProviderType",
    values = ProviderType.entries.toTypedArray(),
)
