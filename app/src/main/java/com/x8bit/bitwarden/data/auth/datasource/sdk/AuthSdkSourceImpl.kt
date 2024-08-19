package com.x8bit.bitwarden.data.auth.datasource.sdk

import com.bitwarden.core.AuthRequestResponse
import com.bitwarden.core.FingerprintRequest
import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.core.MasterPasswordPolicyOptions
import com.bitwarden.core.RegisterKeyResponse
import com.bitwarden.core.RegisterTdeKeyResponse
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.bitwarden.sdk.ClientAuth
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toPasswordStrengthOrNull
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toUByte
import com.x8bit.bitwarden.data.platform.datasource.sdk.BaseSdkSource
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager

/**
 * Primary implementation of [AuthSdkSource] that serves as a convenience wrapper around a
 * [ClientAuth].
 */
class AuthSdkSourceImpl(
    sdkClientManager: SdkClientManager,
) : BaseSdkSource(sdkClientManager = sdkClientManager),
    AuthSdkSource {

    override suspend fun getNewAuthRequest(
        email: String,
    ): Result<AuthRequestResponse> = runCatchingWithLogs {
        getClient()
            .auth()
            .newAuthRequest(
                email = email,
            )
    }

    override suspend fun getUserFingerprint(
        email: String,
        publicKey: String,
    ): Result<String> = runCatchingWithLogs {
        getClient()
            .platform()
            .fingerprint(
                req = FingerprintRequest(
                    fingerprintMaterial = email,
                    publicKey = publicKey,
                ),
            )
    }

    override suspend fun hashPassword(
        email: String,
        password: String,
        kdf: Kdf,
        purpose: HashPurpose,
    ): Result<String> = runCatchingWithLogs {
        getClient()
            .auth()
            .hashPassword(
                email = email,
                password = password,
                kdfParams = kdf,
                purpose = purpose,
            )
    }

    override suspend fun makeKeyConnectorKeys(): Result<KeyConnectorResponse> =
        runCatchingWithLogs {
            getClient()
                .auth()
                .makeKeyConnectorKeys()
        }

    override suspend fun makeRegisterKeys(
        email: String,
        password: String,
        kdf: Kdf,
    ): Result<RegisterKeyResponse> = runCatchingWithLogs {
        getClient()
            .auth()
            .makeRegisterKeys(
                email = email,
                password = password,
                kdf = kdf,
            )
    }

    override suspend fun makeRegisterTdeKeysAndUnlockVault(
        userId: String,
        email: String,
        orgPublicKey: String,
        rememberDevice: Boolean,
    ): Result<RegisterTdeKeyResponse> = runCatchingWithLogs {
        getClient(userId = userId)
            .auth()
            .makeRegisterTdeKeys(
                email = email,
                orgPublicKey = orgPublicKey,
                rememberDevice = rememberDevice,
            )
    }

    override suspend fun passwordStrength(
        email: String,
        password: String,
        additionalInputs: List<String>,
    ): Result<PasswordStrength> = runCatchingWithLogs {
        @Suppress("UnsafeCallOnNullableType")
        getClient()
            .auth()
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
    ): Result<Boolean> = runCatchingWithLogs {
        getClient()
            .auth()
            .satisfiesPolicy(
                password = password,
                strength = passwordStrength.toUByte(),
                policy = policy,
            )
    }
}
