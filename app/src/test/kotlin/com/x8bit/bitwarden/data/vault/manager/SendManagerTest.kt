package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.error.MissingPropertyException
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.network.model.CreateFileSendResponse
import com.bitwarden.network.model.CreateSendJsonResponse
import com.bitwarden.network.model.SendTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateSendResponseJson
import com.bitwarden.network.model.createMockFileSendResponseJson
import com.bitwarden.network.model.createMockSend
import com.bitwarden.network.model.createMockSendJsonRequest
import com.bitwarden.network.service.SendsService
import com.bitwarden.send.SendType
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendUpsertData
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkSend
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkConstructor
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

@Suppress("LargeClass")
class SendManagerTest {
    private val fileManager: FileManager = mockk {
        coEvery { delete(files = anyVararg()) } just runs
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val sendsService = mockk<SendsService>()
    private val vaultDiskSource = mockk<VaultDiskSource>()
    private val vaultSdkSource = mockk<VaultSdkSource>()
    private val reviewPromptManager = mockk<ReviewPromptManager> {
        every { registerCreateSendAction() } just runs
    }
    private val mutableSyncSendDeleteFlow = bufferedMutableSharedFlow<SyncSendDeleteData>()
    private val mutableSyncSendUpsertFlow = bufferedMutableSharedFlow<SyncSendUpsertData>()
    private val pushManager: PushManager = mockk {
        every { syncSendDeleteFlow } returns mutableSyncSendDeleteFlow
        every { syncSendUpsertFlow } returns mutableSyncSendUpsertFlow
    }

    private val sendManager: SendManager = SendManagerImpl(
        sendsService = sendsService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        settingsDiskSource = fakeSettingsDiskSource,
        fileManager = fileManager,
        reviewPromptManager = reviewPromptManager,
        pushManager = pushManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
        mockkConstructor(NoActiveUserException::class, MissingPropertyException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
        every {
            anyConstructed<MissingPropertyException>() == any<MissingPropertyException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class)
        unmockkConstructor(NoActiveUserException::class, MissingPropertyException::class)
    }

    @Test
    fun `syncSendDeleteFlow should delete send from disk`() {
        val userId = "mockId-1"
        val sendId = "mockId-1"
        coEvery { vaultDiskSource.deleteSend(userId = userId, sendId = sendId) } just runs

        mutableSyncSendDeleteFlow.tryEmit(SyncSendDeleteData(userId = userId, sendId = sendId))

        coVerify { vaultDiskSource.deleteSend(userId = userId, sendId = sendId) }
    }

    @Test
    fun `syncSendUpsertFlow create with local send should do nothing`() = runTest {
        val number = 1
        val userId = MOCK_USER_STATE.activeUserId
        val sendId = "mockId-$number"
        val send = createMockSend(number = 1, id = sendId)

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { vaultDiskSource.getSends(userId = userId) } returns MutableStateFlow(listOf(send))

        mutableSyncSendUpsertFlow.tryEmit(
            SyncSendUpsertData(
                userId = userId,
                sendId = sendId,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                isUpdate = false,
            ),
        )

        coVerify(exactly = 0) {
            sendsService.getSend(sendId = sendId)
            vaultDiskSource.saveSend(userId = userId, send = any())
        }
    }

    @Test
    fun `syncSendUpsertFlow update with no local send should do nothing`() = runTest {
        val number = 1
        val userId = MOCK_USER_STATE.activeUserId
        val sendId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { vaultDiskSource.getSends(userId = userId) } returns MutableStateFlow(emptyList())

        mutableSyncSendUpsertFlow.tryEmit(
            SyncSendUpsertData(
                userId = userId,
                sendId = sendId,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                isUpdate = true,
            ),
        )

        coVerify(exactly = 0) {
            sendsService.getSend(sendId = sendId)
            vaultDiskSource.saveSend(userId = userId, send = any())
        }
    }

    @Test
    fun `syncSendUpsertFlow update with more recent local send should do nothing`() = runTest {
        val number = 1
        val userId = MOCK_USER_STATE.activeUserId
        val sendId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val send = createMockSend(
            number = number,
            revisionDate = ZonedDateTime.now(FIXED_CLOCK),
        )
        val updatedSend = createMockSend(number = number)
        coEvery { vaultDiskSource.getSends(userId = userId) } returns MutableStateFlow(listOf(send))
        coEvery { sendsService.getSend(sendId = sendId) } returns updatedSend.asSuccess()
        coEvery { vaultDiskSource.saveSend(userId = userId, send = updatedSend) } just runs

        mutableSyncSendUpsertFlow.tryEmit(
            SyncSendUpsertData(
                userId = userId,
                sendId = sendId,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK).minus(5, ChronoUnit.MINUTES),
                isUpdate = true,
            ),
        )

        coVerify(exactly = 0) {
            sendsService.getSend(sendId = sendId)
            vaultDiskSource.saveSend(userId = userId, send = any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow update failure with 404 code should make a request for a send and then delete it`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val sendId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery { sendsService.getSend(sendId = sendId) } returns response.asFailure()
            coEvery {
                vaultDiskSource.deleteSend(userId = userId, sendId = sendId)
            } just runs

            val sendView = createMockSend(
                number = number,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK).minus(5, ChronoUnit.MINUTES),
            )
            coEvery {
                vaultDiskSource.getSends(userId = userId)
            } returns MutableStateFlow(listOf(sendView))

            mutableSyncSendUpsertFlow.tryEmit(
                SyncSendUpsertData(
                    userId = userId,
                    sendId = sendId,
                    revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                    isUpdate = true,
                ),
            )

            coVerify(exactly = 1) {
                sendsService.getSend(sendId = sendId)
                vaultDiskSource.deleteSend(userId = userId, sendId = sendId)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow create failure with 404 code should make a request for a send and do nothing`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val sendId = "mockId-1"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery { sendsService.getSend(sendId = sendId) } returns response.asFailure()
            coEvery {
                vaultDiskSource.getSends(userId = userId)
            } returns MutableStateFlow(emptyList())

            mutableSyncSendUpsertFlow.tryEmit(
                SyncSendUpsertData(
                    userId = userId,
                    sendId = sendId,
                    revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                    isUpdate = false,
                ),
            )

            coVerify(exactly = 1) {
                sendsService.getSend(sendId = sendId)
            }
            coVerify(exactly = 0) {
                vaultDiskSource.deleteSend(userId = userId, sendId = sendId)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow valid create success should make a request for a send and then store it`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val sendId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultDiskSource.getSends(userId = userId)
            } returns MutableStateFlow(emptyList())
            val send = mockk<SyncResponseJson.Send>()
            coEvery { sendsService.getSend(sendId = sendId) } returns send.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = send) } just runs

            mutableSyncSendUpsertFlow.tryEmit(
                SyncSendUpsertData(
                    userId = userId,
                    sendId = sendId,
                    revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                    isUpdate = false,
                ),
            )

            coVerify(exactly = 1) {
                sendsService.getSend(sendId = sendId)
                vaultDiskSource.saveSend(userId = userId, send = send)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncSendUpsertFlow valid update success should make a request for a send and then store it`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val sendId = "mockId-$number"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sendView = createMockSend(
                number = number,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK).minus(5, ChronoUnit.MINUTES),
            )
            coEvery {
                vaultDiskSource.getSends(userId = userId)
            } returns MutableStateFlow(listOf(sendView))

            val send = mockk<SyncResponseJson.Send>()
            coEvery { sendsService.getSend(sendId = sendId) } returns send.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = send) } just runs

            mutableSyncSendUpsertFlow.tryEmit(
                SyncSendUpsertData(
                    userId = userId,
                    sendId = sendId,
                    revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                    isUpdate = true,
                ),
            )

            coVerify(exactly = 1) {
                sendsService.getSend(sendId = sendId)
                vaultDiskSource.saveSend(userId = userId, send = send)
            }
        }

    @Test
    fun `syncSendUpsertFlow with inactive userId should clear the last sync time`() = runTest {
        val number = 1
        val userId = "nonActiveUserId"
        val sendId = "mockId-$number"
        val lastSyncTime = FIXED_CLOCK.instant()

        fakeSettingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = lastSyncTime)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val sendView = createMockSend(
            number = number,
            revisionDate = ZonedDateTime.now(FIXED_CLOCK).minus(5, ChronoUnit.MINUTES),
        )
        coEvery {
            vaultDiskSource.getSends(userId = userId)
        } returns MutableStateFlow(listOf(sendView))

        mutableSyncSendUpsertFlow.tryEmit(
            SyncSendUpsertData(
                userId = userId,
                sendId = sendId,
                revisionDate = ZonedDateTime.now(FIXED_CLOCK),
                isUpdate = true,
            ),
        )

        fakeSettingsDiskSource.assertLastSyncTime(userId = userId, expected = null)
        coVerify(exactly = 1) {
            vaultDiskSource.getSends(userId = userId)
        }
        coVerify(exactly = 0) {
            sendsService.getSend(sendId = sendId)
            vaultDiskSource.saveSend(userId = userId, send = any())
        }
    }

