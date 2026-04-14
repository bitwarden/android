package com.x8bit.bitwarden.data.auth.datasource.sdk

import com.bitwarden.auth.JitMasterPasswordRegistrationRequest
import com.bitwarden.auth.JitMasterPasswordRegistrationResponse
import com.bitwarden.auth.KeyConnectorRegistrationResult
import com.bitwarden.auth.TdeRegistrationRequest
import com.bitwarden.auth.TdeRegistrationResponse
import com.bitwarden.core.AuthRequestResponse
import com.bitwarden.core.FingerprintRequest
import com.bitwarden.core.KeyConnectorResponse
import com.bitwarden.core.MasterPasswordPolicyOptions
import com.bitwarden.core.RegisterKeyResponse
import com.bitwarden.core.RegisterTdeKeyResponse
import com.bitwarden.crypto.HashPurpose
import com.bitwarden.crypto.Kdf
import com.bitwarden.sdk.AuthClient
import com.x8bit.bitwarden.data.auth.datasource.sdk.model.PasswordStrength
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toPasswordStrengthOrNull
import com.x8bit.bitwarden.data.auth.datasource.sdk.util.toUByte
import com.x8bit.bitwarden.data.platform.datasource.sdk.BaseSdkSource
import com.x8bit.bitwarden.data.platform.manager.SdkClientManager

/**
 * Primary implementation of [AuthSdkSource] that serves as a convenience wrapper around a
 * [AuthClient].
 */
@Suppress("TooManyFunctions")
class AuthSdkSourceImpl(
    sdkClientManager: SdkClientManager,
) : BaseSdkSource(sdkClientManager = sdkClientManager),
    AuthSdkSource {

    override suspend fun postKeysForJitPasswordRegistration(
        userId: String,
        organizationId: String,
        organizationPublicKey: String,
        organizationSsoIdentifier: String,
        salt: String,
        masterPassword: String,
        masterPasswordHint: String?,
        shouldResetPasswordEnroll: Boolean,
    ): Result<JitMasterPasswordRegistrationResponse> = runCatchingWithLogs {
        getClient(userId = userId)
            .auth()
            .registration()
            .postKeysForJitPasswordRegistration(
                request = JitMasterPasswordRegistrationRequest(
                    orgId = organizationId,
                    orgPublicKey = organizationPublicKey,
                    userId = userId,
                    organizationSsoIdentifier = organizationSsoIdentifier,
                    salt = salt,
                    masterPassword = masterPassword,
                    masterPasswordHint = masterPasswordHint,
                    resetPasswordEnroll = shouldResetPasswordEnroll,
                ),
            )
    }

    override suspend fun postKeysForKeyConnectorRegistration(
        userId: String,
        accessToken: String,
        keyConnectorUrl: String,
        ssoOrganizationIdentifier: String,
    ): Result<KeyConnectorRegistrationResult> = runCatchingWithLogs {
        useClient(userId = userId, accessToken = accessToken) {
            auth().registration().postKeysForKeyConnectorRegistration(
                keyConnectorUrl = keyConnectorUrl,
                ssoOrgIdentifier = ssoOrganizationIdentifier,
            )
        }
    }

    override suspend fun postKeysForTdeRegistration(
        userId: String,
        organizationId: String,
        organizationPublicKey: String,
        deviceIdentifier: String,
        shouldTrustDevice: Boolean,
    ): Result<TdeRegistrationResponse> = runCatchingWithLogs {
        getClient(userId = userId)
            .auth()
            .registration()
            .postKeysForTdeRegistration(
                request = TdeRegistrationRequest(
                    orgId = organizationId,
                    orgPublicKey = organizationPublicKey,
                    userId = userId,
                    deviceIdentifier = deviceIdentifier,
                    trustDevice = shouldTrustDevice,
                ),
            )
    }

    override suspend fun getNewAuthRequest(
        email: String,
    ): Result<AuthRequestResponse> = runCatchingWithLogs {
        useClient { auth().newAuthRequest(email = email.lowercase()) }
    }

    override suspend fun getUserFingerprint(
        email: String,
        publicKey: String,
    ): Result<String> = runCatchingWithLogs {
        useClient {
            platform().fingerprint(
                req = FingerprintRequest(
                    fingerprintMaterial = email.lowercase(),
                    publicKey = publicKey,
                ),
            )
        }
    }

    override suspend fun hashPassword(
        email: String,
        password: String,
        kdf: Kdf,
        purpose: HashPurpose,
    ): Result<String> = runCatchingWithLogs {
        useClient {
            auth().hashPassword(
                email = email,
                password = password,
                kdfParams = kdf,
                purpose = purpose,
            )
        }
    }

    override suspend fun makeKeyConnectorKeys(): Result<KeyConnectorResponse> =
        runCatchingWithLogs {
            useClient { auth().makeKeyConnectorKeys() }
        }

    override suspend fun makeRegisterKeys(
        email: String,
        password: String,
        kdf: Kdf,
    ): Result<RegisterKeyResponse> = runCatchingWithLogs {
        useClient {
            auth().makeRegisterKeys(
                email = email,
                password = password,
                kdf = kdf,
            )
        }
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
        useClient {
            @Suppress("UnsafeCallOnNullableType")
            auth()
                .passwordStrength(
                    password = password,
                    email = email,
                    additionalInputs = additionalInputs,
                )
                .toPasswordStrengthOrNull()!!
        }
    }

    override suspend fun satisfiesPolicy(
        password: String,
        passwordStrength: PasswordStrength,
        policy: MasterPasswordPolicyOptions,
    ): Result<Boolean> = runCatchingWithLogs {
        useClient {
            auth().satisfiesPolicy(
                password = password,
                strength = passwordStrength.toUByte(),
                policy = policy,
            )
        }
    }
}
