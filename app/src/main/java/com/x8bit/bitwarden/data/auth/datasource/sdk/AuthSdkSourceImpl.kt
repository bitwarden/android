package com.x8bit.bitwarden.data.auth.datasource.sdk

import com.bitwarden.core.AuthRequestResponse
import com.bitwarden.core.FingerprintRequest
import com.bitwarden.core.MasterPasswordPolicyOptions
import com.bitwarden.core.RegisterKeyResponse
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.bitwarden.crypto.TrustDeviceResponse
import com.bitwarden.sdk.ClientAuth
import com.bitwarden.sdk.ClientPlatform
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toPasswordStrengthOrNull
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toUByte
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.sdk.BitwardenFeatureFlagManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Primary implementation of [AuthSdkSource] that serves as a convenience wrapper around a
 * [ClientAuth].
 */
class AuthSdkSourceImpl(
    private val clientAuth: ClientAuth,
    private val clientPlatform: ClientPlatform,
    dispatcherManager: DispatcherManager,
    featureFlagManager: BitwardenFeatureFlagManager,
) : AuthSdkSource {

    private val ioScope = CoroutineScope(dispatcherManager.io)

    init {
        ioScope.launch {
            clientPlatform.loadFlags(featureFlagManager.featureFlags)
        }
    }

    override suspend fun getTrustDevice(): Result<TrustDeviceResponse> = runCatching {
        clientAuth.trustDevice()
    }

    override suspend fun getNewAuthRequest(
        email: String,
    ): Result<AuthRequestResponse> = runCatching {
        clientAuth.newAuthRequest(
            email = email,
        )
    }

    override suspend fun getUserFingerprint(
        email: String,
        publicKey: String,
    ): Result<String> = runCatching {
        clientPlatform.fingerprint(
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
    ): Result<String> = runCatching {
        clientAuth.hashPassword(
            email = email,
            password = password,
            kdfParams = kdf,
            purpose = purpose,
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
