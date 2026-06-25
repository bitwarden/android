package com.bitwarden.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for resetting the password.
 */
@Serializable
sealed class ResetPasswordRequestJson {
    abstract val currentPasswordHash: String?

    /**
     * Request body for resetting the password using the V1 flow.
     *
     * @param currentPasswordHash The hash of the user's current password.
     * @param newPasswordHash The hash of the user's new password.
     * @param passwordHint The hint for the master password (nullable).
     * @param key The user key for the request (encrypted).
     */
    @Serializable
    data class V1(
        @SerialName("masterPasswordHash")
        override val currentPasswordHash: String?,

        @SerialName("newMasterPasswordHash")
        val newPasswordHash: String,

        @SerialName("masterPasswordHint")
        val passwordHint: String?,

        @SerialName("key")
        val key: String,
    ) : ResetPasswordRequestJson()

    /**
     * Request body for resetting the password using the V2 flow.
     *
     * @property currentPasswordHash The hash of the user's current password.
     * @property passwordHint The hint for the master password (nullable).
     * @property authenticationData The data to authenticate with a master password.
     * @property unlockData The data to unlock with a master password.
     */
    @Serializable
    data class V2(
        @SerialName("masterPasswordHash")
        override val currentPasswordHash: String?,

        @SerialName("masterPasswordHint")
        val passwordHint: String?,

        @SerialName("authenticationData")
        val authenticationData: MasterPasswordAuthenticationDataJson,

        @SerialName("unlockData")
        val unlockData: MasterPasswordUnlockDataJson,
    ) : ResetPasswordRequestJson() {
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
}
