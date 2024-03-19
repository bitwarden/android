package com.x8bit.bitwarden.data.auth.datasource.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object containing keys for this organization.
 *
 * @property privateKey The private key for this organization.
 * @property publicKey The public key for this organization.
 */
@Serializable
data class OrganizationKeysResponseJson(
    @SerialName("privateKey") val privateKey: String?,
    @SerialName("publicKey") val publicKey: String,
)
