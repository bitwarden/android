package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import androidx.core.net.toUri
import com.bitwarden.vault.Attachment
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.AttachmentJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateCipherInOrganizationJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherCollectionsJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockAttachment
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockAttachmentJsonResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipher
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockAttachmentView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkAttachment
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.manager.model.DownloadResult
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
import io.mockk.coEvery
import io.mockk.coVerify
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

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
    private val ciphersService: CiphersService = mockk()
    private val vaultDiskSource: VaultDiskSource = mockk()
    private val vaultSdkSource: VaultSdkSource = mockk()
    private val reviewPromptManager: ReviewPromptManager = mockk {
        every { registerAddCipherAction() } just runs
    }

    private val cipherManager: CipherManager = CipherManagerImpl(
        ciphersService = ciphersService,
        vaultDiskSource = vaultDiskSource,
        vaultSdkSource = vaultSdkSource,
        authDiskSource = fakeAuthDiskSource,
        fileManager = fileManager,
        clock = clock,
        reviewPromptManager = reviewPromptManager,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(Uri::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Uri::class, Instant::class)
        unmockkStatic(Cipher::toEncryptedNetworkCipherResponse)
    }

    @Test
    fun `createCipher with no active user should return CreateCipherResult failure`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.createCipher(cipherView = mockk())

        assertEquals(CreateCipherResult.Error, result)
    }

    @Test
    fun `createCipher with encryptCipher failure should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns IllegalStateException().asFailure()

            val result = cipherManager.createCipher(cipherView = mockCipherView)

            assertEquals(CreateCipherResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipher with ciphersService createCipher failure should return CreateCipherResult failure`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(number = 1, hasNullUri = true),
                )
            } returns IllegalStateException().asFailure()

            val result = cipherManager.createCipher(cipherView = mockCipherView)

            assertEquals(CreateCipherResult.Error, result)
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.createCipher(
                    body = createMockCipherJsonRequest(number = 1, hasNullUri = true),
                )
            } returns mockCipher.asSuccess()
            coEvery { vaultDiskSource.saveCipher(userId, mockCipher) } just runs

            val result = cipherManager.createCipher(cipherView = mockCipherView)

            assertEquals(CreateCipherResult.Success, result)
            verify(exactly = 1) { reviewPromptManager.registerAddCipherAction() }
        }

    @Test
    fun `createCipherInOrganization with no active user should return CreateCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = null

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockk(),
                collectionIds = mockk(),
            )

            assertEquals(CreateCipherResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipherInOrganization with encryptCipher failure should return CreateCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns IllegalStateException().asFailure()

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockCipherView,
                collectionIds = mockk(),
            )

            assertEquals(CreateCipherResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `createCipherInOrganization with ciphersService createCipher failure should return CreateCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val mockCipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = mockCipherView,
                )
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.createCipherInOrganization(
                    body = CreateCipherInOrganizationJsonRequest(
                        cipher = createMockCipherJsonRequest(number = 1, hasNullUri = true),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns IllegalStateException().asFailure()

            val result = cipherManager.createCipherInOrganization(
                cipherView = mockCipherView,
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(CreateCipherResult.Error, result)
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.createCipherInOrganization(
                    body = CreateCipherInOrganizationJsonRequest(
                        cipher = createMockCipherJsonRequest(number = 1, hasNullUri = true),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns mockCipher.asSuccess()
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
            verify(exactly = 1) { reviewPromptManager.registerAddCipherAction() }
        }

    @Test
    fun `updateCipher with no active user should return UpdateCipherResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.updateCipher(
            cipherId = "cipherId",
            cipherView = mockk(),
        )

        assertEquals(UpdateCipherResult.Error(errorMessage = null), result)
    }

    @Test
    fun `updateCipher with encryptCipher failure should return UpdateCipherResult failure`() =
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
            } returns IllegalStateException().asFailure()

            val result = cipherManager.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Error(errorMessage = null), result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipher with ciphersService updateCipher failure should return UpdateCipherResult Error with a null message`() =
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(number = 1, hasNullUri = true),
                )
            } returns IllegalStateException().asFailure()

            val result = cipherManager.updateCipher(
                cipherId = cipherId,
                cipherView = mockCipherView,
            )

            assertEquals(UpdateCipherResult.Error(errorMessage = null), result)
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(number = 1, hasNullUri = true),
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
                UpdateCipherResult.Error(errorMessage = "You do not have permission to edit this."),
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            val mockCipher = createMockCipher(number = 1)
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = createMockCipherJsonRequest(number = 1, hasNullUri = true),
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
    fun `hardDeleteCipher with no active user should return DeleteCipherResult Error`() = runTest {
        fakeAuthDiskSource.userState = null

        val result = cipherManager.hardDeleteCipher(
            cipherId = "cipherId",
        )

        assertEquals(DeleteCipherResult.Error, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `hardDeleteCipher with ciphersService hardDeleteCipher failure should return DeleteCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherId = "mockId-1"
            coEvery {
                ciphersService.hardDeleteCipher(cipherId = cipherId)
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.hardDeleteCipher(cipherId)

            assertEquals(DeleteCipherResult.Error, result)
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

        assertEquals(DeleteCipherResult.Error, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `softDeleteCipher with ciphersService softDeleteCipher failure should return DeleteCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = MOCK_USER_STATE.activeUserId
            val cipherId = "mockId-1"
            val cipherView = createMockCipherView(number = 1)
            val cipher = createMockSdkCipher(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = cipherView)
            } returns cipher.asSuccess()
            coEvery {
                ciphersService.softDeleteCipher(cipherId = cipherId)
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.softDeleteCipher(
                cipherId = cipherId,
                cipherView = cipherView,
            )

            assertEquals(DeleteCipherResult.Error, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `softDeleteCipher with ciphersService softDeleteCipher success should return DeleteCipherResult success`() =
        runTest {
            val fixedInstant = Instant.parse("2023-10-27T12:00:00Z")
            val userId = "mockId-1"
            val cipherId = "mockId-1"
            val cipher = createMockSdkCipher(number = 1, clock = clock)
            val cipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = cipherView)
            } returns cipher.asSuccess()
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = cipherView.copy(deletedDate = fixedInstant),
                )
            } returns cipher.asSuccess()
            coEvery {
                vaultSdkSource.decryptCipher(userId = userId, cipher = cipher)
            } returns cipherView.asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { ciphersService.softDeleteCipher(cipherId = cipherId) } returns Unit.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = cipher.toEncryptedNetworkCipherResponse(),
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
            val cipher = createMockSdkCipher(number = 1, clock = clock)
            val cipherView = createMockCipherView(number = 1).copy(key = null)
            val networkCipher = createMockCipher(number = 1).copy(key = null)
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = cipherView)
            } returns cipher.asSuccess()
            coEvery {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = cipher.toEncryptedNetworkCipher(),
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
            } returns cipher.asSuccess()
            coEvery {
                vaultSdkSource.decryptCipher(userId = userId, cipher = cipher)
            } returns cipherView.asSuccess()
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            coEvery { ciphersService.softDeleteCipher(cipherId = cipherId) } returns Unit.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = cipher.toEncryptedNetworkCipherResponse(),
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
                    body = cipher.toEncryptedNetworkCipher(),
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

            assertEquals(DeleteAttachmentResult.Error, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `deleteCipherAttachment with ciphersService deleteCipherAttachment failure should return DeleteAttachmentResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherId = "mockId-1"
            val attachmentId = "mockId-1"
            coEvery {
                ciphersService.deleteCipherAttachment(
                    cipherId = cipherId,
                    attachmentId = attachmentId,
                )
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
                cipherView = createMockCipherView(number = 1),
            )

            assertEquals(DeleteAttachmentResult.Error, result)
        }

    @Suppress("MaxLineLength")
    @Test
    fun `deleteCipherAttachment with ciphersService deleteCipherAttachment success should return DeleteAttachmentResult success`() =
        runTest {
            mockkStatic(Cipher::toEncryptedNetworkCipherResponse)
            every {
                createMockSdkCipher(number = 1, clock = clock).toEncryptedNetworkCipherResponse()
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
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

        assertEquals(RestoreCipherResult.Error, result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `restoreCipher with ciphersService restoreCipher failure should return RestoreCipherResult Error`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val cipherId = "mockId-1"
            val cipherView = createMockCipherView(number = 1)
            coEvery {
                ciphersService.restoreCipher(cipherId = cipherId)
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.restoreCipher(
                cipherId = cipherId,
                cipherView = cipherView,
            )

            assertEquals(RestoreCipherResult.Error, result)
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

        assertEquals(ShareCipherResult.Error, result)
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.shareCipher(
                    cipherId = "mockId-1",
                    body = ShareCipherJsonRequest(
                        cipher = createMockCipherJsonRequest(number = 1, hasNullUri = true),
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
            val attachment = createMockAttachment(number = 1)
            val encryptedFile = File("path/to/encrypted/file")
            val decryptedFile = File("path/to/encrypted/file_decrypted")
            val mockCipherView = createMockCipherView(number = 1)
            val mockAttachmentJsonResponse = createMockAttachmentJsonResponse(number = 1)
            val mockNetworkCipher = createMockCipher(number = 1)

            // Handle mocks for migration
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = initialCipherView)
            } returns mockCipher.asSuccess()
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
                    attachment = mockAttachment,
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
                    attachmentJsonResponse = mockAttachmentJsonResponse,
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
            } returns mockCipher.asSuccess()
            coEvery {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = mockCipher.toEncryptedNetworkCipherResponse(),
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.shareCipher(
                    cipherId = "mockId-1",
                    body = ShareCipherJsonRequest(
                        cipher = createMockCipherJsonRequest(number = 1, hasNullUri = true),
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.shareCipher(
                    cipherId = "mockId-1",
                    body = ShareCipherJsonRequest(
                        cipher = createMockCipherJsonRequest(number = 1, hasNullUri = true),
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns Throwable("Fail").asFailure()
            coEvery { vaultDiskSource.saveCipher(userId, createMockCipher(number = 1)) } just runs

            val result = cipherManager.shareCipher(
                cipherId = "mockId-1",
                organizationId = organizationId,
                cipherView = createMockCipherView(number = 1),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `shareCipher with cipherService encryptCipher failure should return ShareCipherResultError`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            val organizationId = "organizationId"
            coEvery {
                vaultSdkSource.moveToOrganization(
                    userId = userId,
                    organizationId = organizationId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns Throwable("Fail").asFailure()
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

            assertEquals(ShareCipherResult.Error, result)
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

            assertEquals(ShareCipherResult.Error, result)
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
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
            } returns createMockSdkCipher(number = 1, clock = clock).asSuccess()
            coEvery {
                ciphersService.updateCipherCollections(
                    cipherId = "mockId-1",
                    body = UpdateCipherCollectionsJsonRequest(
                        collectionIds = listOf("mockId-1"),
                    ),
                )
            } returns Throwable("Fail").asFailure()
            coEvery { vaultDiskSource.saveCipher(userId, any()) } just runs

            val result = cipherManager.updateCipherCollections(
                cipherId = "mockId-1",
                cipherView = createMockCipherView(number = 1).copy(
                    collectionIds = listOf("mockId-1"),
                ),
                collectionIds = listOf("mockId-1"),
            )

            assertEquals(ShareCipherResult.Error, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `updateCipherCollections with updateCipherCollections encryptCipher failure should return ShareCipherResultError`() =
        runTest {
            fakeAuthDiskSource.userState = MOCK_USER_STATE
            val userId = "mockId-1"
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = createMockCipherView(number = 1),
                )
            } returns Throwable("Fail").asFailure()
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

            assertEquals(ShareCipherResult.Error, result)
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

            assertEquals(CreateAttachmentResult.Error, result)
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
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error, result)
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
            val mockFile = File.createTempFile("mockFile", "temp")
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            val mockAttachmentView = createMockAttachmentView(number = 1).copy(
                sizeName = null,
                id = null,
                url = null,
                key = null,
            )
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockCipher.asSuccess()
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
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error, result)
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
            val mockFileName = "mockFileName-1"
            val mockFileSize = "1"
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockCipher.asSuccess()
            coEvery {
                fileManager.writeUriToCache(fileUri = mockUri)
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error, result)
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
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockCipher.asSuccess()
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
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error, result)
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
            val mockAttachmentJsonResponse = createMockAttachmentJsonResponse(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockCipher.asSuccess()
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
                    attachmentJsonResponse = mockAttachmentJsonResponse,
                    encryptedFile = File("${mockFile.absoluteFile}.enc"),
                )
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error, result)
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
            val mockAttachmentJsonResponse = createMockAttachmentJsonResponse(number = 1)
            val mockCipherResponse = createMockCipher(number = 1).copy(collectionIds = null)
            val mockUpdatedCipherResponse = createMockCipher(number = 1).copy(
                collectionIds = listOf("mockId-1"),
            )
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockCipher.asSuccess()
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
                    attachmentJsonResponse = mockAttachmentJsonResponse,
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
            } returns Throwable("Fail").asFailure()

            val result = cipherManager.createAttachment(
                cipherId = cipherId,
                cipherView = mockCipherView,
                fileSizeBytes = mockFileSize,
                fileName = mockFileName,
                fileUri = mockUri,
            )

            assertEquals(CreateAttachmentResult.Error, result)
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
            val mockAttachmentJsonResponse = createMockAttachmentJsonResponse(number = 1)
            val mockCipherResponse = createMockCipher(number = 1).copy(collectionIds = null)
            val mockUpdatedCipherResponse = createMockCipher(number = 1).copy(
                collectionIds = listOf("mockId-1"),
            )
            coEvery {
                vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
            } returns mockCipher.asSuccess()
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
                    attachmentJsonResponse = mockAttachmentJsonResponse,
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
        val mockAttachmentJsonResponse = createMockAttachmentJsonResponse(number = 1)
        val mockCipherResponse = createMockCipher(number = 1).copy(collectionIds = null)
        val mockUpdatedCipherResponse = createMockCipher(number = 1).copy(
            collectionIds = listOf("mockId-1"),
        )
        coEvery {
            vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
        } returns mockCipher.asSuccess()
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
                attachmentJsonResponse = mockAttachmentJsonResponse,
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
        val mockAttachmentJsonResponse = createMockAttachmentJsonResponse(number = 1)
        coEvery {
            vaultSdkSource.encryptCipher(userId = userId, cipherView = mockCipherView)
        } returns mockCipher.asSuccess()
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
                attachmentJsonResponse = mockAttachmentJsonResponse,
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
        val cipher = mockk<Cipher> {
            every { attachments } returns emptyList()
            every { id } returns "mockId-1"
        }
        val cipherView = createMockCipherView(number = 1)
        coEvery {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
        } returns cipher.asSuccess()

        assertEquals(
            DownloadAttachmentResult.Failure,
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
            val cipher = mockk<Cipher> {
                every { attachments } returns listOf(attachment)
                every { id } returns "mockId-1"
            }
            val cipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
            } returns cipher.asSuccess()

            coEvery {
                ciphersService.getCipherAttachment(cipherId = any(), attachmentId = any())
            } returns Throwable().asFailure()

            assertEquals(
                DownloadAttachmentResult.Failure,
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
        val cipher = mockk<Cipher> {
            every { attachments } returns listOf(attachment)
            every { id } returns "mockId-1"
        }
        val cipherView = createMockCipherView(number = 1)
        coEvery {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
        } returns cipher.asSuccess()

        val response = mockk<SyncResponseJson.Cipher.Attachment> {
            every { url } returns null
        }
        coEvery {
            ciphersService.getCipherAttachment(any(), any())
        } returns response.asSuccess()

        assertEquals(
            DownloadAttachmentResult.Failure,
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
        val cipher = mockk<Cipher> {
            every { attachments } returns listOf(attachment)
            every { id } returns "mockId-1"
        }

        val cipherView = createMockCipherView(number = 1)
        coEvery {
            vaultSdkSource.encryptCipher(
                userId = MOCK_USER_STATE.activeUserId,
                cipherView = cipherView,
            )
        } returns cipher.asSuccess()

        val response = mockk<SyncResponseJson.Cipher.Attachment> {
            every { url } returns "https://bitwarden.com"
        }
        coEvery {
            ciphersService.getCipherAttachment(cipherId = any(), attachmentId = any())
        } returns response.asSuccess()

        coEvery {
            fileManager.downloadFileToCache(url = any())
        } returns DownloadResult.Failure

        assertEquals(
            DownloadAttachmentResult.Failure,
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
            val cipher = mockk<Cipher> {
                every { attachments } returns listOf(attachment)
                every { id } returns "mockId-1"
            }
            val cipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
            } returns cipher.asSuccess()

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

            coEvery {
                vaultSdkSource.decryptFile(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipher = cipher,
                    attachment = attachment,
                    encryptedFilePath = "path/to/encrypted/file",
                    decryptedFilePath = "path/to/encrypted/file_decrypted",
                )
            } returns Throwable().asFailure()

            assertEquals(
                DownloadAttachmentResult.Failure,
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
                    cipher = cipher,
                    attachment = attachment,
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
            val cipher = mockk<Cipher> {
                every { attachments } returns listOf(attachment)
                every { id } returns "mockId-1"
            }
            val cipherView = createMockCipherView(number = 1)
            coEvery {
                vaultSdkSource.encryptCipher(
                    userId = MOCK_USER_STATE.activeUserId,
                    cipherView = cipherView,
                )
            } returns cipher.asSuccess()

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
                    cipher = cipher,
                    attachment = attachment,
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
                    cipher = cipher,
                    attachment = attachment,
                    encryptedFilePath = "path/to/encrypted/file",
                    decryptedFilePath = "path/to/encrypted/file_decrypted",
                )
                fileManager.delete(file)
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
