package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import kotlinx.coroutines.flow.first

/**
 * Default implementation of [AuthenticatorBridgeRepository].
 */
class AuthenticatorBridgeRepositoryImpl(
    private val authRepository: AuthRepository,
    private val authDiskSource: AuthDiskSource,
    private val vaultRepository: VaultRepository,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
) : AuthenticatorBridgeRepository {

    override val authenticatorSyncSymmetricKey: ByteArray?
        get() {
            val doAnyAccountsHaveAuthenticatorSyncEnabled = authRepository
                .userStateFlow
                .value
                ?.accounts
                ?.any {
                    // Authenticator sync is enabled if any accounts have an authenticator
                    // sync key stored:
                    authDiskSource.getAuthenticatorSyncUnlockKey(it.userId) != null
                }
                ?: false
            return if (doAnyAccountsHaveAuthenticatorSyncEnabled) {
                authDiskSource.authenticatorSyncSymmetricKey
            } else {
                null
            }
        }

    @Suppress("LongMethod")
    override suspend fun getSharedAccounts(): SharedAccountData {
        val allAccounts = authRepository.userStateFlow.value?.accounts ?: emptyList()

        return allAccounts
            .mapNotNull { account ->
                val userId = account.userId

                // Grab the user's authenticator sync unlock key. If it is null,
                // the user has not enabled authenticator sync.
                val decryptedUserKey = authDiskSource.getAuthenticatorSyncUnlockKey(userId)
                    ?: return@mapNotNull null

                // Wait for any unlocking actions to finish:
                vaultRepository.vaultUnlockDataStateFlow.first {
                    it.statusFor(userId) != VaultUnlockData.Status.UNLOCKING
                }

                // Unlock vault if necessary:
                val isVaultAlreadyUnlocked = vaultRepository.isVaultUnlocked(userId = userId)
                if (!isVaultAlreadyUnlocked) {
                    val unlockResult = vaultRepository
                        .unlockVaultWithDecryptedUserKey(
                            userId = userId,
                            decryptedUserKey = decryptedUserKey,
                        )

                    when (unlockResult) {
                        is VaultUnlockResult.AuthenticationError,
                        VaultUnlockResult.GenericError,
                        VaultUnlockResult.InvalidStateError,
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
                            return@mapNotNull null
                        }
                        // Proceed
                        VaultUnlockResult.Success -> Unit
                    }
                }

                // Vault is unlocked, query vault disk source for totp logins:
                val totpUris = vaultDiskSource
                    .getCiphers(userId)
                    .first()
                    // Filter out any ciphers without a totp item and also deleted ciphers:
                    .filter { it.login?.totp != null && it.deletedDate == null }
                    .mapNotNull {
                        // Decrypt each cipher and take just totp codes:
                        vaultSdkSource
                            .decryptCipher(
                                userId = userId,
                                cipher = it.toEncryptedSdkCipher(),
                            )
                            .getOrNull()
                            ?.login
                            ?.totp
                    }

                // Lock the user's vault if we unlocked it for this operation:
                if (!isVaultAlreadyUnlocked) {
                    vaultRepository.lockVault(userId)
                }

                SharedAccountData.Account(
                    userId = account.userId,
                    name = account.name,
                    email = account.email,
                    environmentLabel = account.environment.label,
                    totpUris = totpUris,
                )
            }
            .let {
                SharedAccountData(it)
            }
    }
}
