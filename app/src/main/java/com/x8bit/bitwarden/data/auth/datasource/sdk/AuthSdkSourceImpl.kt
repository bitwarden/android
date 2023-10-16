package com.x8bit.bitwarden.data.auth.datasource.sdk

import com.bitwarden.core.Kdf
import com.bitwarden.core.MasterPasswordPolicyOptions
import com.bitwarden.core.RegisterKeyResponse
import com.bitwarden.sdk.ClientAuth
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toPasswordStrengthOrNull
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toUByte

/**
 * Primary implementation of [AuthSdkSource] that serves as a convenience wrapper around a
 * [ClientAuth].
 */
class AuthSdkSourceImpl(
    private val clientAuth: ClientAuth,
) : AuthSdkSource {

    override suspend fun hashPassword(
        email: String,
        password: String,
        kdf: Kdf,
    ): Result<String> = runCatching {
        clientAuth.hashPassword(
            email = email,
            password = password,
            kdfParams = kdf,
        )
    }

    override suspend fun makeRegisterKeys(
        email: String,
        password: String,
        kdf: Kdf,
    ): Result<RegisterKeyResponse> = runCatching {
        clientAuth.makeRegisterKeys(
            email = email,
            password = password,
            kdf = kdf,
        )
    }

    override suspend fun passwordStrength(
        email: String,
        password: String,
        additionalInputs: List<String>,
    ): Result<PasswordStrength> = runCatching {
        @Suppress("UnsafeCallOnNullableType")
        clientAuth
            .passwordStrength(
                password = password,
                email = email,
                additionalInputs = additionalInputs,
            )
            .toPasswordStrengthOrNull()!!
    }

    override suspend fun satisfiesPolicy(
        password: String,
        passwordStrength: PasswordStrength,
        policy: MasterPasswordPolicyOptions,
    ): Result<Boolean> = runCatching {
        clientAuth.satisfiesPolicy(
            password = password,
            strength = passwordStrength.toUByte(),
            policy = policy,
        )
    }
}
