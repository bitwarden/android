package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import androidx.core.net.toUri
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.manager.model.DownloadResult
import com.bitwarden.network.model.AttachmentJsonRequest
import com.bitwarden.network.model.CreateCipherInOrganizationJsonRequest
import com.bitwarden.network.model.CreateCipherResponseJson
import com.bitwarden.network.model.ShareCipherJsonRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.UpdateCipherCollectionsJsonRequest
import com.bitwarden.network.model.UpdateCipherResponseJson
import com.bitwarden.network.model.createMockAttachment
import com.bitwarden.network.model.createMockAttachmentResponse
import com.bitwarden.network.model.createMockCipher
import com.bitwarden.network.model.createMockCipherJsonRequest
import com.bitwarden.network.model.createMockCollection
import com.bitwarden.network.model.createMockLogin
import com.bitwarden.network.service.CiphersService
import com.bitwarden.vault.Attachment
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.EncryptionContext
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherUpsertData
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockAttachmentView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockEncryptionContext
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkAttachment
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.RestoreCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.ShareCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipherResponse
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toNetworkAttachmentRequest
import io.mockk.Ordering
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
class CipherManagerTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val fileManager: FileManager = mockk {
        coEvery { delete(*anyVararg()) } just runs
    }
    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val ciphersService: CiphersService = mockk()
    private val vaultDiskSource: VaultDiskSource = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val reviewPromptManager: ReviewPromptManager = mockk {
        every { registerAddCipherAction() } just runs
    }
    private val mutableSyncCipherDeleteFlow = bufferedMutableSharedFlow<SyncCipherDeleteData>()
    private val mutableSyncCipherUpsertFlow = bufferedMutableSharedFlow<SyncCipherUpsertData>()
    private val pushManager: PushManager = mockk {
        every { syncCipherDeleteFlow } returns mutableSyncCipherDeleteFlow
        every { syncCipherUpsertFlow } returns mutableSyncCipherUpsertFlow
    }

    private val cipherManager: CipherManager = CipherManagerImpl(
        ciphersService = ciphersService,
        settingsDiskSource = fakeSettingsDiskSource,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        fileManager = fileManager,
        clock = clock,
        reviewPromptManager = reviewPromptManager,
        pushManager = pushManager,
        dispatcherManager = FakeDispatcherManager(),
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
        mockkConstructor(NoActiveUserException::class)
        every {
            anyConstructed<NoActiveUserException>() == any<NoActiveUserException>()
        } returns true
        mockkConstructor(IllegalStateException::class)
        every {
            anyConstructed<IllegalStateException>() == any<IllegalStateException>()
        } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class, Instant::class)
        unmockkStatic(
            Cipher::toEncryptedNetworkCipherResponse,
            SyncResponseJson.Cipher::toEncryptedSdkCipher,
        )
        unmockkConstructor(NoActiveUserException::class)
        unmockkConstructor(IllegalStateException::class)
    }

    @Test
    fun `createCipher with no active user should return CreateCipherResult failure`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.createCipher(cipherView = mockk())

        assertEquals(
            CreateCipherResult.Error(
                errorMessage = null,
                error = NoActiveUserException(),
            ),
            result,
        )
    }

    @Test
    fun `createCipher with encryptCipher failure should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns error.asFailure()

            val result = cipherManager.createCipher(cipherView = mockCipherView)

            assertEquals(CreateCipherResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipher with ciphersService createCipher failure should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(
                        number = 1,
                        login = createMockLogin(number = 1, uri = null),
                    ),
                )
            } returns error.asFailure()

            val result = cipherManager.createCipher(cipherView = mockCipherView)

            assertEquals(CreateCipherResult.Error(errorMessage = null, error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createCipher with ciphersService createCipher Invalid response should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(
                        number = 1,
                        login = createMockLogin(number = 1, uri = null),
                    ),
                )
            } returns CreateCipherResponseJson.Invalid(
                message = "You do not have permission to edit this.",
                validationErrors = null,
            ).asSuccess()

            val result = cipherManager.createCipher(cipherView = mockCipherView)

            assertEquals(
                CreateCipherResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                    error = null,
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipher with ciphersService createCipher success should return CreateCipherResult success and increment addCipherActionCount`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(
                        number = 1,
                        login = createMockLogin(number = 1, uri = null),
                    ),
                )
            } returns CreateCipherResponseJson.Success(mockCipher).asSuccess()
            coEvery { vaultDiskSource.saveCipher(userId, mockCipher) } just runs

            val result = cipherManager.createCipher(cipherView = mockCipherView)

            assertEquals(CreateCipherResult.Success, result)
            coVerify(ordering = Ordering.ORDERED) {
                vaultDiskSource.saveCipher(userId, mockCipher)
                reviewPromptManager.registerAddCipherAction()
            }
        }

    @Test
    fun `createCipherInOrganization with no active user should return CreateCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockk(),
                collectionIds = mockk(),
            )

            assertEquals(
                CreateCipherResult.Error(
                    errorMessage = null,
                    error = NoActiveUserException(),
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipherInOrganization with encryptCipher failure should return CreateCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns error.asFailure()

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockCipherView,
                collectionIds = mockk(),
            )

            assertEquals(CreateCipherResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipherInOrganization with ciphersService createCipher failure should return CreateCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.createCipherInOrganization(
                    body = CreateCipherInOrganizationJsonRequest(
                        cipher = createMockCipherJsonRequest(
                            number = 1,
                            login = createMockLogin(number = 1, uri = null),
                        ),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns error.asFailure()

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockCipherView,
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(CreateCipherResult.Error(errorMessage = null, error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createCipherInOrganization with ciphersService createCipher Invalid response should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.createCipherInOrganization(
                    body = CreateCipherInOrganizationJsonRequest(
                        cipher = createMockCipherJsonRequest(
                            number = 1,
                            login = createMockLogin(number = 1, uri = null),
                        ),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns CreateCipherResponseJson.Invalid(
                message = "You do not have permission to edit this.",
                validationErrors = null,
            ).asSuccess()

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockCipherView,
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(
                CreateCipherResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                    error = null,
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipherInOrganization with ciphersService createCipher success should return CreateCipherResult success and increment addCipherActionCount`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.createCipherInOrganization(
                    body = CreateCipherInOrganizationJsonRequest(
                        cipher = createMockCipherJsonRequest(
                            number = 1,
                            login = createMockLogin(number = 1, uri = null),
                        ),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns CreateCipherResponseJson.Success(mockCipher).asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId,
                    mockCipher.copy(collectionIds = listOf("mockId-1")),
                )
            } just runs

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockCipherView,
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(CreateCipherResult.Success, result)
            coVerify(ordering = Ordering.ORDERED) {
                vaultDiskSource.saveCipher(
                    userId,
                    mockCipher.copy(collectionIds = listOf("mockId-1")),
                )
                reviewPromptManager.registerAddCipherAction()
            }
        }

    @Test
    fun `updateCipher with no active user should return UpdateCipherResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.updateCipher(
            cipherId = "cipherId",
            cipherView = mockk(),
        )

        assertEquals(
            UpdateCipherResult.Error(errorMessage = null, error = NoActiveUserException()),
            result,
        )
    }

    @Test
    fun `updateCipher with encryptCipher failure should return UpdateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns error.asFailure()

            val result = cipherManager.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher failure should return UpdateCipherResult Error with a null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            val error = IllegalStateException()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(
                        number = 1,
                        login = createMockLogin(number = 1, uri = null),
                    ),
                )
            } returns error.asFailure()

            val result = cipherManager.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Error(errorMessage = null, error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher Invalid response should return UpdateCipherResult Error with a non-null message`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(
                        number = 1,
                        login = createMockLogin(number = 1, uri = null),
                    ),
                )
            } returns UpdateCipherResponseJson
                .Invalid(
                    message = "You do not have permission to edit this.",
                    validationErrors = null,
                )
                .asSuccess()

            val result = cipherManager.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(
                UpdateCipherResult.Error(
                    errorMessage = "You do not have permission to edit this.",
                    error = null,
                ),
                result,
            )
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher Success response should return UpdateCipherResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId1234"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(
                        number = 1,
                        login = createMockLogin(number = 1, uri = null),
                    ),
                )
            } returns UpdateCipherResponseJson
                .Success(cipher = mockCipher)
                .asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = mockCipher.copy(collectionIds = mockCipherView.collectionIds),
                )
            } just runs

            val result = cipherManager.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Success, result)
        }

    @Test
    fun `getCipher with no active user should return Failure`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.getCipher(cipherId = "cipherId")

        assertEquals(GetCipherResult.Failure(NoActiveUserException()), result)
    }

    @Test
    fun `getCipher with decryption error should return Failure`() = runTest {
        mockkStatic(SyncResponseJson.Cipher::toEncryptedSdkCipher)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val cipherId = "cipherId"
        val sdkCipher = mockk<Cipher>()
        val cipher = mockk<SyncResponseJson.Cipher> {
            every { toEncryptedSdkCipher() } returns sdkCipher
        }
        val expected = Throwable("Fail")
        coEvery {
            vaultDiskSource.getCipher(userId = "mockId-1", cipherId = cipherId)
        } returns cipher
        coEvery {
            vaultSdkSource.decryptCipher(userId = "mockId-1", cipher = sdkCipher)
        } returns expected.asFailure()

        val result = cipherManager.getCipher(cipherId = "cipherId")

        assertEquals(GetCipherResult.Failure(expected), result)
    }

    @Test
    fun `getCipher with no cipher found should return CipherNotFound`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val cipherId = "cipherId"
        coEvery { vaultDiskSource.getCipher(userId = "mockId-1", cipherId = cipherId) } returns null

        val result = cipherManager.getCipher(cipherId = cipherId)

        assertEquals(GetCipherResult.CipherNotFound, result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = "mockId-1", cipherId = cipherId)
        }
    }

    @Test
    fun `getCipher with cipher found should return Success`() = runTest {
        mockkStatic(SyncResponseJson.Cipher::toEncryptedSdkCipher)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val cipherId = "cipherId"
        val sdkCipher = mockk<Cipher>()
        val cipher = mockk<SyncResponseJson.Cipher> {
            every { toEncryptedSdkCipher() } returns sdkCipher
        }
        val expected = mockk<CipherView>()
        coEvery {
            vaultDiskSource.getCipher(userId = "mockId-1", cipherId = cipherId)
        } returns cipher
        coEvery {
            vaultSdkSource.decryptCipher(userId = "mockId-1", cipher = sdkCipher)
        } returns expected.asSuccess()

        val result = cipherManager.getCipher(cipherId = cipherId)

        assertEquals(GetCipherResult.Success(expected), result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = "mockId-1", cipherId = cipherId)
            vaultSdkSource.decryptCipher(userId = "mockId-1", cipher = sdkCipher)
        }
    }

    @Test
    fun `hardDeleteCipher with no active user should return DeleteCipherResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.hardDeleteCipher(
            cipherId = "cipherId",
        )

        assertEquals(DeleteCipherResult.Error(error = NoActiveUserException()), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hardDeleteCipher with ciphersService hardDeleteCipher failure should return DeleteCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherId = "mockId-1"
            val error = Throwable("Fail")
            coEvery {
                ciphersService.hardDeleteCipher(cipherId = cipherId)
            } returns error.asFailure()

            val result = cipherManager.hardDeleteCipher(cipherId)

            assertEquals(DeleteCipherResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `hardDeleteCipher with ciphersService hardDeleteCipher success should return DeleteCipherResult success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "mockId-1"
            coEvery { ciphersService.hardDeleteCipher(cipherId = cipherId) } returns Unit.asSuccess()
            coEvery { vaultDiskSource.deleteCipher(userId, cipherId) } just runs

            val result = cipherManager.hardDeleteCipher(cipherId)

            assertEquals(DeleteCipherResult.Success, result)
        }

    @Test
    fun `softDeleteCipher with no active user should return DeleteCipherResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.softDeleteCipher(
            cipherId = "cipherId",
            cipherView = mockk(),
        )

        assertEquals(DeleteCipherResult.Error(error = NoActiveUserException()), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `softDeleteCipher with ciphersService softDeleteCipher failure should return DeleteCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-1"
            val cipherView = createMockCipherView(number = 1)
            val encryptionContext = createMockEncryptionContext(number = 1)
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = cipherView)
            } returns encryptionContext.asSuccess()
            coEvery {
                ciphersService.softDeleteCipher(cipherId = cipherId)
            } returns error.asFailure()

            val result = cipherManager.softDeleteCipher(
                cipherId = cipherId,
                cipherView = cipherView,
            )

            assertEquals(DeleteCipherResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `softDeleteCipher with ciphersService softDeleteCipher success should return DeleteCipherResult success`() =
        runTest {
            val fixedInstant = Instant.parse("2023-10-27T12:00:00Z")
            val userId = "mockId-1"
            val cipherId = "mockId-1"
            val encryptionContext = createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            )
            val cipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = cipherView)
            } returns encryptionContext.asSuccess()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = cipherView.copy(deletedDate = fixedInstant),
                )
            } returns encryptionContext.asSuccess()
            coEvery {
                vaultSdkSource.decryptCipher(
                    userId = userId,
                    cipher = encryptionContext.cipher,
                )
            } returns cipherView.asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { ciphersService.softDeleteCipher(cipherId = cipherId) } returns Unit.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = encryptionContext.toEncryptedNetworkCipherResponse(),
                )
            } just runs

            val result = cipherManager.softDeleteCipher(
                cipherId = cipherId,
                cipherView = cipherView,
            )

            assertEquals(DeleteCipherResult.Success, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `softDeleteCipher with cipher migration success should return DeleteCipherResult success`() =
        runTest {
            val fixedInstant = Instant.parse("2023-10-27T12:00:00Z")
            val userId = "mockId-1"
            val cipherId = "mockId-1"
            val encryptionContext = createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            )
            val cipherView = createMockCipherView(number = 1).copy(key = null)
            val networkCipher = createMockCipher(number = 1).copy(key = null)
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = cipherView)
            } returns encryptionContext.asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = encryptionContext.toEncryptedNetworkCipher(),
                )
            } returns UpdateCipherResponseJson.Success(networkCipher).asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = userId, cipher = networkCipher)
            } just runs
            coEvery {
                vaultSdkSource.decryptCipher(
                    userId = userId,
                    cipher = networkCipher.toEncryptedSdkCipher(),
                )
            } returns cipherView.asSuccess()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = cipherView.copy(deletedDate = fixedInstant),
                )
            } returns encryptionContext.asSuccess()
            coEvery {
                vaultSdkSource.decryptCipher(userId = userId, cipher = encryptionContext.cipher)
            } returns cipherView.asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { ciphersService.softDeleteCipher(cipherId = cipherId) } returns Unit.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = encryptionContext.toEncryptedNetworkCipherResponse(),
                )
            } just runs

            val result = cipherManager.softDeleteCipher(
                cipherId = cipherId,
                cipherView = cipherView,
            )

            assertEquals(DeleteCipherResult.Success, result)
            coVerify(exactly = 1) {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = encryptionContext.toEncryptedNetworkCipher(),
                )
                vaultDiskSource.saveCipher(userId = userId, cipher = networkCipher)
            }
        }

    @Test
    fun `deleteCipherAttachment with no active user should return DeleteAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = cipherManager.deleteCipherAttachment(
                cipherId = "cipherId",
                attachmentId = "attachmentId",
                cipherView = mockk(),
            )

            assertEquals(
                DeleteAttachmentResult.Error(error = NoActiveUserException()),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `deleteCipherAttachment with ciphersService deleteCipherAttachment failure should return DeleteAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherId = "mockId-1"
            val attachmentId = "mockId-1"
            val error = Throwable("Fail")
            coEvery {
                ciphersService.deleteCipherAttachment(
                    cipherId = cipherId,
                    attachmentId = attachmentId,
                )
            } returns error.asFailure()

            val result = cipherManager.deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
                cipherView = createMockCipherView(number = 1),
            )

            assertEquals(DeleteAttachmentResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `deleteCipherAttachment with ciphersService deleteCipherAttachment success should return DeleteAttachmentResult success`() =
        runTest {
            mockkStatic(Cipher::toEncryptedNetworkCipherResponse)
            every {
                createMockEncryptionContext(
                    number = 1,
                    cipher = createMockSdkCipher(number = 1, clock = clock),
                )
                    .toEncryptedNetworkCipherResponse()
            } returns createMockCipher(number = 1)
            val fixedInstant = Instant.parse("2021-01-01T00:00:00Z")
            val userId = "mockId-1"
            val cipherId = "mockId-1"
            val attachmentId = "mockId-1"
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = createMockCipherView(number = 1).copy(attachments = emptyList()),
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                ciphersService.deleteCipherAttachment(
                    cipherId = cipherId,
                    attachmentId = attachmentId,
                )
            } returns Unit.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = createMockCipher(number = 1),
                )
            } just runs
            val cipherView = createMockCipherView(number = 1)
            mockkStatic(Instant::class)
            every { Instant.now() } returns fixedInstant

            val result = cipherManager.deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
                cipherView = cipherView,
            )

            assertEquals(DeleteAttachmentResult.Success, result)
            unmockkStatic(Instant::class)
            unmockkStatic(Cipher::toEncryptedNetworkCipherResponse)
        }

    @Test
    fun `restoreCipher with no active user should return RestoreCipherResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.restoreCipher(
            cipherId = "cipherId",
            cipherView = createMockCipherView(number = 1),
        )

        assertEquals(RestoreCipherResult.Error(error = NoActiveUserException()), result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `restoreCipher with ciphersService restoreCipher failure should return RestoreCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherId = "mockId-1"
            val cipherView = createMockCipherView(number = 1)
            val error = Throwable("Fail")
            coEvery {
                ciphersService.restoreCipher(cipherId = cipherId)
            } returns error.asFailure()

            val result = cipherManager.restoreCipher(
                cipherId = cipherId,
                cipherView = cipherView,
            )

            assertEquals(RestoreCipherResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `restoreCipher with ciphersService restoreCipher success should return RestoreCipherResult success`() =
        runTest {
            val userId = "mockId-1"
            val cipherId = "mockId-1"
            val cipher = createMockCipher(number = 1)
            val cipherView = createMockCipherView(number = 1)
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { ciphersService.restoreCipher(cipherId = cipherId) } returns cipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = cipher.copy(collectionIds = cipherView.collectionIds),
                )
            } just runs

            val result = cipherManager.restoreCipher(
                cipherId = cipherId,
                cipherView = cipherView,
            )

            assertEquals(RestoreCipherResult.Success, result)
        }

    @Test
    fun `shareCipher with no active user should return ShareCipherResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.shareCipher(
            cipherId = "cipherId",
            organizationId = "organizationId",
            cipherView = mockk(),
            collectionIds = emptyList(),
        )

        assertEquals(ShareCipherResult.Error(error = NoActiveUserException()), result)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `shareCipher with cipherService shareCipher success should return ShareCipherResultSuccess`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val organizationId = "organizationId"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.moveToOrganization(
                    userId = userId,
                    organizationId = organizationId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns mockCipherView.asSuccess()
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.shareCipher(
                    cipherId = "mockId-1",
                    body = ShareCipherJsonRequest(
                        cipher = createMockCipherJsonRequest(
                            number = 1,
                            login = createMockLogin(number = 1, uri = null),
                        ),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns createMockCipher(number = 1).asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = createMockCipher(number = 1).copy(collectionIds = listOf("mockId-1")),
                )
            } just runs

            val result = cipherManager.shareCipher(
                cipherId = "mockId-1",
                organizationId = organizationId,
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Success, result)
        }

    @Test
    fun `shareCipher with attachment migration success should return ShareCipherResultSuccess`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val organizationId = "mockOrganizationId"
            val mockAttachmentView = createMockAttachmentView(number = 1, key = null)
            val initialCipherView = createMockCipherView(number = 1).copy(
                attachments = listOf(mockAttachmentView),
            )
            val mockAttachment = createMockSdkAttachment(number = 1, key = null)
            val mockCipher = createMockSdkCipher(number = 1).copy(
                attachments = listOf(mockAttachment),
            )
            val mockEncryptionContext = createMockEncryptionContext(
                number = 1,
                cipher = mockCipher,
            )
            val attachment = createMockAttachment(number = 1)
            val encryptedFile = File("path/to/encrypted/file")
            val decryptedFile = File("path/to/encrypted/file_decrypted")
            val mockCipherView = createMockCipherView(number = 1)
            val mockAttachmentJsonResponse = createMockAttachmentResponse(number = 1)
            val mockNetworkCipher = createMockCipher(number = 1)

            // Handle mocks for migration
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = initialCipherView)
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                vaultSdkSource.decryptCipher(userId = userId, cipher = mockCipher)
            } returns initialCipherView.asSuccess()
            coEvery {
                ciphersService.getCipherAttachment(cipherId = "mockId-1", attachmentId = "mockId-1")
            } returns attachment.asSuccess()
            coEvery {
                fileManager.downloadFileToCache(url = "mockUrl-1")
            } returns DownloadResult.Success(file = encryptedFile)
            coEvery {
                vaultSdkSource.decryptFile(
                    userId = userId,
                    cipher = mockCipher,
                    attachmentView = mockAttachmentView,
                    encryptedFilePath = encryptedFile.path,
                    decryptedFilePath = decryptedFile.path,
                )
            } returns Unit.asSuccess()
            val cacheFile = File("path/to/cache/file")
            coEvery {
                fileManager.writeUriToCache(fileUri = decryptedFile.toUri())
            } returns cacheFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptAttachment(
                    userId = userId,
                    cipher = mockCipher,
                    attachmentView = AttachmentView(
                        id = null,
                        url = null,
                        size = "1",
                        sizeName = null,
                        fileName = "mockFileName-1",
                        key = null,
                    ),
                    decryptedFilePath = cacheFile.absolutePath,
                    encryptedFilePath = "${cacheFile.absolutePath}.enc",
                )
            } returns mockAttachment.asSuccess()
            coEvery {
                ciphersService.createAttachment(
                    cipherId = "mockId-1",
                    body = mockAttachment.toNetworkAttachmentRequest(),
                )
            } returns mockAttachmentJsonResponse.asSuccess()
            coEvery {
                ciphersService.uploadAttachment(
                    attachment = createMockAttachmentResponse(number = 1),
                    encryptedFile = File("${cacheFile.absolutePath}.enc"),
                )
            } returns mockNetworkCipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = mockNetworkCipher.copy(collectionIds = listOf("mockId-1")),
                )
            } just runs
            coEvery {
                vaultSdkSource.decryptCipher(
                    userId = userId,
                    cipher = mockNetworkCipher
                        .copy(collectionIds = listOf("mockId-1"))
                        .toEncryptedSdkCipher(),
                )
            } returns mockCipherView.asSuccess()
            coEvery {
                ciphersService.deleteCipherAttachment(
                    cipherId = "mockId-1",
                    attachmentId = "mockId-1",
                )
            } returns Unit.asSuccess()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView.copy(attachments = emptyList()),
                )
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = mockCipher.toEncryptedNetworkCipherResponse("mockEncryptedFor-1"),
                )
            } just runs
            // Done with mocks for migration

            coEvery {
                vaultSdkSource.moveToOrganization(
                    userId = userId,
                    organizationId = organizationId,
                    cipherView = initialCipherView,
                )
            } returns mockCipherView.asSuccess()
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.shareCipher(
                    cipherId = "mockId-1",
                    body = ShareCipherJsonRequest(
                        cipher = createMockCipherJsonRequest(
                            number = 1,
                            login = createMockLogin(number = 1, uri = null),
                        ),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns createMockCipher(number = 1).asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = createMockCipher(number = 1).copy(collectionIds = listOf("mockId-1")),
                )
            } just runs

            val result = cipherManager.shareCipher(
                cipherId = "mockId-1",
                organizationId = organizationId,
                cipherView = initialCipherView,
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Success, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `shareCipher with cipherService shareCipher failure should return ShareCipherResultError`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val organizationId = "organizationId"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.moveToOrganization(
                    userId = userId,
                    organizationId = organizationId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns mockCipherView.asSuccess()
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            val error = Throwable("Fail")
            coEvery {
                ciphersService.shareCipher(
                    cipherId = "mockId-1",
                    body = ShareCipherJsonRequest(
                        cipher = createMockCipherJsonRequest(
                            number = 1,
                            login = createMockLogin(number = 1, uri = null),
                        ),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns error.asFailure()
            coEvery { vaultDiskSource.saveCipher(userId, createMockCipher(number = 1)) } just runs

            val result = cipherManager.shareCipher(
                cipherId = "mockId-1",
                organizationId = organizationId,
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Error(error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `shareCipher with cipherService encryptCipher failure should return ShareCipherResultError`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val organizationId = "organizationId"
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.moveToOrganization(
                    userId = userId,
                    organizationId = organizationId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns error.asFailure()
            coEvery {
                ciphersService.shareCipher(
                    cipherId = "mockId-1",
                    body = ShareCipherJsonRequest(
                        cipher = createMockCipherJsonRequest(number = 1),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns createMockCipher(number = 1).asSuccess()
            coEvery { vaultDiskSource.saveCipher(userId, createMockCipher(number = 1)) } just runs

            val result = cipherManager.shareCipher(
                cipherId = "mockId-1",
                organizationId = organizationId,
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Error(error = error), result)
        }

    @Test
    fun `updateCipherCollections with no active user should return ShareCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = cipherManager.updateCipherCollections(
                cipherId = "cipherId",
                cipherView = mockk(),
                collectionIds = emptyList(),
            )

            assertEquals(ShareCipherResult.Error(error = NoActiveUserException()), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipherCollections with cipherService updateCipherCollections success should return ShareCipherResultSuccess`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            coEvery {
                ciphersService.updateCipherCollections(
                    cipherId = "mockId-1",
                    body = UpdateCipherCollectionsJsonRequest(
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns Unit.asSuccess()
            coEvery { vaultDiskSource.saveCipher(userId, any()) } just runs

            val result = cipherManager.updateCipherCollections(
                cipherId = "mockId-1",
                cipherView = createMockCipherView(number = 1).copy(
                    collectionIds = listOf("mockId-1"),
                ),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Success, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipherCollections with updateCipherCollections shareCipher failure should return ShareCipherResultError`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns createMockEncryptionContext(
                number = 1,
                cipher = createMockSdkCipher(number = 1, clock = clock),
            ).asSuccess()
            val error = Throwable("Fail")
            coEvery {
                ciphersService.updateCipherCollections(
                    cipherId = "mockId-1",
                    body = UpdateCipherCollectionsJsonRequest(
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns error.asFailure()
            coEvery { vaultDiskSource.saveCipher(userId, any()) } just runs

            val result = cipherManager.updateCipherCollections(
                cipherId = "mockId-1",
                cipherView = createMockCipherView(number = 1).copy(
                    collectionIds = listOf("mockId-1"),
                ),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Error(error = error), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipherCollections with updateCipherCollections encryptCipher failure should return ShareCipherResultError`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns error.asFailure()
            coEvery {
                ciphersService.updateCipherCollections(
                    cipherId = "mockId-1",
                    body = UpdateCipherCollectionsJsonRequest(
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns Unit.asSuccess()
            coEvery { vaultDiskSource.saveCipher(userId, any()) } just runs

            val result = cipherManager.updateCipherCollections(
                cipherId = "mockId-1",
                cipherView = createMockCipherView(number = 1).copy(
                    collectionIds = listOf("mockId-1"),
                ),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Error(error = error), result)
        }

    @Test
    fun `createAttachment with no active user should return CreateAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = cipherManager.createAttachment(
                cipherId = "cipherId",
                cipherView = mockk(),
                fileSizeBytes = "mockFileSize",
                fileName = "mockFileName",
                fileUri = mockk(),
            )

            assertEquals(
                CreateAttachmentResult.Error(
                    error = NoActiveUserException(),
                    message = "No current active user!",
                ),
                result,
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAttachment with encryptCipher failure should return CreateAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId-1"
            val mockUri = setupMockUri(url = "www.test.com")
            val mockCipherView = createMockCipherView(number = 1)
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns error.asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAttachment with encryptAttachment failure should return CreateAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId-1"
            val mockUri = setupMockUri(url = "www.test.com")
            val mockCipherView = createMockCipherView(number = 1)
            val mockCipher = createMockSdkCipher(number = 1, clock = clock)
            val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
            val mockFile = File.createTempFile("mockFile", "temp")
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val mockAttachmentView = createMockAttachmentView(number = 1).copy(
                sizeName = null,
                id = null,
                url = null,
                key = null,
            )
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                fileManager.writeUriToCache(fileUri = mockUri)
            } returns mockFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptAttachment(
                    userId = userId,
                    cipher = mockCipher,
                    attachmentView = mockAttachmentView,
                    decryptedFilePath = mockFile.absolutePath,
                    encryptedFilePath = "${mockFile.absolutePath}.enc",
                )
            } returns error.asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAttachment with uriToByteArray failure should return CreateAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId-1"
            val mockUri = setupMockUri(url = "www.test.com")
            val mockCipherView = createMockCipherView(number = 1)
            val mockCipher = createMockSdkCipher(number = 1, clock = clock)
            val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                fileManager.writeUriToCache(fileUri = mockUri)
            } returns error.asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAttachment with createAttachment failure should return CreateAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId-1"
            val mockUri = setupMockUri(url = "www.test.com")
            val mockCipherView = createMockCipherView(number = 1)
            val mockCipher = createMockSdkCipher(number = 1, clock = clock)
            val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val mockAttachmentView = createMockAttachmentView(number = 1).copy(
                sizeName = null,
                id = null,
                url = null,
                key = null,
            )
            val mockFile = File.createTempFile("mockFile", "temp")
            val mockAttachment = createMockSdkAttachment(number = 1)
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                fileManager.writeUriToCache(fileUri = mockUri)
            } returns mockFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptAttachment(
                    userId = userId,
                    cipher = mockCipher,
                    attachmentView = mockAttachmentView,
                    decryptedFilePath = mockFile.absolutePath,
                    encryptedFilePath = "${mockFile.absolutePath}.enc",
                )
            } returns mockAttachment.asSuccess()
            coEvery {
                ciphersService.createAttachment(
                    cipherId = cipherId,
                    body = AttachmentJsonRequest(
                        fileName = mockFileName,
                        key = "mockKey-1",
                        fileSize = mockFileSize,
                    ),
                )
            } returns error.asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAttachment with uploadAttachment failure should return CreateAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId-1"
            val mockUri = setupMockUri(url = "www.test.com")
            val mockCipherView = createMockCipherView(number = 1)
            val mockCipher = createMockSdkCipher(number = 1, clock = clock)
            val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val mockAttachmentView = createMockAttachmentView(number = 1).copy(
                sizeName = null,
                id = null,
                url = null,
                key = null,
            )
            val mockFile = File.createTempFile("mockFile", "temp")
            val mockAttachment = createMockSdkAttachment(number = 1)
            val mockAttachmentJsonResponse = createMockAttachmentResponse(number = 1)
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                fileManager.writeUriToCache(fileUri = mockUri)
            } returns mockFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptAttachment(
                    userId = userId,
                    cipher = mockCipher,
                    attachmentView = mockAttachmentView,
                    decryptedFilePath = mockFile.absolutePath,
                    encryptedFilePath = "${mockFile.absolutePath}.enc",
                )
            } returns mockAttachment.asSuccess()
            coEvery {
                ciphersService.createAttachment(
                    cipherId = cipherId,
                    body = AttachmentJsonRequest(
                        fileName = mockFileName,
                        key = "mockKey-1",
                        fileSize = mockFileSize,
                    ),
                )
            } returns mockAttachmentJsonResponse.asSuccess()
            coEvery {
                ciphersService.uploadAttachment(
                    attachment = createMockAttachmentResponse(number = 1),
                    encryptedFile = File("${mockFile.absoluteFile}.enc"),
                )
            } returns error.asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAttachment with decryptCipher failure should return CreateAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId-1"
            val mockUri = setupMockUri(url = "www.test.com")
            val mockCipherView = createMockCipherView(number = 1)
            val mockCipher = createMockSdkCipher(number = 1, clock = clock)
            val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val mockAttachmentView = createMockAttachmentView(number = 1).copy(
                sizeName = null,
                id = null,
                url = null,
                key = null,
            )
            val mockFile = File.createTempFile("mockFile", "temp")
            val mockAttachment = createMockSdkAttachment(number = 1)
            val mockAttachmentJsonResponse = createMockAttachmentResponse(number = 1)
            val mockCipherResponse = createMockCipher(number = 1).copy(collectionIds = null)
            val mockUpdatedCipherResponse = createMockCipher(number = 1).copy(
                collectionIds = listOf("mockId-1"),
            )
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                fileManager.writeUriToCache(fileUri = mockUri)
            } returns mockFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptAttachment(
                    userId = userId,
                    cipher = mockCipher,
                    attachmentView = mockAttachmentView,
                    decryptedFilePath = mockFile.absolutePath,
                    encryptedFilePath = "${mockFile.absolutePath}.enc",
                )
            } returns mockAttachment.asSuccess()
            coEvery {
                ciphersService.createAttachment(
                    cipherId = cipherId,
                    body = AttachmentJsonRequest(
                        fileName = mockFileName,
                        key = "mockKey-1",
                        fileSize = mockFileSize,
                    ),
                )
            } returns mockAttachmentJsonResponse.asSuccess()
            coEvery {
                ciphersService.uploadAttachment(
                    attachment = createMockAttachmentResponse(number = 1),
                    encryptedFile = File("${mockFile.absoluteFile}.enc"),
                )
            } returns mockCipherResponse.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = userId, cipher = mockUpdatedCipherResponse)
            } just runs
            coEvery {
                vaultSdkSource.decryptCipher(
                    userId = userId,
                    cipher = mockUpdatedCipherResponse.toEncryptedSdkCipher(),
                )
            } returns error.asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error(error = error), result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `createAttachment with createAttachment success should return CreateAttachmentResult Success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val cipherId = "cipherId-1"
            val mockUri = setupMockUri(url = "www.test.com")
            val mockCipherView = createMockCipherView(number = 1)
            val mockCipher = createMockSdkCipher(number = 1, clock = clock)
            val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val mockAttachmentView = createMockAttachmentView(number = 1).copy(
                sizeName = null,
                id = null,
                url = null,
                key = null,
            )
            val mockFile = File.createTempFile("mockFile", "temp")
            val mockAttachment = createMockSdkAttachment(number = 1)
            val mockAttachmentJsonResponse = createMockAttachmentResponse(number = 1)
            val mockCipherResponse = createMockCipher(number = 1).copy(collectionIds = null)
            val mockUpdatedCipherResponse = createMockCipher(number = 1).copy(
                collectionIds = listOf("mockId-1"),
            )
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockEncryptionContext.asSuccess()
            coEvery {
                fileManager.writeUriToCache(fileUri = mockUri)
            } returns mockFile.asSuccess()
            coEvery {
                vaultSdkSource.encryptAttachment(
                    userId = userId,
                    cipher = mockCipher,
                    attachmentView = mockAttachmentView,
                    decryptedFilePath = mockFile.absolutePath,
                    encryptedFilePath = "${mockFile.absolutePath}.enc",
                )
            } returns mockAttachment.asSuccess()
            coEvery {
                ciphersService.createAttachment(
                    cipherId = cipherId,
                    body = AttachmentJsonRequest(
                        fileName = mockFileName,
                        key = "mockKey-1",
                        fileSize = mockFileSize,
                    ),
                )
            } returns mockAttachmentJsonResponse.asSuccess()
            coEvery {
                ciphersService.uploadAttachment(
                    attachment = createMockAttachmentResponse(number = 1),
                    encryptedFile = File("${mockFile.absolutePath}.enc"),
                )
            } returns mockCipherResponse.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = userId, cipher = mockUpdatedCipherResponse)
            } just runs
            coEvery {
                vaultSdkSource.decryptCipher(
                    userId = userId,
                    cipher = mockUpdatedCipherResponse.toEncryptedSdkCipher(),
                )
            } returns mockCipherView.asSuccess()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Success(cipherView = mockCipherView), result)
        }

    @Test
    fun `createAttachment should delete temp files after upload success`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val cipherId = "cipherId-1"
        val mockUri = setupMockUri(url = "www.test.com")
        val mockCipherView = createMockCipherView(number = 1)
        val mockCipher = createMockSdkCipher(number = 1, clock = clock)
        val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
        val mockFileName = "mockFileName-1"
        val mockFileSize = "1"
        val mockAttachmentView = createMockAttachmentView(number = 1).copy(
            sizeName = null,
            id = null,
            url = null,
            key = null,
        )
        val mockFile = File.createTempFile("mockFile", "temp")
        val mockAttachment = createMockSdkAttachment(number = 1)
        val mockAttachmentJsonResponse = createMockAttachmentResponse(number = 1)
        val mockCipherResponse = createMockCipher(number = 1).copy(collectionIds = null)
        val mockUpdatedCipherResponse = createMockCipher(number = 1).copy(
            collectionIds = listOf("mockId-1"),
        )
        coEvery {
            vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
        } returns mockEncryptionContext.asSuccess()
        coEvery {
            fileManager.writeUriToCache(fileUri = mockUri)
        } returns mockFile.asSuccess()
        coEvery {
            vaultSdkSource.encryptAttachment(
                userId = userId,
                cipher = mockCipher,
                attachmentView = mockAttachmentView,
                decryptedFilePath = mockFile.absolutePath,
                encryptedFilePath = "${mockFile.absolutePath}.enc",
            )
        } returns mockAttachment.asSuccess()
        coEvery {
            ciphersService.createAttachment(
                cipherId = cipherId,
                body = AttachmentJsonRequest(
                    fileName = mockFileName,
                    key = "mockKey-1",
                    fileSize = mockFileSize,
                ),
            )
        } returns mockAttachmentJsonResponse.asSuccess()
        coEvery {
            ciphersService.uploadAttachment(
                attachment = createMockAttachmentResponse(number = 1),
                encryptedFile = File("${mockFile.absolutePath}.enc"),
            )
        } returns mockCipherResponse.asSuccess()
        coEvery {
            vaultDiskSource.saveCipher(userId = userId, cipher = mockUpdatedCipherResponse)
        } just runs
        coEvery {
            vaultSdkSource.decryptCipher(
                userId = userId,
                cipher = mockUpdatedCipherResponse.toEncryptedSdkCipher(),
            )
        } returns mockCipherView.asSuccess()

        cipherManager.createAttachment(
            cipherId = cipherId,
            cipherView = mockCipherView,
            fileSizeBytes = mockFileSize,
            fileName = mockFileName,
            fileUri = mockUri,
        )

        coVerify(exactly = 1) {
            fileManager.delete(*anyVararg())
        }
    }

    @Test
    fun `createAttachment should delete temp files after upload failure`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        val userId = "mockId-1"
        val cipherId = "cipherId-1"
        val mockUri = setupMockUri(url = "www.test.com")
        val mockCipherView = createMockCipherView(number = 1)
        val mockCipher = createMockSdkCipher(number = 1, clock = clock)
        val mockEncryptionContext = createMockEncryptionContext(number = 1, cipher = mockCipher)
        val mockFileName = "mockFileName-1"
        val mockFileSize = "1"
        val mockAttachmentView = createMockAttachmentView(number = 1).copy(
            sizeName = null,
            id = null,
            url = null,
            key = null,
        )
        val mockFile = File.createTempFile("mockFile", "temp")
        val mockAttachment = createMockSdkAttachment(number = 1)
        val mockAttachmentJsonResponse = createMockAttachmentResponse(number = 1)
        coEvery {
            vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
        } returns mockEncryptionContext.asSuccess()
        coEvery {
            fileManager.writeUriToCache(fileUri = mockUri)
        } returns mockFile.asSuccess()
        coEvery {
            vaultSdkSource.encryptAttachment(
                userId = userId,
                cipher = mockCipher,
                attachmentView = mockAttachmentView,
                decryptedFilePath = mockFile.absolutePath,
                encryptedFilePath = "${mockFile.absolutePath}.enc",
            )
        } returns mockAttachment.asSuccess()
        coEvery {
            ciphersService.createAttachment(
                cipherId = cipherId,
                body = AttachmentJsonRequest(
                    fileName = mockFileName,
                    key = "mockKey-1",
                    fileSize = mockFileSize,
                ),
            )
        } returns mockAttachmentJsonResponse.asSuccess()
        coEvery {
            ciphersService.uploadAttachment(
                attachment = createMockAttachmentResponse(number = 1),
                encryptedFile = File("${mockFile.absolutePath}.enc"),
            )
        } returns Throwable("Fail").asFailure()

        cipherManager.createAttachment(
            cipherId = cipherId,
            cipherView = mockCipherView,
            fileSizeBytes = mockFileSize,
            fileName = mockFileName,
            fileUri = mockUri,
        )

        coVerify(exactly = 1) {
            fileManager.delete(*anyVararg())
        }
    }

    @Test
    fun `downloadAttachment with missing attachment should return Failure`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val attachmentId = "mockId-1"
        val mockCipher = mockk<Cipher> {
            every { key } returns "key"
            every { attachments } returns emptyList()
            every { id } returns "mockId-1"
        }
        val mockEncryptionContext = mockk<EncryptionContext> {
            every { encryptedFor } returns "mockEncryptedFor-1"
            every { cipher } returns mockCipher
        }
        val cipherView = createMockCipherView(number = 1, attachments = emptyList())
        coEvery {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
        } returns mockEncryptionContext.asSuccess()

        assertEquals(
            DownloadAttachmentResult.Failure(IllegalStateException()),
            cipherManager.downloadAttachment(
                cipherView = cipherView,
                attachmentId = attachmentId,
            ),
        )

        coVerify(exactly = 1) {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
        }
        coVerify(exactly = 0) {
            ciphersService.getCipherAttachment(cipherId = any(), attachmentId = any())
        }
    }

    @Test
    fun `downloadAttachment with failed attachment details request should return Failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val attachmentId = "mockId-1"
            val attachment = mockk<Attachment> {
                every { id } returns attachmentId
            }
            val mockCipher = mockk<Cipher> {
                every { key } returns "key"
                every { attachments } returns listOf(attachment)
                every { id } returns "mockId-1"
            }
            val mockEncryptionContext = mockk<EncryptionContext> {
                every { encryptedFor } returns "mockEncryptedFor-1"
                every { cipher } returns mockCipher
            }
            val cipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
            } returns mockEncryptionContext.asSuccess()
            val error = Throwable()

            coEvery {
                ciphersService.getCipherAttachment(cipherId = any(), attachmentId = any())
            } returns error.asFailure()

            assertEquals(
                DownloadAttachmentResult.Failure(error = error),
                cipherManager.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = attachmentId,
                ),
            )

            coVerify(exactly = 1) {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
                ciphersService.getCipherAttachment(
                    cipherId = requireNotNull(cipherView.id),
                    attachmentId = attachmentId,
                )
            }
        }

    @Test
    fun `downloadAttachment with attachment details missing url should return Failure`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val attachmentId = "mockId-1"
        val attachment = mockk<Attachment> {
            every { id } returns attachmentId
        }
        val mockCipher = mockk<Cipher> {
            every { key } returns "key"
            every { attachments } returns listOf(attachment)
            every { id } returns "mockId-1"
        }
        val mockEncryptionContext = mockk<EncryptionContext> {
            every { encryptedFor } returns "mockEncryptedFor-1"
            every { cipher } returns mockCipher
        }
        val cipherView = createMockCipherView(number = 1)
        coEvery {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
        } returns mockEncryptionContext.asSuccess()

        val response = mockk<SyncResponseJson.Cipher.Attachment> {
            every { url } returns null
        }
        coEvery {
            ciphersService.getCipherAttachment(any(), any())
        } returns response.asSuccess()

        assertEquals(
            DownloadAttachmentResult.Failure(error = IllegalStateException()),
            cipherManager.downloadAttachment(
                cipherView = cipherView,
                attachmentId = attachmentId,
            ),
        )

        coVerify(exactly = 1) {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
            ciphersService.getCipherAttachment(
                cipherId = requireNotNull(cipherView.id),
                attachmentId = attachmentId,
            )
        }
    }

    @Test
    fun `downloadAttachment with failed download should return Failure`() = runTest {
        fakeAuthDiskSource.userState = MOCK_USER_STATE

        val attachmentId = "mockId-1"
        val attachment = mockk<Attachment> {
            every { id } returns attachmentId
        }
        val mockCipher = mockk<Cipher> {
            every { key } returns "key"
            every { attachments } returns listOf(attachment)
            every { id } returns "mockId-1"
        }
        val mockEncryptionContext = mockk<EncryptionContext> {
            every { encryptedFor } returns "mockEncryptedFor-1"
            every { cipher } returns mockCipher
        }

        val cipherView = createMockCipherView(number = 1)
        coEvery {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
        } returns mockEncryptionContext.asSuccess()

        val response = mockk<SyncResponseJson.Cipher.Attachment> {
            every { url } returns "https://bitwarden.com"
        }
        coEvery {
            ciphersService.getCipherAttachment(cipherId = any(), attachmentId = any())
        } returns response.asSuccess()
        coEvery {
            fileManager.downloadFileToCache(url = any())
        } returns DownloadResult.Failure(error = Throwable("Fail!"))

        assertEquals(
            DownloadAttachmentResult.Failure(IllegalStateException()),
            cipherManager.downloadAttachment(
                cipherView = cipherView,
                attachmentId = attachmentId,
            ),
        )

        coVerify(exactly = 1) {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
            ciphersService.getCipherAttachment(
                cipherId = requireNotNull(cipherView.id),
                attachmentId = attachmentId,
            )
            fileManager.downloadFileToCache("https://bitwarden.com")
        }
    }

    @Test
    fun `downloadAttachment with failed decryption should delete file and return Failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val attachmentId = "mockId-1"
            val attachment = mockk<Attachment> {
                every { id } returns attachmentId
            }
            val mockCipher = mockk<Cipher> {
                every { key } returns "key"
                every { attachments } returns listOf(attachment)
                every { id } returns "mockId-1"
            }
            val mockEncryptionContext = mockk<EncryptionContext> {
                every { encryptedFor } returns "mockEncryptedFor-1"
                every { cipher } returns mockCipher
            }
            val attachmentView = createMockAttachmentView(number = 1)
            val cipherView = createMockCipherView(number = 1, attachments = listOf(attachmentView))
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
            } returns mockEncryptionContext.asSuccess()

            val response = mockk<SyncResponseJson.Cipher.Attachment> {
                every { url } returns "https://bitwarden.com"
            }
            coEvery {
                ciphersService.getCipherAttachment(cipherId = any(), attachmentId = any())
            } returns response.asSuccess()

            val file = mockk<File> {
                every { path } returns "path/to/encrypted/file"
            }
            coEvery { fileManager.delete(file) } just runs
            coEvery {
                fileManager.downloadFileToCache(url = any())
            } returns DownloadResult.Success(file)
            val error = Throwable("Fail")
            coEvery {
                vaultSdkSource.decryptFile(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipher = mockCipher,
                    attachmentView = attachmentView,
                    encryptedFilePath = "path/to/encrypted/file",
                    decryptedFilePath = "path/to/encrypted/file_decrypted",
                )
            } returns error.asFailure()

            assertEquals(
                DownloadAttachmentResult.Failure(error = error),
                cipherManager.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = attachmentId,
                ),
            )

            coVerify(exactly = 1) {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
                ciphersService.getCipherAttachment(
                    cipherId = requireNotNull(cipherView.id),
                    attachmentId = attachmentId,
                )
                fileManager.downloadFileToCache("https://bitwarden.com")
                vaultSdkSource.decryptFile(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipher = mockCipher,
                    attachmentView = attachmentView,
                    encryptedFilePath = "path/to/encrypted/file",
                    decryptedFilePath = "path/to/encrypted/file_decrypted",
                )
                fileManager.delete(file)
            }
        }

    @Test
    fun `downloadAttachment with successful decryption should delete file and return Success`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            val attachmentId = "mockId-1"
            val attachment = mockk<Attachment> {
                every { id } returns attachmentId
            }
            val mockCipher = mockk<Cipher> {
                every { key } returns "key"
                every { attachments } returns listOf(attachment)
                every { id } returns "mockId-1"
            }
            val mockEncryptionContext = mockk<EncryptionContext> {
                every { encryptedFor } returns "mockEncryptedFor-1"
                every { cipher } returns mockCipher
            }
            val attachmentView = mockk<AttachmentView> {
                every { id } returns attachmentId
            }
            val cipherView = createMockCipherView(number = 1, attachments = listOf(attachmentView))
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
            } returns mockEncryptionContext.asSuccess()

            val response = mockk<SyncResponseJson.Cipher.Attachment> {
                every { url } returns "https://bitwarden.com"
            }
            coEvery {
                ciphersService.getCipherAttachment(any(), any())
            } returns response.asSuccess()

            val file = mockk<File> {
                every { path } returns "path/to/encrypted/file"
            }
            coEvery { fileManager.delete(file) } just runs
            coEvery {
                fileManager.downloadFileToCache(any())
            } returns DownloadResult.Success(file)

            coEvery {
                vaultSdkSource.decryptFile(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipher = mockCipher,
                    attachmentView = attachmentView,
                    encryptedFilePath = "path/to/encrypted/file",
                    decryptedFilePath = "path/to/encrypted/file_decrypted",
                )
            } returns Unit.asSuccess()

            assertEquals(
                DownloadAttachmentResult.Success(
                    file = File("path/to/encrypted/file_decrypted"),
                ),
                cipherManager.downloadAttachment(
                    cipherView = cipherView,
                    attachmentId = attachmentId,
                ),
            )

            coVerify(exactly = 1) {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
                ciphersService.getCipherAttachment(
                    cipherId = requireNotNull(cipherView.id),
                    attachmentId = attachmentId,
                )
                fileManager.downloadFileToCache(url = "https://bitwarden.com")
                vaultSdkSource.decryptFile(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipher = mockCipher,
                    attachmentView = attachmentView,
                    encryptedFilePath = "path/to/encrypted/file",
                    decryptedFilePath = "path/to/encrypted/file_decrypted",
                )
                fileManager.delete(file)
            }
        }

    @Test
    fun `syncCipherDeleteFlow should delete cipher from disk`() {
        val userId = "mockId-1"
        val cipherId = "mockId-1"

        coEvery { vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId) } just runs

        mutableSyncCipherDeleteFlow.tryEmit(
            SyncCipherDeleteData(userId = userId, cipherId = cipherId),
        )

        coVerify { vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow create with local cipher with no common collections should do nothing`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-$number"
            val originalCipher = createMockCipher(
                number = number,
                revisionDate = ZonedDateTime.now(clock).minus(5, ChronoUnit.MINUTES),
            )

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
            } returns originalCipher

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    userId = userId,
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(clock),
                    isUpdate = false,
                    collectionIds = null,
                    organizationId = null,
                ),
            )

            coVerify(exactly = 1) {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
            }
            coVerify(exactly = 0) {
                ciphersService.getCipher(cipherId = any())
                vaultDiskSource.saveCipher(userId = any(), cipher = any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow create with local cipher, and with common collections, should make a request and save cipher to disk`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-$number"
            val collection = createMockCollection(number = number)
            val originalCipher = createMockCipher(
                number = number,
                revisionDate = ZonedDateTime.now(clock).minus(5, ChronoUnit.MINUTES),
            )
            val updatedCipher = mockk<SyncResponseJson.Cipher>()

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
            } returns originalCipher
            coEvery {
                vaultDiskSource.getCollections(userId = userId)
            } returns MutableStateFlow(listOf(collection))
            coEvery {
                ciphersService.getCipher(cipherId = cipherId)
            } returns updatedCipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            } just runs

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    userId = userId,
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(clock),
                    isUpdate = false,
                    collectionIds = listOf("mockId-1"),
                    organizationId = "mock-id",
                ),
            )

            coVerify(exactly = 1) {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
                ciphersService.getCipher(cipherId = cipherId)
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow update with no local cipher, but with common collections, should make a request save cipher to disk`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val number = 1
            val cipherId = "mockId-$number"
            val originalCipher = createMockCipher(
                number = number,
                revisionDate = ZonedDateTime.now(clock).minus(5, ChronoUnit.MINUTES),
            )
            val updatedCipher = mockk<SyncResponseJson.Cipher>()
            val collection = createMockCollection(number = number)

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { vaultDiskSource.getCipher(any(), any()) } returns originalCipher
            coEvery {
                vaultDiskSource.getCollections(userId = userId)
            } returns MutableStateFlow(listOf(collection))

            coEvery { ciphersService.getCipher(cipherId) } returns updatedCipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            } just runs

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    userId = userId,
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(clock),
                    isUpdate = true,
                    collectionIds = listOf("mockId-1"),
                    organizationId = "mock-id",
                ),
            )

            coVerify(exactly = 1) {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
                ciphersService.getCipher(cipherId = cipherId)
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            }
        }

    @Test
    fun `syncCipherUpsertFlow update with no local cipher should do nothing`() = runTest {
        val number = 1
        val userId = MOCK_USER_STATE.activeUserId
        val cipherId = "mockId-$number"

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery { vaultDiskSource.getCipher(userId = userId, cipherId = cipherId) } returns null

        mutableSyncCipherUpsertFlow.tryEmit(
            SyncCipherUpsertData(
                userId = userId,
                cipherId = cipherId,
                revisionDate = ZonedDateTime.now(clock),
                isUpdate = true,
                collectionIds = null,
                organizationId = null,
            ),
        )

        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
        }
        coVerify(exactly = 0) {
            ciphersService.getCipher(cipherId = any())
            vaultDiskSource.saveCipher(userId = any(), cipher = any())
        }
    }

    @Test
    fun `syncCipherUpsertFlow update with more recent local cipher should do nothing`() = runTest {
        val number = 1
        val userId = MOCK_USER_STATE.activeUserId
        val cipherId = "mockId-$number"
        val originalCipher = createMockCipher(
            number = number,
            revisionDate = ZonedDateTime.now(clock),
        )

        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
        } returns originalCipher

        mutableSyncCipherUpsertFlow.tryEmit(
            SyncCipherUpsertData(
                userId = userId,
                cipherId = cipherId,
                revisionDate = ZonedDateTime.now(clock).minus(5, ChronoUnit.MINUTES),
                isUpdate = true,
                collectionIds = null,
                organizationId = null,
            ),
        )

        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
        }
        coVerify(exactly = 0) {
            ciphersService.getCipher(cipherId = any())
            vaultDiskSource.saveCipher(userId = any(), cipher = any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow update failure with 404 code should make a request for a cipher and then delete it`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-$number"

            coEvery {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
            } returns createMockCipher(number = number)
            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery { ciphersService.getCipher(cipherId = cipherId) } returns response.asFailure()
            coEvery { vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId) } just runs
            fakeAuthDiskSource.userState = MOCK_USER_STATE

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    userId = userId,
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(clock),
                    isUpdate = true,
                    collectionIds = null,
                    organizationId = null,
                ),
            )

            coVerify(exactly = 1) {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
                ciphersService.getCipher(cipherId = cipherId)
                vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow create failure with 404 code should make a request for a cipher and do nothing`() =
        runTest {
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-1"

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val response: HttpException = mockk {
                every { code() } returns 404
            }
            coEvery { ciphersService.getCipher(cipherId = cipherId) } returns response.asFailure()
            coEvery { vaultDiskSource.getCipher(userId = userId, cipherId = cipherId) } returns null

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    userId = userId,
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(clock),
                    isUpdate = false,
                    collectionIds = null,
                    organizationId = null,
                ),
            )

            coVerify(exactly = 1) {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
                ciphersService.getCipher(cipherId = cipherId)
            }
            coVerify(exactly = 0) {
                vaultDiskSource.deleteCipher(userId = any(), cipherId = any())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow valid create success should make a request for a cipher and then store it`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-$number"
            val updatedCipher = mockk<SyncResponseJson.Cipher>()

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
            } returns null
            coEvery {
                ciphersService.getCipher(cipherId = cipherId)
            } returns updatedCipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            } just runs

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    userId = userId,
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(clock),
                    isUpdate = false,
                    collectionIds = null,
                    organizationId = null,
                ),
            )

            coVerify(exactly = 1) {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
                ciphersService.getCipher(cipherId = cipherId)
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `syncCipherUpsertFlow valid update success should make a request for a cipher and then store it`() =
        runTest {
            val number = 1
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-$number"
            val originalCipher = mockk<SyncResponseJson.Cipher> {
                every { revisionDate } returns ZonedDateTime.now(clock).minus(5, ChronoUnit.MINUTES)
            }
            val updatedCipher = mockk<SyncResponseJson.Cipher>()

            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
            } returns originalCipher
            coEvery { ciphersService.getCipher(cipherId) } returns updatedCipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            } just runs

            mutableSyncCipherUpsertFlow.tryEmit(
                SyncCipherUpsertData(
                    userId = userId,
                    cipherId = cipherId,
                    revisionDate = ZonedDateTime.now(clock),
                    isUpdate = true,
                    collectionIds = null,
                    organizationId = null,
                ),
            )

            coVerify(exactly = 1) {
                vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
                ciphersService.getCipher(cipherId)
                vaultDiskSource.saveCipher(userId = userId, cipher = updatedCipher)
            }
        }

    @Test
    fun `syncCipherUpsertFlow with inactive userId should clear the last sync time`() = runTest {
        val number = 1
        val userId = "nonActiveUserId"
        val cipherId = "mockId-$number"
        val originalCipher = mockk<SyncResponseJson.Cipher> {
            every { revisionDate } returns ZonedDateTime.now(clock).minus(5, ChronoUnit.MINUTES)
        }
        val lastSyncTime = clock.instant()

        fakeSettingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = lastSyncTime)
        fakeAuthDiskSource.userState = MOCK_USER_STATE
        coEvery {
            vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
        } returns originalCipher

        mutableSyncCipherUpsertFlow.tryEmit(
            SyncCipherUpsertData(
                userId = userId,
                cipherId = cipherId,
                revisionDate = ZonedDateTime.now(clock),
                isUpdate = true,
                collectionIds = null,
                organizationId = null,
            ),
        )

        fakeSettingsDiskSource.assertLastSyncTime(userId = userId, expected = null)
        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
        }
        coVerify(exactly = 0) {
            ciphersService.getCipher(cipherId)
            vaultDiskSource.saveCipher(userId = userId, cipher = any())
        }
    }

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
}

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
