package com.x8bit.bitwarden.data.auth.datasource.network.model

import com.x8bit.bitwarden.data.auth.datasource.network.model.RegisterRequestJson.Keys
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for resetting the password.
 *
 * @property kdfType The KDF type.
 * @property kdfIterations The number of iterations when calculating a user's password.
 * @property kdfMemory The amount of memory to use when calculating a password hash (MB).
 * @property kdfParallelism The number of threads to use when calculating a password hash.
 * @param key The user key for the request (encrypted).
 * @param keys A [Keys] object containing public and private keys.
 * @param organizationIdentifier The SSO organization identifier.
 * @param passwordHash The hash of the user's new password.
 * @param passwordHint The hint for the master password (nullable).
 */
@Serializable
data class SetPasswordRequestJson(
    @SerialName("kdf")
    val kdfType: KdfTypeJson? = null,

    @SerialName("kdfIterations")
    val kdfIterations: Int? = null,

    @SerialName("kdfMemory")
    val kdfMemory: Int? = null,

    @SerialName("kdfParallelism")
    val kdfParallelism: Int? = null,

    @SerialName("key")
    val key: String,

    @SerialName("keys")
    val keys: Keys?,

    @SerialName("orgIdentifier")
    val organizationIdentifier: String,

    @SerialName("masterPasswordHash")
    val passwordHash: String?,

    @SerialName("masterPasswordHint")
    val passwordHint: String?,
)
