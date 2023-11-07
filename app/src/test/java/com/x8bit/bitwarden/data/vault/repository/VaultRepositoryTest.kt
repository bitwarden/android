package com.x8bit.bitwarden.data.vault.repository

import com.bitwarden.core.InitCryptoRequest
import com.bitwarden.core.Kdf
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.util.KdfParamsConstants.DEFAULT_PBKDF2_ITERATIONS
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.InitializeCryptoResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.createMockSdkFolder
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultRepositoryTest {

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val syncService: SyncService = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val vaultRepository = VaultRepositoryImpl(
        syncService = syncService,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        dispatcherManager = dispatcherManager,
    )

    @Test
    fun `sync when syncService Success should update AuthDiskSource with keys`() = runTest {
        coEvery { syncService.sync() } returns Result.success(createMockSyncResponse(number = 1))
        coEvery {
            vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
        } returns mockk()
        coEvery {
            vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
        } returns mockk()
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        vaultRepository.sync()

        fakeAuthDiskSource.assertUserKey(
            userId = "mockUserId",
            userKey = "mockKey-1",
        )
        fakeAuthDiskSource.assertPrivateKey(
            userId = "mockUserId",
            privateKey = "mockPrivateKey-1",
        )
    }

    @Test
    fun `unlockVaultAndSync with initializeCrypto Success should sync and return Success`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockk()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockk()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                        privateKey = "mockPrivateKey-1",
                        organizationKeys = mapOf(),
                    ),
                )
            } returns Result.success(InitializeCryptoResult.Success)

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "mockPassword-1")

            assertEquals(
                VaultUnlockResult.Success,
                result,
            )
            coVerify { syncService.sync() }
        }

    @Test
    fun `unlockVaultAndSync with initializeCrypto failure should return GenericError`() =
        runTest {
            coEvery {
                syncService.sync()
            } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockk()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockk()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        password = "mockPassword-1",
                        userKey = "mockKey-1",
                        privateKey = "mockPrivateKey-1",
                        organizationKeys = mapOf(),
                    ),
                )
            } returns Result.failure(IllegalStateException())

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "mockPassword-1")

            assertEquals(
                VaultUnlockResult.GenericError,
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `unlockVaultAndSync with initializeCrypto AuthenticationError should return AuthenticationError`() =
        runTest {
            coEvery { syncService.sync() } returns Result.success(createMockSyncResponse(number = 1))
            coEvery {
                vaultSdkSource.decryptCipherList(listOf(createMockSdkCipher(1)))
            } returns mockk()
            coEvery {
                vaultSdkSource.decryptFolderList(listOf(createMockSdkFolder(1)))
            } returns mockk()
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultSdkSource.initializeCrypto(
                    request = InitCryptoRequest(
                        kdfParams = Kdf.Pbkdf2(iterations = DEFAULT_PBKDF2_ITERATIONS.toUInt()),
                        email = "email",
                        password = "",
                        userKey = "mockKey-1",
                        privateKey = "mockPrivateKey-1",
                        organizationKeys = mapOf(),
                    ),
                )
            } returns Result.success(InitializeCryptoResult.AuthenticationError)

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")
            assertEquals(
                VaultUnlockResult.AuthenticationError,
                result,
            )
        }

    @Test
    fun `unlockVaultAndSync with missing user state should return InvalidStateError `() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")

            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
        }

    @Test
    fun `unlockVaultAndSync with missing user key should return InvalidStateError `() =
        runTest {
            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = null,
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = "mockPrivateKey-1",
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
        }

    @Test
    fun `unlockVaultAndSync with missing private key should return InvalidStateError `() =
        runTest {
            val result = vaultRepository.unlockVaultAndSync(masterPassword = "")
            fakeAuthDiskSource.storeUserKey(
                userId = "mockUserId",
                userKey = "mockKey-1",
            )
            fakeAuthDiskSource.storePrivateKey(
                userId = "mockUserId",
                privateKey = null,
            )
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                VaultUnlockResult.InvalidStateError,
                result,
            )
        }
}

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = "mockUserId",
    accounts = mapOf(
        "mockUserId" to AccountJson(
            profile = AccountJson.Profile(
                userId = "activeUserId",
                email = "email",
                isEmailVerified = true,
                name = null,
                stamp = null,
                organizationId = null,
                avatarColorHex = null,
                hasPremium = true,
                forcePasswordResetReason = null,
                kdfType = null,
                kdfIterations = null,
                kdfMemory = null,
                kdfParallelism = null,
                userDecryptionOptions = null,
            ),
            tokens = AccountJson.Tokens(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            ),
            settings = AccountJson.Settings(
                environmentUrlData = null,
            ),
        ),
    ),
)
