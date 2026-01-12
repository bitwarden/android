package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.FolderJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateFolderResponseJson
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockFolder
import com.bitwarden.network.service.FolderService
import com.bitwarden.vault.Folder
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderUpsertData
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.model.CreateFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteFolderResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateFolderResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkFolder
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.unmockkConstructor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

class FolderManagerTest {
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val folderService = mockk<FolderService>()
    private val vaultDiskSource = mockk<VaultDiskSource>()
    private val vaultSdkSource = mockk<VaultSdkSource>()
    private val mutableSyncFolderDeleteFlow = bufferedMutableSharedFlow<SyncFolderDeleteData>()
    private val mutableSyncFolderUpsertFlow = bufferedMutableSharedFlow<SyncFolderUpsertData>()
    private val pushManager: PushManager = mockk {
        every { syncFolderDeleteFlow } returns mutableSyncFolderDeleteFlow
        every { syncFolderUpsertFlow } returns mutableSyncFolderUpsertFlow
    }

    private val folderManager: FolderManager = FolderManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        folderService = folderService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        pushManager = pushManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @BeforeEach
    fun setup() {
        mockkConstructor(NoActiveUserException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkConstructor(NoActiveUserException::class)
    }

    @Test
    fun `createFolder with no active user should return CreateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = folderManager.createFolder(folderView = mockk())

            assertEquals(CreateFolderResult.Error(error = NoActiveUserException()), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createFolder with folderService Delete failure should return DeleteFolderResult Failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderId = "mockId-1"
            val error = Throwable("fail")
            coEvery { folderService.deleteFolder(folderId = folderId) } returns error.asFailure()

            val result = folderManager.deleteFolder(folderId = folderId)

            assertEquals(DeleteFolderResult.Error(error = error), result)
        }

    @Test
    fun `createFolder with encryptFolder failure should return CreateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = null,
                name = "TestName",
                revisionDate = FIXED_CLOCK.instant(),
            )
            val error = IllegalStateException()

            coEvery {
                vaultSdkSource.encryptFolder(userId = ACTIVE_USER_ID, folder = folderView)
            } returns error.asFailure()

