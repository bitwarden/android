package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for setting the password.
 *
 * @property organizationIdentifier The SSO organization identifier.
 * @property passwordHint The hint for the master password (nullable).
 * @property authenticationData The data to authenticate with a master password.
 * @property unlockData The data to unlock with a master password.
 * @property keys A [Keys] object containing public and private keys.
 */
@Serializable
data class SetPasswordRequestJson(
    @SerialName("orgIdentifier")
    val organizationIdentifier: String,

    @SerialName("masterPasswordHint")
    val passwordHint: String?,

    @SerialName("masterPasswordAuthentication")
    val authenticationData: MasterPasswordAuthenticationDataJson,

    @SerialName("masterPasswordUnlock")
    val unlockData: MasterPasswordUnlockDataJson,

    @SerialName("keys")
    val keys: Keys?,
) {
    constructor(
        organizationIdentifier: String,
        passwordHint: String?,
        kdf: KdfJson,
        salt: String,
        masterPasswordAuthenticationHash: String,
        masterKeyWrappedUserKey: String,
        keys: Keys?,
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
            containedKeyId = null,
        ),
        keys = keys,
    )

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
