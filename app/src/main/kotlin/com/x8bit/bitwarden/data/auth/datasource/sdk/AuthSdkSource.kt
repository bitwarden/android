package com.x8bit.bitwarden.data.auth.datasource.sdk

import com.bitwarden.core.AuthRequestResponse
import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.core.MasterPasswordPolicyOptions
import com.bitwarden.core.RegisterKeyResponse
import com.bitwarden.core.RegisterTdeKeyResponse
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength

/**
 * Source of authentication information and functionality from the Bitwarden SDK.
 */
interface AuthSdkSource {
    /**
     * Gets the data needed to create a new auth request.
     */
    suspend fun getNewAuthRequest(
        email: String,
    ): Result<AuthRequestResponse>

    /**
     * Gets the fingerprint phrase for this [email] and [publicKey].
     */
    suspend fun getUserFingerprint(
        email: String,
        publicKey: String,
    ): Result<String>

    /**
     * Creates a hashed password provided the given [email], [password], [kdf], and [purpose].
     */
    suspend fun hashPassword(
        email: String,
        password: String,
        kdf: Kdf,
        purpose: HashPurpose,
    ): Result<String>

    /**
     * Creates a set of encryption key information for use with a key connector.
     */
    suspend fun makeKeyConnectorKeys(): Result<KeyConnectorResponse>

    /**
     * Creates a set of encryption key information for registration.
     */
    suspend fun makeRegisterKeys(
        email: String,
        password: String,
        kdf: Kdf,
    ): Result<RegisterKeyResponse>

    /**
     * Creates a set of encryption key information for registration of a trusted device and unlocks
     * the vault for the user.
     */
    suspend fun makeRegisterTdeKeysAndUnlockVault(
        userId: String,
        email: String,
        orgPublicKey: String,
        rememberDevice: Boolean,
    ): Result<RegisterTdeKeyResponse>

    /**
     * Checks the password strength for the given [email] and [password] combination, along with
     * some [additionalInputs].
     */
    suspend fun passwordStrength(
        email: String,
        password: String,
        additionalInputs: List<String> = emptyList(),
    ): Result<PasswordStrength>

    /**
     * Checks that the given [password] with the given [passwordStrength] satisfies the given
     * [policy]. Returns `true` if so and `false` otherwise.
     */
    suspend fun satisfiesPolicy(
        password: String,
        passwordStrength: PasswordStrength,
        policy: MasterPasswordPolicyOptions,
    ): Result<Boolean>
}