            val result = folderManager.createFolder(folderView = folderView)
            assertEquals(CreateFolderResult.Error(error = error), result)
        }

    @Test
    fun `createFolder with folderService failure should return CreateFolderResult failure`() =
        runTest {
            val date = FIXED_CLOCK.instant()
            val testFolderName = "TestName"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = null,
                name = testFolderName,
                revisionDate = date,
            )
            val error = IllegalStateException()

            coEvery {
                vaultSdkSource.encryptFolder(userId = ACTIVE_USER_ID, folder = folderView)
            } returns Folder(id = null, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.createFolder(body = FolderJsonRequest(name = testFolderName))
            } returns error.asFailure()

            val result = folderManager.createFolder(folderView = folderView)
            assertEquals(CreateFolderResult.Error(error = error), result)
        }

    @Test
    fun `createFolder with folderService createFolder should return CreateFolderResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val date = FIXED_CLOCK.instant()
            val testFolderName = "TestName"
            val folderView = FolderView(
                id = null,
                name = testFolderName,
                revisionDate = date,
            )
            val networkFolder = SyncResponseJson.Folder(
                id = "1",
                name = testFolderName,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
            )

            coEvery {
                vaultSdkSource.encryptFolder(userId = ACTIVE_USER_ID, folder = folderView)
            } returns Folder(id = null, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.createFolder(body = FolderJsonRequest(name = testFolderName))
            } returns networkFolder.asSuccess()

            coEvery {
                vaultDiskSource.saveFolder(userId = ACTIVE_USER_ID, folder = networkFolder)
            } just runs

            coEvery {
                vaultSdkSource.decryptFolder(
                    userId = ACTIVE_USER_ID,
                    folder = networkFolder.toEncryptedSdkFolder(),
                )
            } returns folderView.asSuccess()

            val result = folderManager.createFolder(folderView = folderView)

            assertEquals(CreateFolderResult.Success(folderView = folderView), result)
        }

    @Test
    fun `deleteFolder with no active user should return DeleteFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = folderManager.deleteFolder(folderId = "Test")

            assertEquals(DeleteFolderResult.Error(error = NoActiveUserException()), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DeleteFolder with folderService Delete failure should return DeleteFolderResult Failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val error = Throwable("fail")
            val folderId = "mockId-1"
            coEvery { folderService.deleteFolder(folderId = folderId) } returns error.asFailure()

            val result = folderManager.deleteFolder(folderId = folderId)

            assertEquals(DeleteFolderResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `DeleteFolder with folderService Delete success should return DeleteFolderResult Success and update ciphers`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = ACTIVE_USER_ID
            val folderId = "mockFolderId-1"
            val mockCipher = createMockCipher(number = 1)
            val ciphers = listOf(mockCipher, createMockCipher(number = 2))
            coEvery { folderService.deleteFolder(folderId = folderId) } returns Unit.asSuccess()
            coEvery { vaultDiskSource.deleteFolder(userId = userId, folderId = folderId) } just runs
            coEvery { vaultDiskSource.getCiphers(userId = userId) } returns ciphers
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = mockCipher.copy(folderId = null),
                )
            } just runs

            val result = folderManager.deleteFolder(folderId = folderId)

            coVerify(exactly = 1) {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = mockCipher.copy(folderId = null),
                )
            }

            assertEquals(DeleteFolderResult.Success, result)
        }

    @Test
    fun `updateFolder with no active user should return UpdateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = folderManager.updateFolder(folderId = "Test", folderView = mockk())

            assertEquals(
                UpdateFolderResult.Error(errorMessage = null, error = NoActiveUserException()),
                result,
            )
        }

    @Test
    fun `updateFolder with encryptFolder failure should return UpdateFolderResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderId = "testId"
            val folderView = FolderView(
                id = folderId,
                name = "TestName",
                revisionDate = FIXED_CLOCK.instant(),
            )
            val error = IllegalStateException()

            coEvery {
                vaultSdkSource.encryptFolder(userId = ACTIVE_USER_ID, folder = folderView)
            } returns error.asFailure()

            val result = folderManager.updateFolder(folderId = folderId, folderView = folderView)

            assertEquals(UpdateFolderResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    fun `updateFolder with folderService failure should return UpdateFolderResult failure`() =
        runTest {
            val date = FIXED_CLOCK.instant()
            val testFolderName = "TestName"
            val folderId = "testId"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = folderId,
                name = testFolderName,
                revisionDate = date,
            )
            val error = IllegalStateException()

            coEvery {
                vaultSdkSource.encryptFolder(userId = ACTIVE_USER_ID, folder = folderView)
            } returns Folder(id = folderId, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.updateFolder(
                    folderId = folderId,
                    body = FolderJsonRequest(name = testFolderName),
                )
            } returns error.asFailure()

            val result = folderManager.updateFolder(folderId = folderId, folderView = folderView)
            assertEquals(UpdateFolderResult.Error(errorMessage = null, error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateFolder with folderService updateFolder Invalid response should return UpdateFolderResult Error with a non-null message`() =
        runTest {
            val date = FIXED_CLOCK.instant()
            val testFolderName = "TestName"
            val folderId = "testId"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = folderId,
                name = testFolderName,
                revisionDate = date,
            )

            coEvery {
                vaultSdkSource.encryptFolder(userId = ACTIVE_USER_ID, folder = folderView)
            } returns Folder(id = folderId, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.updateFolder(
                    folderId = folderId,
                    body = FolderJsonRequest(name = testFolderName),
                )
            } returns UpdateFolderResponseJson
                .Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                )
                .asSuccess()

            val result = folderManager.updateFolder(folderId = folderId, folderView = folderView)
            assertEquals(
                UpdateFolderResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                    error = null,
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `updateFolder with folderService updateFolder success should return UpdateFolderResult success`() =
        runTest {
            val date = FIXED_CLOCK.instant()
            val testFolderName = "TestName"
            val folderId = "testId"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = FolderView(
                id = folderId,
                name = testFolderName,
                revisionDate = date,
            )
            val networkFolder = SyncResponseJson.Folder(
                id = "1",
                name = testFolderName,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
            )

            coEvery {
                vaultSdkSource.encryptFolder(userId = ACTIVE_USER_ID, folder = folderView)
            } returns Folder(id = folderId, name = testFolderName, revisionDate = date).asSuccess()

            coEvery {
                folderService.updateFolder(
                    folderId = folderId,
                    body = FolderJsonRequest(name = testFolderName),
                )
            } returns UpdateFolderResponseJson
                .Success(folder = networkFolder)
                .asSuccess()

            coEvery {
                vaultDiskSource.saveFolder(userId = ACTIVE_USER_ID, folder = networkFolder)
            } just runs

            coEvery {
                vaultSdkSource.decryptFolder(
                    userId = ACTIVE_USER_ID,
                    folder = networkFolder.toEncryptedSdkFolder(),
                )
            } returns folderView.asSuccess()

            val result = folderManager.updateFolder(folderId = folderId, folderView = folderView)
            assertEquals(UpdateFolderResult.Success(folderView = folderView), result)
        }

    @Test
    fun `syncFolderDeleteFlow should delete folder from disk and update ciphers`() {
        val userId = "mockId-1"
        val folderId = "mockId-1"
        val cipher = createMockCipher(number = 1, folderId = folderId)
        val updatedCipher = createMockCipher(number = 1, folderId = null)

        coEvery { vaultDiskSource.deleteFolder(userId = userId, folderId = folderId) } just runs
        coEvery { vaultDiskSource.getCiphers(userId = userId) } returns listOf(cipher)
        coEvery { vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher) } just runs

        mutableSyncFolderDeleteFlow.tryEmit(
            SyncFolderDeleteData(userId = userId, folderId = folderId),
        )

        coVerify(exactly = 1) {
            vaultDiskSource.deleteFolder(userId = userId, folderId = folderId)
            vaultDiskSource.getCiphers(userId = userId)
            vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
        }
    }

    @Test
    fun `syncFolderUpsertFlow create with local folder should do nothing`() = runTest {
        val number = 1
        val userId = ACTIVE_USER_ID
        val folderId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val folder = createMockFolder(number = number)
        coEvery {
            vaultDiskSource.getFolders(userId = userId)
        } returns MutableStateFlow(listOf(folder))

        mutableSyncFolderUpsertFlow.tryEmit(
            SyncFolderUpsertData(
                userId = userId,
                folderId = folderId,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                isUpdate = false,
            ),
        )

        coVerify(exactly = 0) {
            folderService.getFolder(folderId = any())
            vaultDiskSource.saveFolder(userId = any(), folder = any())
        }
    }

    @Test
    fun `syncFolderUpsertFlow update with no local folder should do nothing`() = runTest {
        val number = 1
        val userId = ACTIVE_USER_ID
        val folderId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultDiskSource.getFolders(userId = userId)
        } returns MutableStateFlow(emptyList())

        mutableSyncFolderUpsertFlow.tryEmit(
            SyncFolderUpsertData(
                userId = userId,
                folderId = folderId,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                isUpdate = true,
            ),
        )

        coVerify(exactly = 0) {
            folderService.getFolder(folderId = any())
            vaultDiskSource.saveFolder(userId = any(), folder = any())
        }
    }

    @Test
    fun `syncFolderUpsertFlow update with more recent local folder should do nothing`() = runTest {
        val number = 1
        val userId = ACTIVE_USER_ID
        val folderId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val folder = createMockFolder(number = number)
        coEvery {
            vaultDiskSource.getFolders(userId = userId)
        } returns MutableStateFlow(listOf(folder))

        mutableSyncFolderUpsertFlow.tryEmit(
            SyncFolderUpsertData(
                userId = userId,
                folderId = folderId,
                revisionDate = ZonedDateTime.ofInstant(
                    Instant.ofEpochSecond(0), ZoneId.of("UTC"),
                ),
                isUpdate = true,
            ),
        )

        coVerify(exactly = 0) {
            folderService.getFolder(folderId = any())
            vaultDiskSource.saveFolder(userId = any(), folder = any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncFolderUpsertFlow valid create success should make a request for a folder and then store it`() =
        runTest {
            val number = 1
            val userId = ACTIVE_USER_ID
            val folderId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultDiskSource.getFolders(userId = userId)
            } returns MutableStateFlow(emptyList())
            val folder = mockk<SyncResponseJson.Folder>()
            coEvery { folderService.getFolder(folderId = folderId) } returns folder.asSuccess()
            coEvery { vaultDiskSource.saveFolder(userId = userId, folder = folder) } just runs

            mutableSyncFolderUpsertFlow.tryEmit(
                SyncFolderUpsertData(
                    userId = userId,
                    folderId = folderId,
                    revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                    isUpdate = false,
                ),
            )

            coVerify(exactly = 1) {
                folderService.getFolder(folderId = folderId)
                vaultDiskSource.saveFolder(userId = userId, folder = folder)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncFolderUpsertFlow valid update success should make a request for a folder and then store it`() =
        runTest {
            val number = 1
            val userId = ACTIVE_USER_ID
            val folderId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val folderView = createMockFolder(
                number = number,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK).minus(5, ChronoUnit.MINUTES),
            )
            coEvery {
                vaultDiskSource.getFolders(userId = userId)
            } returns MutableStateFlow(listOf(folderView))
            val folder = mockk<SyncResponseJson.Folder>()
            coEvery { folderService.getFolder(folderId = folderId) } returns folder.asSuccess()
            coEvery { vaultDiskSource.saveFolder(userId = userId, folder = folder) } just runs

            mutableSyncFolderUpsertFlow.tryEmit(
                SyncFolderUpsertData(
                    userId = userId,
                    folderId = folderId,
                    revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                    isUpdate = true,
                ),
            )

            coVerify(exactly = 1) {
                folderService.getFolder(folderId = folderId)
                vaultDiskSource.saveFolder(userId = userId, folder = folder)
            }
        }

    @Test
    fun `syncFolderUpsertFlow with inactive userId should clear the last sync time`() = runTest {
        val number = 1
        val userId = "nonActiveUserId"
        val folderId = "mockId-$number"
        val lastSyncTime = FIXED_CLOCK.instant()

        fakeSettingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = lastSyncTime)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val folderView = createMockFolder(
            number = number,
            revisionDate = ZonedDateTime.now(FIXED_CLOCK).minus(5, ChronoUnit.MINUTES),
        )
        coEvery {
            vaultDiskSource.getFolders(userId = userId)
        } returns MutableStateFlow(listOf(folderView))

        mutableSyncFolderUpsertFlow.tryEmit(
            SyncFolderUpsertData(
                userId = userId,
                folderId = folderId,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                isUpdate = true,
            ),
        )

        fakeSettingsDiskSource.assertLastSyncTime(userId = userId, expected = null)
        coVerify(exactly = 1) {
            vaultDiskSource.getFolders(userId = userId)
        }
        coVerify(exactly = 0) {
            folderService.getFolder(folderId = folderId)
            vaultDiskSource.saveFolder(userId = userId, folder = any())
        }
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private const val ACTIVE_USER_ID: String = "mockId-1"

private val MOCK_PROFILE = AccountJson.Profile(
    userId = ACTIVE_USER_ID,
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = "mockSecurityStamp-1",
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
    isTwoFactorEnabled = false,
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountTokensJson(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = ACTIVE_USER_ID,
    accounts = mapOf(
        ACTIVE_USER_ID to MOCK_ACCOUNT,
    ),
)
