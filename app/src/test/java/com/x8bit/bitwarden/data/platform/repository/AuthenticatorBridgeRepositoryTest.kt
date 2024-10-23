package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.bitwarden.authenticatorbridge.util.toSymmetricEncryptionKeyData
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class AuthenticatorBridgeRepositoryTest {

    private val authRepository = mockk<AuthRepository>()
    private val vaultSdkSource = mockk<VaultSdkSource>()
    private val vaultDiskSource = mockk<VaultDiskSource>()
    private val vaultRepository = mockk<VaultRepository>()
    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val authenticatorBridgeRepository = AuthenticatorBridgeRepositoryImpl(
        authRepository = authRepository,
        authDiskSource = fakeAuthDiskSource,
        vaultRepository = vaultRepository,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
    )

    @BeforeEach
    fun setup() {
        // Because there is so much setup for the happy path, we set that all up here and
        // then adjust accordingly in each test. The "base" setup here is that
        // there are two users, USER_1, which we will often manipulate in tests,
        // and USER_2, which will remain usually not manipulated to demonstrate that a single
        // account failing shouldn't impact other accounts.

        // Store symmetric encryption key on disk:
        fakeAuthDiskSource.authenticatorSyncSymmetricKey =
            SYMMETRIC_KEY.symmetricEncryptionKey.byteArray

        // Setup authRepository to return default USER_STATE:
        every { authRepository.userStateFlow } returns MutableStateFlow(USER_STATE)

        // Setup authDiskSource to have each user's authenticator sync unlock key:
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_1_ID,
            authenticatorSyncUnlockKey = USER_1_UNLOCK_KEY,
        )
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_2_ID,
            authenticatorSyncUnlockKey = USER_2_UNLOCK_KEY,
        )
        // Setup vaultRepository to not be stuck unlocking:
        every { vaultRepository.vaultUnlockDataStateFlow } returns MutableStateFlow(
            listOf(
                VaultUnlockData(USER_1_ID, VaultUnlockData.Status.UNLOCKED),
                VaultUnlockData(USER_2_ID, VaultUnlockData.Status.UNLOCKED),
            ),
        )
        // Setup vaultRepository to be unlocked for user 1:
        every { vaultRepository.isVaultUnlocked(USER_1_ID) } returns true
        // But locked for user 2:
        every { vaultRepository.isVaultUnlocked(USER_2_ID) } returns false
        every { vaultRepository.lockVault(USER_2_ID) } returns Unit
        coEvery {
            vaultRepository.unlockVaultWithDecryptedUserKey(
                userId = USER_2_ID,
                decryptedUserKey = USER_2_UNLOCK_KEY,
            )
        } returns VaultUnlockResult.Success

        // Add some ciphers to vaultDiskSource for each user,
        // and setup mock decryption for them:
        every { vaultDiskSource.getCiphers(USER_1_ID) } returns flowOf(USER_1_CIPHERS)
        every { vaultDiskSource.getCiphers(USER_2_ID) } returns flowOf(USER_2_CIPHERS)
        mockkStatic(SyncResponseJson.Cipher::toEncryptedSdkCipher)
        every {
            USER_1_TOTP_CIPHER.toEncryptedSdkCipher()
        } returns USER_1_ENCRYPTED_SDK_TOTP_CIPHER
        every {
            USER_2_TOTP_CIPHER.toEncryptedSdkCipher()
        } returns USER_2_ENCRYPTED_SDK_TOTP_CIPHER
        coEvery {
            vaultSdkSource.decryptCipher(USER_1_ID, USER_1_ENCRYPTED_SDK_TOTP_CIPHER)
        } returns USER_1_DECRYPTED_TOTP_CIPHER.asSuccess()
        coEvery {
            vaultSdkSource.decryptCipher(USER_2_ID, USER_2_ENCRYPTED_SDK_TOTP_CIPHER)
        } returns USER_2_DECRYPTED_TOTP_CIPHER.asSuccess()
    }

    @AfterEach
    fun teardown() {
        confirmVerified(authRepository, vaultSdkSource, vaultRepository, vaultDiskSource)
        unmockkStatic(SyncResponseJson.Cipher::toEncryptedSdkCipher)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `syncAccounts with user 1 vault unlocked and all data present should send expected shared accounts data`() =
        runTest {
            val sharedAccounts = authenticatorBridgeRepository.getSharedAccounts()
            assertEquals(
                BOTH_ACCOUNT_SUCCESS,
                sharedAccounts,
            )
            verify { authRepository.userStateFlow }
            verify { vaultRepository.vaultUnlockDataStateFlow }
            verify { vaultDiskSource.getCiphers(USER_1_ID) }
            verify { vaultDiskSource.getCiphers(USER_2_ID) }
            verify { vaultRepository.isVaultUnlocked(USER_1_ID) }
            verify { vaultRepository.isVaultUnlocked(USER_2_ID) }
            coVerify {
                vaultRepository.unlockVaultWithDecryptedUserKey(
                    userId = USER_2_ID,
                    decryptedUserKey = USER_2_UNLOCK_KEY,
                )
            }
            verify { vaultRepository.lockVault(USER_2_ID) }
            coVerify { vaultSdkSource.decryptCipher(USER_1_ID, USER_1_ENCRYPTED_SDK_TOTP_CIPHER) }
            coVerify { vaultSdkSource.decryptCipher(USER_2_ID, USER_2_ENCRYPTED_SDK_TOTP_CIPHER) }
        }

    @Test
    fun `syncAccounts when userStateFlow is null should return an empty list`() = runTest {
        every { authRepository.userStateFlow } returns MutableStateFlow(null)

        val sharedData = authenticatorBridgeRepository.getSharedAccounts()

        assertTrue(sharedData.accounts.isEmpty())
        verify { authRepository.userStateFlow }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `syncAccounts when there is no authenticator sync unlock key for user 1 should omit user 1 from list`() =
        runTest {
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_1_ID,
                authenticatorSyncUnlockKey = null,
            )

            assertEquals(
                SharedAccountData(listOf(USER_2_SHARED_ACCOUNT)),
                authenticatorBridgeRepository.getSharedAccounts(),
            )

            verify { authRepository.userStateFlow }
            verify { vaultRepository.isVaultUnlocked(USER_2_ID) }
            coVerify {
                vaultRepository.unlockVaultWithDecryptedUserKey(
                    userId = USER_2_ID,
                    decryptedUserKey = USER_2_UNLOCK_KEY,
                )
            }
            verify { vaultRepository.vaultUnlockDataStateFlow }
            verify { vaultRepository.lockVault(USER_2_ID) }
            verify { vaultDiskSource.getCiphers(USER_2_ID) }
            coVerify { vaultSdkSource.decryptCipher(USER_2_ID, USER_2_ENCRYPTED_SDK_TOTP_CIPHER) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `syncAccounts when vault is locked for both users should unlock and re-lock vault for both users and filter out deleted ciphers`() =
        runTest {
            every { vaultRepository.isVaultUnlocked(USER_1_ID) } returns false
            coEvery {
                vaultRepository.unlockVaultWithDecryptedUserKey(USER_1_ID, USER_1_UNLOCK_KEY)
            } returns VaultUnlockResult.Success
            every { vaultRepository.lockVault(USER_1_ID) } returns Unit

            val sharedAccounts = authenticatorBridgeRepository.getSharedAccounts()
            assertEquals(
                BOTH_ACCOUNT_SUCCESS,
                sharedAccounts,
            )
            verify { vaultRepository.vaultUnlockDataStateFlow }
            verify { vaultDiskSource.getCiphers(USER_1_ID) }
            verify { vaultRepository.isVaultUnlocked(USER_1_ID) }
            coVerify { vaultSdkSource.decryptCipher(USER_1_ID, USER_1_ENCRYPTED_SDK_TOTP_CIPHER) }
            verify { authRepository.userStateFlow }
            coVerify {
                vaultRepository.unlockVaultWithDecryptedUserKey(
                    userId = USER_1_ID,
                    decryptedUserKey = USER_1_UNLOCK_KEY,
                )
            }
            verify { vaultRepository.lockVault(USER_1_ID) }
            verify { vaultRepository.isVaultUnlocked(USER_2_ID) }
            coVerify {
                vaultRepository.unlockVaultWithDecryptedUserKey(
                    userId = USER_2_ID,
                    decryptedUserKey = USER_2_UNLOCK_KEY,
                )
            }
            verify { vaultRepository.vaultUnlockDataStateFlow }
            verify { vaultRepository.lockVault(USER_2_ID) }
            verify { vaultDiskSource.getCiphers(USER_2_ID) }
            coVerify { vaultSdkSource.decryptCipher(USER_2_ID, USER_2_ENCRYPTED_SDK_TOTP_CIPHER) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `syncAccounts when for user 1 vault is locked and unlock fails should reset authenticator sync unlock key and omit user from the list`() =
        runTest {
            every { vaultRepository.isVaultUnlocked(USER_1_ID) } returns false
            coEvery {
                vaultRepository.unlockVaultWithDecryptedUserKey(USER_1_ID, USER_1_UNLOCK_KEY)
            } returns VaultUnlockResult.InvalidStateError

            val sharedAccounts = authenticatorBridgeRepository.getSharedAccounts()
            assertEquals(SharedAccountData(listOf(USER_2_SHARED_ACCOUNT)), sharedAccounts)
            assertNull(fakeAuthDiskSource.getAuthenticatorSyncUnlockKey(USER_1_ID))
            verify { vaultRepository.vaultUnlockDataStateFlow }
            verify { vaultRepository.isVaultUnlocked(USER_1_ID) }
            verify { authRepository.userStateFlow }
            coVerify {
                vaultRepository.unlockVaultWithDecryptedUserKey(
                    userId = USER_1_ID,
                    decryptedUserKey = USER_1_UNLOCK_KEY,
                )
            }
            verify { vaultRepository.isVaultUnlocked(USER_2_ID) }
            coVerify {
                vaultRepository.unlockVaultWithDecryptedUserKey(
                    userId = USER_2_ID,
                    decryptedUserKey = USER_2_UNLOCK_KEY,
                )
            }
            verify { vaultRepository.vaultUnlockDataStateFlow }
            verify { vaultRepository.lockVault(USER_2_ID) }
            verify { vaultDiskSource.getCiphers(USER_2_ID) }
            coVerify { vaultSdkSource.decryptCipher(USER_2_ID, USER_2_ENCRYPTED_SDK_TOTP_CIPHER) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `syncAccounts when when the vault repository never leaves unlocking state should never callback`() =
        runTest {
            val vaultUnlockStateFlow = MutableStateFlow(
                listOf(
                    VaultUnlockData(USER_1_ID, VaultUnlockData.Status.UNLOCKING),
                    VaultUnlockData(USER_2_ID, VaultUnlockData.Status.UNLOCKED),
                ),
            )
            every { vaultRepository.vaultUnlockDataStateFlow } returns vaultUnlockStateFlow
            val deferred = async {
                val sharedAccounts = authenticatorBridgeRepository.getSharedAccounts()
                assertEquals(BOTH_ACCOUNT_SUCCESS, sharedAccounts)
            }

            // None of these calls should happen until after user 1's vault state is not UNLOCKING:
            verify(exactly = 0) {
                vaultRepository.isVaultUnlocked(userId = USER_1_ID)
                vaultDiskSource.getCiphers(USER_1_ID)
            }

            // Then move out of UNLOCKING state, and things should proceed as normal:
            vaultUnlockStateFlow.value = listOf(
                VaultUnlockData(USER_1_ID, VaultUnlockData.Status.UNLOCKED),
                VaultUnlockData(USER_2_ID, VaultUnlockData.Status.UNLOCKED),
            )

            deferred.await()

            verify { authRepository.userStateFlow }
            verify { vaultDiskSource.getCiphers(USER_1_ID) }
            verify { vaultDiskSource.getCiphers(USER_2_ID) }
            verify { vaultRepository.isVaultUnlocked(USER_1_ID) }
            verify { vaultRepository.isVaultUnlocked(USER_2_ID) }
            verify { vaultRepository.vaultUnlockDataStateFlow }
            coVerify {
                vaultRepository.unlockVaultWithDecryptedUserKey(
                    userId = USER_2_ID,
                    decryptedUserKey = USER_2_UNLOCK_KEY,
                )
            }
            verify { vaultRepository.lockVault(USER_2_ID) }
            coVerify { vaultSdkSource.decryptCipher(USER_1_ID, USER_1_ENCRYPTED_SDK_TOTP_CIPHER) }
            coVerify { vaultSdkSource.decryptCipher(USER_2_ID, USER_2_ENCRYPTED_SDK_TOTP_CIPHER) }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `authenticatorSyncSymmetricKey should read from authDiskSource when one user has authenticator sync enabled`() {
        every { authRepository.userStateFlow } returns MutableStateFlow(USER_STATE)
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_1_ID,
            authenticatorSyncUnlockKey = USER_1_UNLOCK_KEY,
        )

        fakeAuthDiskSource.authenticatorSyncSymmetricKey = null
        assertNull(authenticatorBridgeRepository.authenticatorSyncSymmetricKey)

        val syncKey = generateSecretKey().getOrThrow().encoded
        fakeAuthDiskSource.authenticatorSyncSymmetricKey = syncKey

        assertEquals(syncKey, authenticatorBridgeRepository.authenticatorSyncSymmetricKey)
        verify { authRepository.userStateFlow }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `authenticatorSyncSymmetricKey should return null when no user has authenticator sync enabled`() {
        every { authRepository.userStateFlow } returns MutableStateFlow(USER_STATE)
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_1_ID,
            authenticatorSyncUnlockKey = null,
        )
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_2_ID,
            authenticatorSyncUnlockKey = null,
        )

        fakeAuthDiskSource.authenticatorSyncSymmetricKey = null
        assertNull(authenticatorBridgeRepository.authenticatorSyncSymmetricKey)

        val syncKey = generateSecretKey().getOrThrow().encoded
        fakeAuthDiskSource.authenticatorSyncSymmetricKey = syncKey
        assertNull(authenticatorBridgeRepository.authenticatorSyncSymmetricKey)
        verify { authRepository.userStateFlow }
    }
}

/**
 * Symmetric encryption key that can be used for test.
 */
private val SYMMETRIC_KEY = generateSecretKey()
    .getOrThrow()
    .encoded
    .toSymmetricEncryptionKeyData()

private const val USER_1_ID = "user1Id"
private const val USER_2_ID = "user2Id"

private const val USER_1_UNLOCK_KEY = "user1UnlockKey"
private const val USER_2_UNLOCK_KEY = "user2UnlockKey"

private val ACCOUNT_1 = mockk<UserState.Account> {
    every { userId } returns USER_1_ID
    every { name } returns "John Doe"
    every { email } returns "john@doe.com"
    every { environment.label } returns "bitwarden.com"
}

private val ACCOUNT_2 = mockk<UserState.Account> {
    every { userId } returns USER_2_ID
    every { name } returns "Jane Doe"
    every { email } returns "Jane@doe.com"
    every { environment.label } returns "bitwarden.com"
}

private val USER_STATE = UserState(
    activeUserId = USER_1_ID,
    accounts = listOf(
        ACCOUNT_1,
        ACCOUNT_2,
    ),
)

private val USER_1_TOTP_CIPHER = mockk<SyncResponseJson.Cipher> {
    every { login?.totp } returns "encryptedTotp1"
    every { deletedDate } returns null
}

private val USER_1_DELETED_TOTP_CIPHER = mockk<SyncResponseJson.Cipher> {
    every { login?.totp } returns "encryptedTotp1Deleted"
    every { deletedDate } returns ZonedDateTime.now()
}

private val USER_2_TOTP_CIPHER = mockk<SyncResponseJson.Cipher> {
    every { login?.totp } returns "encryptedTotp2"
    every { deletedDate } returns null
}

private val USER_1_ENCRYPTED_SDK_TOTP_CIPHER = mockk<Cipher>()
private val USER_2_ENCRYPTED_SDK_TOTP_CIPHER = mockk<Cipher>()

private val USER_1_DECRYPTED_TOTP_CIPHER = mockk<CipherView> {
    every { login?.totp } returns "totp1"
}
private val USER_2_DECRYPTED_TOTP_CIPHER = mockk<CipherView> {
    every { login?.totp } returns "totp2"
}

private val USER_1_EXPECTED_TOTP_LIST = listOf("totp1")
private val USER_2_EXPECTED_TOTP_LIST = listOf("totp2")

private val USER_1_SHARED_ACCOUNT = SharedAccountData.Account(
    userId = ACCOUNT_1.userId,
    name = ACCOUNT_1.name,
    email = ACCOUNT_1.email,
    environmentLabel = ACCOUNT_1.environment.label,
    totpUris = USER_1_EXPECTED_TOTP_LIST,
)

private val USER_2_SHARED_ACCOUNT = SharedAccountData.Account(
    userId = ACCOUNT_2.userId,
    name = ACCOUNT_2.name,
    email = ACCOUNT_2.email,
    environmentLabel = ACCOUNT_2.environment.label,
    totpUris = USER_2_EXPECTED_TOTP_LIST,
)

private val USER_1_CIPHERS = listOf(
    USER_1_TOTP_CIPHER,
    USER_1_DELETED_TOTP_CIPHER,
)

private val USER_2_CIPHERS = listOf(
    USER_2_TOTP_CIPHER,
)

private val BOTH_ACCOUNT_SUCCESS = SharedAccountData(
    listOf(
        USER_1_SHARED_ACCOUNT,
        USER_2_SHARED_ACCOUNT,
    ),
)