    @Test
    fun `createSend with no active user should return CreateSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = sendManager.createSend(
                sendView = mockk(),
                fileUri = mockk(),
            )

            assertEquals(
                CreateSendResult.Error(message = null, error = NoActiveUserException()),
                result,
            )
        }

    @Test
    fun `createSend with encryptSend failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns error.asFailure()

            val result = sendManager.createSend(sendView = mockSendView, fileUri = null)

            assertEquals(CreateSendResult.Error(message = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with TEXT and sendsService createTextSend failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            coEvery {
                sendsService.createTextSend(
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns error.asFailure()

            val result = sendManager.createSend(sendView = mockSendView, fileUri = null)

            assertEquals(CreateSendResult.Error(message = error.message, error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createSend with TEXT and sendsService createTextSend success should return CreateSendResult success and increment send action count`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            val mockSdkSend = createMockSdkSend(number = 1, type = SendType.TEXT)
            val mockSend = createMockSend(number = 1, type = SendTypeJson.TEXT)
            val mockSendViewResult = createMockSendView(number = 2)
            val sendTextResponse = CreateSendJsonResponse.Success(send = mockSend)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery {
                sendsService.createTextSend(
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns sendTextResponse.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId, mockSend) } just runs
            coEvery {
                vaultSdkSource.decryptSend(userId, mockSdkSend)
            } returns mockSendViewResult.asSuccess()

            val result = sendManager.createSend(sendView = mockSendView, fileUri = null)

            assertEquals(CreateSendResult.Success(mockSendViewResult), result)

            verify(exactly = 1) { reviewPromptManager.registerCreateSendAction() }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and sendsService createFileSend failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val uri = setupMockUri(url = "www.test.com")
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val decryptedFile = mockk<File> {
                every { length() } returns 1
                every { absolutePath } returns "mockAbsolutePath"
            }
            val encryptedFile = mockk<File> {
                every { length() } returns 1
                every { absolutePath } returns "mockAbsolutePath"
            }
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery { fileManager.writeUriToCache(any()) } returns decryptedFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = mockSdkSend,
                    path = "mockAbsolutePath",
                    destinationFilePath = "mockAbsolutePath",
                )
            } returns encryptedFile.asSuccess()
            val error = IllegalStateException()
            coEvery {
                sendsService.createFileSend(body = createMockSendJsonRequest(number = 1))
            } returns error.asFailure()

            val result = sendManager.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Error(message = error.message, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and sendsService uploadFile failure should return CreateSendResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val url = "www.test.com"
            val uri = setupMockUri(url = url)
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val decryptedFile = mockk<File> {
                every { name } returns "mockFileName"
                every { absolutePath } returns "mockAbsolutePath"
                every { length() } returns 1
            }
            val encryptedFile = mockk<File> {
                every { name } returns "mockFileName"
                every { absolutePath } returns "mockAbsolutePath"
                every { length() } returns 1
            }
            val sendFileResponse = CreateFileSendResponse.Success(
                createMockFileSendResponseJson(number = 1),
            )
            val error = Throwable()
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(userId, mockSdkSend)
            } returns mockSendView.asSuccess()
            every { fileManager.filesDirectory } returns "mockFilesDirectory"
            coEvery { fileManager.writeUriToCache(any()) } returns decryptedFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = mockSdkSend,
                    path = "mockAbsolutePath",
                    destinationFilePath = "mockAbsolutePath",
                )
            } returns encryptedFile.asSuccess()
            coEvery {
                vaultDiskSource.saveSend(
                    userId,
                    sendFileResponse.createFileJsonResponse.sendResponse,
                )
            } just runs
            coEvery {
                sendsService.createFileSend(body = createMockSendJsonRequest(number = 1))
            } returns sendFileResponse.asSuccess()
            coEvery {
                sendsService.uploadFile(
                    sendFileResponse = sendFileResponse.createFileJsonResponse,
                    encryptedFile = encryptedFile,
                )
            } returns error.asFailure()

            val result = sendManager.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Error(message = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and fileManager uriToByteArray failure should return CreateSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val url = "www.test.com"
            val uri = setupMockUri(url = url)
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val error = Throwable()
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()
            coEvery { fileManager.writeUriToCache(any()) } returns error.asFailure()

            val result = sendManager.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Error(message = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createSend with FILE and sendsService uploadFile success should return CreateSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val url = "www.test.com"
            val uri = setupMockUri(url = url)
            val mockSendView = createMockSendView(number = 1)
            val mockSdkSend = createMockSdkSend(number = 1)
            val decryptedFile = mockk<File> {
                every { name } returns "mockFileName"
                every { absolutePath } returns "mockAbsolutePath"
            }
            val encryptedFile = mockk<File> {
                every { length() } returns 1
            }
            val sendFileResponse = CreateFileSendResponse.Success(
                createMockFileSendResponseJson(number = 1),
            )
            val mockSendViewResult = createMockSendView(number = 1)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns mockSdkSend.asSuccess()

            every { fileManager.filesDirectory } returns "mockFilesDirectory"
            coEvery { fileManager.writeUriToCache(any()) } returns decryptedFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = mockSdkSend,
                    path = "mockAbsolutePath",
                    destinationFilePath = "mockAbsolutePath",
                )
            } returns encryptedFile.asSuccess()
            coEvery {
                sendsService.createFileSend(body = createMockSendJsonRequest(number = 1))
            } returns sendFileResponse.asSuccess()
            coEvery {
                sendsService.uploadFile(
                    sendFileResponse = sendFileResponse.createFileJsonResponse,
                    encryptedFile = encryptedFile,
                )
            } returns sendFileResponse.createFileJsonResponse.sendResponse.asSuccess()
            coEvery {
                vaultDiskSource.saveSend(
                    userId,
                    sendFileResponse.createFileJsonResponse.sendResponse,
                )
            } just runs
            coEvery {
                vaultSdkSource.decryptSend(userId, mockSdkSend)
            } returns mockSendViewResult.asSuccess()

            val result = sendManager.createSend(sendView = mockSendView, fileUri = uri)

            assertEquals(CreateSendResult.Success(mockSendViewResult), result)
        }

    @Test
    fun `deleteSend with no active user should return DeleteSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = sendManager.deleteSend(sendId = "sendId")

            assertEquals(
                DeleteSendResult.Error(error = NoActiveUserException()),
                result,
            )
        }

    @Test
    fun `deleteSend with sendsService deleteSend failure should return DeleteSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sendId = "mockId-1"
            val error = Throwable("Fail")
            coEvery {
                sendsService.deleteSend(sendId = sendId)
            } returns error.asFailure()

            val result = sendManager.deleteSend(sendId)

            assertEquals(DeleteSendResult.Error(error = error), result)
        }

    @Test
    fun `deleteSend with sendsService deleteSend success should return DeleteSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "mockId-1"
            coEvery { sendsService.deleteSend(sendId = sendId) } returns Unit.asSuccess()
            coEvery { vaultDiskSource.deleteSend(userId, sendId) } just runs

            val result = sendManager.deleteSend(sendId)

            assertEquals(DeleteSendResult.Success, result)
        }

    @Test
    fun `removePasswordSend with no active user should return RemovePasswordSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = sendManager.removePasswordSend(sendId = "sendId")

            assertEquals(
                RemovePasswordSendResult.Error(
                    errorMessage = null,
                    error = NoActiveUserException(),
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePasswordSend with sendsService removeSendPassword Error should return RemovePasswordSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val sendId = "sendId1234"
            val error = Throwable("Fail")
            coEvery {
                sendsService.removeSendPassword(sendId = sendId)
            } returns error.asFailure()

            val result = sendManager.removePasswordSend(sendId = sendId)

            assertEquals(RemovePasswordSendResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePasswordSend with sendsService removeSendPassword Success and vaultSdkSource decryptSend Failure should return RemovePasswordSendResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSend = createMockSend(number = 1)
            val error = Throwable("Fail")
            coEvery {
                sendsService.removeSendPassword(sendId = sendId)
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(userId = userId, send = createMockSdkSend(number = 1))
            } returns error.asFailure()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = sendManager.removePasswordSend(sendId = sendId)

            assertEquals(RemovePasswordSendResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `removePasswordSend with sendsService removeSendPassword Success should return RemovePasswordSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1)
            val mockSend = createMockSend(number = 1)
            coEvery {
                sendsService.removeSendPassword(sendId = sendId)
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            coEvery {
                vaultSdkSource.decryptSend(userId = userId, send = createMockSdkSend(number = 1))
            } returns mockSendView.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = sendManager.removePasswordSend(sendId = sendId)

            assertEquals(RemovePasswordSendResult.Success(mockSendView), result)
        }

    @Test
    fun `updateSend with no active user should return UpdateSendResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = sendManager.updateSend(
            sendId = "sendId",
            sendView = mockk(),
        )

        assertEquals(
            UpdateSendResult.Error(errorMessage = null, error = NoActiveUserException()),
            result,
        )
    }

    @Test
    fun `updateSend with encryptSend failure should return UpdateSendResult failure`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val sendId = "sendId1234"
        val mockSendView = createMockSendView(number = 1)
        val error = IllegalStateException()
        coEvery {
            vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
        } returns error.asFailure()

        val result = sendManager.updateSend(
            sendId = sendId,
            sendView = mockSendView,
        )

        assertEquals(UpdateSendResult.Error(errorMessage = null, error = error), result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend failure should return UpdateSendResult Error with a null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            val error = IllegalStateException()
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns error.asFailure()

            val result = sendManager.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend Invalid response should return UpdateSendResult Error with a non-null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns UpdateSendResponseJson
                .Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                )
                .asSuccess()

            val result = sendManager.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(
                UpdateSendResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                    error = null,
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend success and decryption error should return UpdateSendResult Error with a null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            val mockSend = createMockSend(number = 1, type = SendTypeJson.TEXT)
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.decryptSend(
                    userId = userId, send = createMockSdkSend(number = 1, type = SendType.TEXT),
                )
            } returns error.asFailure()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = sendManager.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateSend with sendsService updateSend Success response should return UpdateSendResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val sendId = "sendId1234"
            val mockSendView = createMockSendView(number = 1, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.encryptSend(userId = userId, sendView = mockSendView)
            } returns createMockSdkSend(number = 1, type = SendType.TEXT).asSuccess()
            val mockSend = createMockSend(number = 1, type = SendTypeJson.TEXT)
            coEvery {
                sendsService.updateSend(
                    sendId = sendId,
                    body = createMockSendJsonRequest(number = 1, type = SendTypeJson.TEXT)
                        .copy(fileLength = null),
                )
            } returns UpdateSendResponseJson.Success(send = mockSend).asSuccess()
            val mockSendViewResult = createMockSendView(number = 2, type = SendType.TEXT)
            coEvery {
                vaultSdkSource.decryptSend(
                    userId = userId,
                    send = createMockSdkSend(number = 1, type = SendType.TEXT),
                )
            } returns mockSendViewResult.asSuccess()
            coEvery { vaultDiskSource.saveSend(userId = userId, send = mockSend) } just runs

            val result = sendManager.updateSend(
                sendId = sendId,
                sendView = mockSendView,
            )

            assertEquals(UpdateSendResult.Success(mockSendViewResult), result)
        }

    //region Helper functions

    private fun setupMockUri(
        url: String,
        queryParams: Map<String, String> = emptyMap(),
    ): Uri {
        val mockUri = mockk<Uri> {
            queryParams.forEach {
                every { getQueryParameter(it.key) } returns it.value
            }
        }
        every { Uri.parse(url) } returns mockUri
        return mockUri
    }

    //endregion Helper functions
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private val MOCK_PROFILE = AccountJson.Profile(
    userId = "mockId-1",
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
    activeUserId = "mockId-1",
    accounts = mapOf(
        "mockId-1" to MOCK_ACCOUNT,
    ),
)
