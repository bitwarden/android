package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for resetting the password.
 *
 * @property currentPasswordHash The hash of the user's current password.
 * @property passwordHint The hint for the master password (nullable).
 * @property authenticationData The data to authenticate with a master password.
 * @property unlockData The data to unlock with a master password.
 */
@Serializable
data class ResetPasswordRequestJson(
    @SerialName("masterPasswordHash")
    val currentPasswordHash: String?,

    @SerialName("masterPasswordHint")
    val passwordHint: String?,

    @SerialName("authenticationData")
    val authenticationData: MasterPasswordAuthenticationDataJson,

    @SerialName("unlockData")
    val unlockData: MasterPasswordUnlockDataJson,
) {
    constructor(
        currentPasswordHash: String?,
        passwordHint: String?,
        kdf: KdfJson,
        salt: String,
        masterPasswordAuthenticationHash: String,
        masterKeyWrappedUserKey: String,
    ) : this(
        currentPasswordHash = currentPasswordHash,
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
