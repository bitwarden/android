package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Response object returned when creating account keys.
 *
 * @property key The user key (nullable).
 * @property publicKey The public key for the account.
 * @property privateKey The encrypted private key for the account.
 * @property accountKeys The account keys containing encryption key pairs and security state.
 */
@Serializable
data class CreateAccountKeysResponseJson(
    @SerialName("key")
    val key: String?,

    @SerialName("publicKey")
    val publicKey: String?,

    @SerialName("privateKey")
    val privateKey: String?,

    @SerialName("accountKeys")
    val accountKeys: AccountKeysJson?,
)
