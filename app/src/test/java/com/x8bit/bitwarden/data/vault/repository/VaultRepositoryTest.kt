package com.x8bit.bitwarden.data.vault.repository

import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockSyncResponse
import com.x8bit.bitwarden.data.vault.datasource.network.service.SyncService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.createMockSdkFolder
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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
