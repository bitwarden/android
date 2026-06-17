package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for setting the password.
 */
@Serializable
sealed class SetPasswordRequestJson {
    /**
     * Request body for setting the password in a v2 flow.
     *
     * @property organizationIdentifier The SSO organization identifier.
     * @property passwordHint The hint for the master password (nullable).
     * @property authenticationData The data to authenticate with a master password.
     * @property unlockData The data to unlock with a master password.
     */
    @Serializable
    data class V2(
        @SerialName("orgIdentifier")
        val organizationIdentifier: String,

        @SerialName("masterPasswordHint")
        val passwordHint: String?,

        @SerialName("masterPasswordAuthentication")
        val authenticationData: MasterPasswordAuthenticationDataJson,

        @SerialName("masterPasswordUnlock")
        val unlockData: MasterPasswordUnlockDataJson,
    ) : SetPasswordRequestJson() {
        constructor(
            organizationIdentifier: String,
            passwordHint: String?,
            kdf: KdfJson,
            salt: String,
            masterPasswordAuthenticationHash: String,
            masterKeyWrappedUserKey: String,
        ) : this(
            organizationIdentifier = organizationIdentifier,
            passwordHint = passwordHint,
            authenticationData = MasterPasswordAuthenticationDataJson(
                kdf = kdf,
                salt = salt,
                masterPasswordAuthenticationHash = masterPasswordAuthenticationHash,
            ),
            unlockData = MasterPasswordUnlockDataJson(
                kdf = kdf,
                salt = salt,
                masterKeyWrappedUserKey = masterKeyWrappedUserKey,
            ),
        )
    }

    /**
     * Request body for setting the password in a v1 flow.
     *
     * @property kdfType The KDF type.
     * @property kdfIterations The number of iterations when calculating a user's password.
     * @property kdfMemory The amount of memory to use when calculating a password hash (MB).
     * @property kdfParallelism The number of threads to use when calculating a password hash.
     * @property key The user key for the request (encrypted).
     * @property keys A [Keys] object containing public and private keys.
     * @property organizationIdentifier The SSO organization identifier.
     * @property passwordHash The hash of the user's new password.
     * @property passwordHint The hint for the master password (nullable).
     */
    @Serializable
    data class V1(
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
    ) : SetPasswordRequestJson() {
        /**
         * A keys object containing public and private keys.
         *
         * @param publicKey the public key (encrypted).
         * @param encryptedPrivateKey the private key (encrypted).
         */
        @Serializable
        data class Keys(
            @SerialName("publicKey")
            val publicKey: String,

            @SerialName("encryptedPrivateKey")
            val encryptedPrivateKey: String,
        )
    }
}
