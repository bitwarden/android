package com.x8bit.bitwarden.data.vault.datasource.network.model

import androidx.annotation.Keep
import com.x8bit.bitwarden.data.platform.datasource.network.serializer.BaseEnumeratedIntSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents how a URI should be matched for autofill to occur.
 */
@Serializable(UriMatchTypeSerializer::class)
enum class UriMatchTypeJson {
    /**
     * Matching of the URI is based on the domain.
     */
    @SerialName("0")
    DOMAIN,

    /**
     * Matching of the URI is based on the host.
     */
    @SerialName("1")
    HOST,

    /**
     * Matching of the URI is based the start of resource.
     */
    @SerialName("2")
    STARTS_WITH,

    /**
     * Matching of the URI requires an exact match.
     */
    @SerialName("3")
    EXACT,

    /**
     * Requires users to authenticate with SSO.
     */
    @SerialName("4")
    REGULAR_EXPRESSION,

    /**
     * The URI should never be autofilled.
     */
    @SerialName("5")
    NEVER,
}

@Keep
private class UriMatchTypeSerializer :
    BaseEnumeratedIntSerializer<UriMatchTypeJson>(UriMatchTypeJson.entries.toTypedArray())
