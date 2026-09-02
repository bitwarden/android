package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for setting the password.
 *
 * @property organizationIdentifier The SSO organization identifier.
 * @property passwordHint The hint for the master password (nullable).
 * @property kdfType The KDF type.
 * @property kdfIterations The number of iterations when calculating a user's password.
 * @property kdfMemory The amount of memory to use when calculating a password hash (MB).
 * @property kdfParallelism The number of threads to use when calculating a password hash.
 * @property key The user key for the request (encrypted).
 * @property passwordHash The hash of the user's new password.
 * @property keys A [KeysJson] object containing public and private keys.
 */
@Serializable
data class SetPasswordRequestJson(
    @SerialName("orgIdentifier")
    val organizationIdentifier: String,

    @SerialName("masterPasswordHint")
    val passwordHint: String?,

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

    @SerialName("masterPasswordHash")
    val passwordHash: String?,

    @SerialName("keys")
    val keys: KeysJson?,
) {
    constructor(
        organizationIdentifier: String,
        passwordHint: String?,
        kdf: KdfJson,
        salt: String,
        masterPasswordAuthenticationHash: String,
        masterKeyWrappedUserKey: String,
        keys: KeysJson?,
    ) : this(
        organizationIdentifier = organizationIdentifier,
        passwordHint = passwordHint,
        kdfType = kdf.kdfType,
        kdfIterations = kdf.iterations,
        kdfMemory = kdf.memory,
        kdfParallelism = kdf.parallelism,
        key = masterKeyWrappedUserKey,
        passwordHash = masterPasswordAuthenticationHash,
        keys = keys,
    )
}
