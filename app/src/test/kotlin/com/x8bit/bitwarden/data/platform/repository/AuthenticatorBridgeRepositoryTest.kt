package com.x8bit.bitwarden.data.platform.repository

import com.bitwarden.authenticatorbridge.model.SharedAccountData
import com.bitwarden.authenticatorbridge.util.generateSecretKey
import com.bitwarden.authenticatorbridge.util.toSymmetricEncryptionKeyData
import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.core.InitUserCryptoMethod
import com.bitwarden.core.InitUserCryptoRequest
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.crypto.Kdf
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.toEnvironmentUrlsOrDefault
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.repository.util.sanitizeTotpUri
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.ScopedVaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.repository.util.createWrappedAccountCryptographicState
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class AuthenticatorBridgeRepositoryTest {

    private val scopedVaultSdkSource = mockk<ScopedVaultSdkSource>()
    private val vaultDiskSource = mockk<VaultDiskSource>()
    private val fakeAuthDiskSource = FakeAuthDiskSource()

    private val authenticatorBridgeRepository: AuthenticatorBridgeRepository =
        AuthenticatorBridgeRepositoryImpl(
            authDiskSource = fakeAuthDiskSource,
            vaultDiskSource = vaultDiskSource,
            scopedVaultSdkSource = scopedVaultSdkSource,
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
        fakeAuthDiskSource.userState = USER_STATE_JSON

        // Setup authDiskSource to have each user's authenticator sync unlock key:
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_1_ID,
            authenticatorSyncUnlockKey = USER_1_UNLOCK_KEY,
        )
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_2_ID,
            authenticatorSyncUnlockKey = USER_2_UNLOCK_KEY,
        )
        fakeAuthDiskSource.storePrivateKey(userId = USER_1_ID, privateKey = USER_1_PRIVATE_KEY)
        fakeAuthDiskSource.storePrivateKey(userId = USER_2_ID, privateKey = USER_2_PRIVATE_KEY)
        coEvery {
            scopedVaultSdkSource.initializeCrypto(
                userId = USER_1_ID,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = USER_1_PRIVATE_KEY,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_1_ID,
                    kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                    email = USER_1_EMAIL,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = USER_1_UNLOCK_KEY,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        coEvery {
            scopedVaultSdkSource.initializeCrypto(
                userId = USER_2_ID,
                request = InitUserCryptoRequest(
                    accountCryptographicState = createWrappedAccountCryptographicState(
                        privateKey = USER_2_PRIVATE_KEY,
                        securityState = null,
                        signedPublicKey = null,
                        signingKey = null,
                    ),
                    userId = USER_2_ID,
                    kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                    email = USER_2_EMAIL,
                    method = InitUserCryptoMethod.DecryptedKey(
                        decryptedUserKey = USER_2_UNLOCK_KEY,
                    ),
                ),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        fakeAuthDiskSource.storeOrganizationKeys(
            userId = USER_1_ID,
            organizationKeys = USER_1_ORG_KEYS,
        )
        fakeAuthDiskSource.storeOrganizationKeys(
            userId = USER_2_ID,
            organizationKeys = USER_2_ORG_KEYS,
        )
        coEvery {
            scopedVaultSdkSource.initializeOrganizationCrypto(
                userId = USER_1_ID,
                request = InitOrgCryptoRequest(organizationKeys = USER_1_ORG_KEYS),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        coEvery {
            scopedVaultSdkSource.initializeOrganizationCrypto(
                userId = USER_2_ID,
                request = InitOrgCryptoRequest(organizationKeys = USER_2_ORG_KEYS),
            )
        } returns InitializeCryptoResult.Success.asSuccess()
        every { scopedVaultSdkSource.clearCrypto(userId = USER_1_ID) } just runs
        every { scopedVaultSdkSource.clearCrypto(userId = USER_2_ID) } just runs

        // Add some ciphers to vaultDiskSource for each user,
        // and setup mock decryption for them:
        coEvery { vaultDiskSource.getTotpCiphers(USER_1_ID) } returns USER_1_CIPHERS
        coEvery { vaultDiskSource.getTotpCiphers(USER_2_ID) } returns USER_2_CIPHERS
        mockkStatic(
            SyncResponseJson.Cipher::toEncryptedSdkCipher,
            EnvironmentUrlDataJson::toEnvironmentUrlsOrDefault,
        )
        every {
            USER_1_TOTP_CIPHER.toEncryptedSdkCipher()
        } returns USER_1_ENCRYPTED_SDK_TOTP_CIPHER
        every {
            USER_2_TOTP_CIPHER.toEncryptedSdkCipher()
        } returns USER_2_ENCRYPTED_SDK_TOTP_CIPHER
        coEvery {
            scopedVaultSdkSource.decryptCipher(USER_1_ID, USER_1_ENCRYPTED_SDK_TOTP_CIPHER)
        } returns USER_1_DECRYPTED_TOTP_CIPHER.asSuccess()
        coEvery {
            scopedVaultSdkSource.decryptCipher(USER_2_ID, USER_2_ENCRYPTED_SDK_TOTP_CIPHER)
        } returns USER_2_DECRYPTED_TOTP_CIPHER.asSuccess()
        mockkStatic(String::sanitizeTotpUri)
        every {
            any<String>().sanitizeTotpUri(any(), any())
        } returns "totp"
    }

    @AfterEach
    fun teardown() {
        confirmVerified(scopedVaultSdkSource, vaultDiskSource)
        unmockkStatic(
            SyncResponseJson.Cipher::toEncryptedSdkCipher,
            EnvironmentUrlDataJson::toEnvironmentUrlsOrDefault,
            String::sanitizeTotpUri,
        )
    }

    @Test
    fun `getSharedAccounts when userStateFlow is null should return an empty list`() = runTest {
        fakeAuthDiskSource.userState = null

        val sharedData = authenticatorBridgeRepository.getSharedAccounts()

        assertTrue(sharedData.accounts.isEmpty())
    }

    @Test
    @Suppress("MaxLineLength")
    fun `getSharedAccounts when there is no authenticator sync unlock key for user 1 should omit user 1 from list`() =
        runTest {
            fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
                userId = USER_1_ID,
                authenticatorSyncUnlockKey = null,
            )

            assertEquals(
                SharedAccountData(listOf(USER_2_SHARED_ACCOUNT)),
                authenticatorBridgeRepository.getSharedAccounts(),
            )

            coVerify(exactly = 1) {
                scopedVaultSdkSource.initializeCrypto(
                    userId = USER_2_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = USER_2_PRIVATE_KEY,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_2_ID,
                        kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                        email = USER_2_EMAIL,
                        method = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = USER_2_UNLOCK_KEY,
                        ),
                    ),
                )
                scopedVaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_2_ID,
                    request = InitOrgCryptoRequest(organizationKeys = USER_2_ORG_KEYS),
                )
                vaultDiskSource.getTotpCiphers(userId = USER_2_ID)
                scopedVaultSdkSource.decryptCipher(
                    userId = USER_2_ID,
                    cipher = USER_2_ENCRYPTED_SDK_TOTP_CIPHER,
                )
            }
            verify(exactly = 1) {
                scopedVaultSdkSource.clearCrypto(userId = USER_2_ID)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `getSharedAccounts should unlock and re-lock vault for both users and filter out deleted ciphers`() =
        runTest {
            assertEquals(
                BOTH_ACCOUNT_SUCCESS,
                authenticatorBridgeRepository.getSharedAccounts(),
            )

            coVerify(exactly = 1) {
                scopedVaultSdkSource.initializeCrypto(
                    userId = USER_1_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = USER_1_PRIVATE_KEY,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_1_ID,
                        kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                        email = USER_1_EMAIL,
                        method = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = USER_1_UNLOCK_KEY,
                        ),
                    ),
                )
                scopedVaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_1_ID,
                    request = InitOrgCryptoRequest(organizationKeys = USER_1_ORG_KEYS),
                )
                vaultDiskSource.getTotpCiphers(userId = USER_1_ID)
                scopedVaultSdkSource.decryptCipher(
                    userId = USER_1_ID,
                    cipher = USER_1_ENCRYPTED_SDK_TOTP_CIPHER,
                )
                scopedVaultSdkSource.initializeCrypto(
                    userId = USER_2_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = USER_2_PRIVATE_KEY,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_2_ID,
                        kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                        email = USER_2_EMAIL,
                        method = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = USER_2_UNLOCK_KEY,
                        ),
                    ),
                )
                scopedVaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_2_ID,
                    request = InitOrgCryptoRequest(organizationKeys = USER_2_ORG_KEYS),
                )
                vaultDiskSource.getTotpCiphers(userId = USER_2_ID)
                scopedVaultSdkSource.decryptCipher(
                    userId = USER_2_ID,
                    cipher = USER_2_ENCRYPTED_SDK_TOTP_CIPHER,
                )
            }
            verify(exactly = 1) {
                scopedVaultSdkSource.clearCrypto(userId = USER_1_ID)
                scopedVaultSdkSource.clearCrypto(userId = USER_2_ID)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `getSharedAccounts when for user 1 vault fails to unlock should reset authenticator sync unlock key and omit user from the list`() =
        runTest {
            coEvery {
                scopedVaultSdkSource.initializeCrypto(
                    userId = USER_1_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = USER_1_PRIVATE_KEY,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_1_ID,
                        kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                        email = USER_1_EMAIL,
                        method = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = USER_1_UNLOCK_KEY,
                        ),
                    ),
                )
            } returns InitializeCryptoResult.AuthenticationError(error = Throwable()).asSuccess()
            assertEquals(
                SharedAccountData(listOf(USER_2_SHARED_ACCOUNT)),
                authenticatorBridgeRepository.getSharedAccounts(),
            )

            assertNull(fakeAuthDiskSource.getAuthenticatorSyncUnlockKey(USER_1_ID))
            coVerify(exactly = 1) {
                scopedVaultSdkSource.initializeCrypto(
                    userId = USER_1_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = USER_1_PRIVATE_KEY,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_1_ID,
                        kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                        email = USER_1_EMAIL,
                        method = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = USER_1_UNLOCK_KEY,
                        ),
                    ),
                )
                scopedVaultSdkSource.initializeCrypto(
                    userId = USER_2_ID,
                    request = InitUserCryptoRequest(
                        accountCryptographicState = createWrappedAccountCryptographicState(
                            privateKey = USER_2_PRIVATE_KEY,
                            securityState = null,
                            signedPublicKey = null,
                            signingKey = null,
                        ),
                        userId = USER_2_ID,
                        kdfParams = Kdf.Argon2id(iterations = 0U, memory = 0U, parallelism = 0U),
                        email = USER_2_EMAIL,
                        method = InitUserCryptoMethod.DecryptedKey(
                            decryptedUserKey = USER_2_UNLOCK_KEY,
                        ),
                    ),
                )
                scopedVaultSdkSource.initializeOrganizationCrypto(
                    userId = USER_2_ID,
                    request = InitOrgCryptoRequest(organizationKeys = USER_2_ORG_KEYS),
                )
                vaultDiskSource.getTotpCiphers(userId = USER_2_ID)
                scopedVaultSdkSource.decryptCipher(
                    userId = USER_2_ID,
                    cipher = USER_2_ENCRYPTED_SDK_TOTP_CIPHER,
                )
            }
            verify(exactly = 1) {
                scopedVaultSdkSource.clearCrypto(userId = USER_1_ID)
                scopedVaultSdkSource.clearCrypto(userId = USER_2_ID)
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `authenticatorSyncSymmetricKey should read from authDiskSource when one user has authenticator sync enabled`() {
        fakeAuthDiskSource.storeAuthenticatorSyncUnlockKey(
            userId = USER_1_ID,
            authenticatorSyncUnlockKey = USER_1_UNLOCK_KEY,
        )

        fakeAuthDiskSource.authenticatorSyncSymmetricKey = null
        assertNull(authenticatorBridgeRepository.authenticatorSyncSymmetricKey)

        val syncKey = generateSecretKey().getOrThrow().encoded
        fakeAuthDiskSource.authenticatorSyncSymmetricKey = syncKey

        assertEquals(syncKey, authenticatorBridgeRepository.authenticatorSyncSymmetricKey)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `authenticatorSyncSymmetricKey should return null when no user has authenticator sync enabled`() {
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

private const val USER_1_EMAIL = "john@doe.com"
private const val USER_2_EMAIL = "jane@doe.com"

private const val USER_1_PRIVATE_KEY = "user1PrivateKey"
private const val USER_2_PRIVATE_KEY = "user2PrivateKey"

private const val USER_1_UNLOCK_KEY = "user1UnlockKey"
private const val USER_2_UNLOCK_KEY = "user2UnlockKey"

private val USER_1_ORG_KEYS = mapOf("test_1" to "test_1_data")
private val USER_2_ORG_KEYS = mapOf("test_2" to "test_2_data")

private val ACCOUNT_JSON_1 = AccountJson(
    profile = mockk {
        every { userId } returns USER_1_ID
        every { name } returns "John Doe"
        every { email } returns USER_1_EMAIL
        every { kdfType } returns KdfTypeJson.ARGON2_ID
        every { kdfIterations } returns 0
        every { kdfMemory } returns 0
        every { kdfParallelism } returns 0
    },
    tokens = AccountTokensJson(
        accessToken = "accessToken1",
        refreshToken = "refreshToken1",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson(
            base = "https://vault.bitwarden.com",
        ),
    ),
)

private val ACCOUNT_JSON_2 = AccountJson(
    profile = mockk {
        every { userId } returns USER_2_ID
        every { name } returns "Jane Doe"
        every { email } returns USER_2_EMAIL
        every { kdfType } returns KdfTypeJson.ARGON2_ID
        every { kdfIterations } returns 0
        every { kdfMemory } returns 0
        every { kdfParallelism } returns 0
    },
    tokens = AccountTokensJson(
        accessToken = "accessToken2",
        refreshToken = "refreshToken2",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson(
            base = "https://vault.bitwarden.com",
        ),
    ),
)

private val USER_STATE_JSON = UserStateJson(
    activeUserId = USER_1_ID,
    accounts = mapOf(
        USER_1_ID to ACCOUNT_JSON_1,
        USER_2_ID to ACCOUNT_JSON_2,
    ),
)

private val USER_1_TOTP_CIPHER = mockk<SyncResponseJson.Cipher> {
    every { login?.totp } returns "encryptedTotp1"
    every { login?.username } returns "username"
    every { deletedDate } returns null
    every { name } returns "cipher1"
}

private val USER_1_DELETED_TOTP_CIPHER = mockk<SyncResponseJson.Cipher> {
    every { login?.totp } returns "encryptedTotp1Deleted"
    every { login?.username } returns "username"
    every { deletedDate } returns ZonedDateTime.now()
    every { name } returns "cipher1"
}

private val USER_2_TOTP_CIPHER = mockk<SyncResponseJson.Cipher> {
    every { login?.totp } returns "encryptedTotp2"
    every { login?.username } returns "username"
    every { deletedDate } returns null
    every { name } returns "cipher2"
}

private val USER_1_ENCRYPTED_SDK_TOTP_CIPHER = mockk<Cipher>()
private val USER_2_ENCRYPTED_SDK_TOTP_CIPHER = mockk<Cipher>()

private val USER_1_DECRYPTED_TOTP_CIPHER = mockk<CipherView> {
    every { login?.totp } returns "totp"
    every { login?.username } returns "username"
    every { name } returns "cipher1"
}
private val USER_2_DECRYPTED_TOTP_CIPHER = mockk<CipherView> {
    every { login?.totp } returns "totp"
    every { login?.username } returns "username"
    every { name } returns "cipher1"
}

private val USER_1_EXPECTED_TOTP_LIST = listOf("totp")
private val USER_2_EXPECTED_TOTP_LIST = listOf("totp")

private val USER_1_SHARED_ACCOUNT = SharedAccountData.Account(
    userId = ACCOUNT_JSON_1.profile.userId,
    name = ACCOUNT_JSON_1.profile.name,
    email = ACCOUNT_JSON_1.profile.email,
    environmentLabel = Environment.Us.label,
    totpUris = USER_1_EXPECTED_TOTP_LIST,
)

private val USER_2_SHARED_ACCOUNT = SharedAccountData.Account(
    userId = ACCOUNT_JSON_2.profile.userId,
    name = ACCOUNT_JSON_2.profile.name,
    email = ACCOUNT_JSON_2.profile.email,
    environmentLabel = Environment.Us.label,
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
