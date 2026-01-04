package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.data.repository.util.toEnvironmentUrlsOrDefault
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.repository.util.toSdkParams
import com.x8bit.bitwarden.data.platform.repository.util.sanitizeTotpUri
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.ScopedVaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.createWrappedAccountCryptographicState
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toVaultUnlockResult

/**
 * Default implementation of [AuthenticatorBridgeRepository].
 */
class AuthenticatorBridgeRepositoryImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val scopedVaultSdkSource: ScopedVaultSdkSource,
) : AuthenticatorBridgeRepository {

    override val authenticatorSyncSymmetricKey: ByteArray?
        get() {
            val doAnyAccountsHaveAuthenticatorSyncEnabled = authDiskSource
                .userState
                ?.accounts
                ?.keys
                ?.any { userId ->
                    // Authenticator sync is enabled if any accounts have an authenticator
                    // sync key stored:
                    authDiskSource.getAuthenticatorSyncUnlockKey(userId = userId) != null
                }
                ?: false
            return if (doAnyAccountsHaveAuthenticatorSyncEnabled) {
                authDiskSource.authenticatorSyncSymmetricKey
            } else {
                null
            }
        }

    override suspend fun getSharedAccounts(): SharedAccountData {
        return authDiskSource
            .userState
            ?.accounts
            .orEmpty()
            .mapNotNull { (userId, account) ->
                // Grab the user's authenticator sync unlock key. If it is null,
                // the user has not enabled authenticator sync and we skip the account.
                val decryptedUserKey = authDiskSource.getAuthenticatorSyncUnlockKey(userId)
                    ?: return@mapNotNull null
                val vaultUnlockResult = unlockClient(
                    userId = userId,
                    account = account,
                    decryptedUserKey = decryptedUserKey,
                )
                when (vaultUnlockResult) {
                    is VaultUnlockResult.AuthenticationError,
                    is VaultUnlockResult.BiometricDecodingError,
                    is VaultUnlockResult.GenericError,
                    is VaultUnlockResult.InvalidStateError,
                        -> {
                        // Not being able to unlock the user's vault with the
                        // decrypted unlock key is an unexpected case, but if it does
                        // happen we omit the account from list of shared accounts
                        // and remove that user's authenticator sync unlock key.
                        // This gives the user a way to potentially re-enable syncing
                        // (going to Account Security and re-enabling the toggle)
                        authDiskSource.storeAuthenticatorSyncUnlockKey(
                            userId = userId,
                            authenticatorSyncUnlockKey = null,
                        )
                        // Destroy our stand-alone instance of the vault.
                        scopedVaultSdkSource.clearCrypto(userId = userId)
                        return@mapNotNull null
                    }
                    // Proceed
                    VaultUnlockResult.Success -> Unit
                }

                // Vault is unlocked, query vault disk source for totp logins:
                val totpUris = vaultDiskSource
                    .getTotpCiphers(userId = userId)
                    // Filter out any deleted ciphers.
                    .filter { it.deletedDate == null }
                    .mapNotNull {
                        scopedVaultSdkSource
                            .decryptCipher(userId = userId, cipher = it.toEncryptedSdkCipher())
                            .getOrNull()
                            ?.let { decryptedCipher ->
                                val rawTotp = decryptedCipher.login?.totp
                                val cipherName = decryptedCipher.name
                                val username = decryptedCipher.login?.username
                                rawTotp.sanitizeTotpUri(issuer = cipherName, username = username)
                            }
                    }

                // Lock and destroy our stand-alone instance of the vault:
                scopedVaultSdkSource.clearCrypto(userId = userId)

                SharedAccountData.Account(
                    userId = userId,
                    name = account.profile.name,
                    email = account.profile.email,
                    environmentLabel = account
                        .settings
                        .environmentUrlData
                        .toEnvironmentUrlsOrDefault()
                        .label,
                    totpUris = totpUris,
                )
            }
            .let(::SharedAccountData)
    }

    private suspend fun unlockClient(
        userId: String,
        account: AccountJson,
        decryptedUserKey: String,
    ): VaultUnlockResult {
        val accountKeys = authDiskSource.getAccountKeys(userId = userId)
        val privateKey = accountKeys?.publicKeyEncryptionKeyPair?.wrappedPrivateKey
            ?: authDiskSource.getPrivateKey(userId = userId)
            ?: return VaultUnlockResult.InvalidStateError(
                MissingPropertyException("Private key"),
            )
        val securityState = authDiskSource
            .getAccountKeys(userId = userId)
            ?.securityState
            ?.securityState
        val signingKey = accountKeys?.signatureKeyPair?.wrappedSigningKey
        val signedPublicKey = accountKeys?.publicKeyEncryptionKeyPair?.signedPublicKey

        return scopedVaultSdkSource
            .initializeCrypto(
                userId = userId,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = privateKey,
                        securityState = securityState,
                        signingKey = signingKey,
                        signedPublicKey = signedPublicKey,
                    ),
                    userId = userId,
                    kdfParams = account.profile.toSdkParams(),
                    email = account.profile.email,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = decryptedUserKey,
                    ),
                ),
            )
            .flatMap { result ->
                // Initialize the SDK for organizations if necessary
                val organizationKeys = authDiskSource.getOrganizationKeys(userId = userId)
                if (organizationKeys != null && result is InitializeCryptoResult.Success) {
                    scopedVaultSdkSource.initializeOrganizationCrypto(
                        userId = userId,
                        request = InitOrgCryptoRequest(organizationKeys = organizationKeys),
                    )
                } else {
                    result.asSuccess()
                }
            }
            .fold(
                onFailure = { VaultUnlockResult.GenericError(error = it) },
                onSuccess = { it.toVaultUnlockResult() },
            )
    }
}
