package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for register.
 *
 * @property email the email to be registered.
 * @property emailVerificationToken token used to finish the registration process.
 * @property masterPasswordHint the hint for the master password (nullable).
 * @property userAsymmetricKeys a [KeysJson] object containing public and private keys.
 * @property authenticationData The data to authenticate with a master password.
 * @property unlockData The data to unlock with a master password.
 */
@Serializable
data class RegisterFinishRequestJson(
    @SerialName("email")
    val email: String,

    @SerialName("emailVerificationToken")
    val emailVerificationToken: String,

    @SerialName("masterPasswordHint")
    val masterPasswordHint: String?,

    @SerialName("userAsymmetricKeys")
    val userAsymmetricKeys: KeysJson,

    @SerialName("MasterPasswordAuthentication")
    val authenticationData: MasterPasswordAuthenticationDataJson,

    @SerialName("MasterPasswordUnlock")
    val unlockData: MasterPasswordUnlockDataJson,
) {
    constructor(
        email: String,
        emailVerificationToken: String,
        masterPasswordHint: String?,
        userAsymmetricKeys: KeysJson,
        kdf: KdfJson,
        salt: String,
        masterPasswordAuthenticationHash: String,
        masterKeyWrappedUserKey: String,
    ) : this(
        email = email,
        emailVerificationToken = emailVerificationToken,
        masterPasswordHint = masterPasswordHint,
        userAsymmetricKeys = userAsymmetricKeys,
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
    )
}
