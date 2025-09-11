package com.x8bit.bitwarden.data.vault.manager

import androidx.credentials.providerevents.exception.ImportCredentialsUnknownErrorException
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.ImportCiphersJsonRequest
import com.bitwarden.network.model.ImportCiphersResponseJson
import com.bitwarden.network.service.CiphersService
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.manager.model.ImportCxfPayloadResult
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class CredentialExchangeImportManagerTest {

    private val vaultSdkSource: VaultSdkSource = mockk()
    private val ciphersService: CiphersService = mockk()
    private val syncManager: VaultSyncManager = mockk()

    private lateinit var importManager: CredentialExchangeImportManagerImpl

    private val testUserId = "test-user-id"
    private val testPayload = "test-cxf-payload"
    private val mockSdkCipher: Cipher = createMockSdkCipher(number = 1)
    private val mockSdkCipherList: List<Cipher> = listOf(mockSdkCipher)

    @BeforeEach
    fun setUp() {
        importManager = CredentialExchangeImportManagerImpl(
            vaultSdkSource = vaultSdkSource,
            ciphersService = ciphersService,
            syncManager = syncManager,
        )
    }

    @Test
    fun `when vaultSdkSource importCxf fails, should return Error`() = runTest {
        val exception = RuntimeException("SDK import failed")
        coEvery {
            vaultSdkSource.importCxf(
                userId = testUserId,
                payload = testPayload,
            )
        } returns exception.asFailure()

        val result = importManager.importCxfPayload(testUserId, testPayload)

        assertEquals(ImportCxfPayloadResult.Error(exception), result)
        coVerify(exactly = 1) { vaultSdkSource.importCxf(testUserId, testPayload) }
        coVerify(exactly = 0) { ciphersService.importCiphers(any()) }
        coVerify(exactly = 0) { syncManager.sync(any(), any()) }
    }

    @Test
    fun `when ciphersService importCiphers fails, should return Error`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = testUserId,
                payload = testPayload,
            )
        } returns mockSdkCipherList.asSuccess()

        val exception = RuntimeException("Network import failed")
        val capturedRequest = slot<ImportCiphersJsonRequest>()
        coEvery {
            ciphersService.importCiphers(capture(capturedRequest))
        } returns exception.asFailure()

        val result = importManager.importCxfPayload(testUserId, testPayload)

        assertEquals(ImportCxfPayloadResult.Error(exception), result)
        assertEquals(1, capturedRequest.captured.ciphers.size)
        coVerify(exactly = 1) { vaultSdkSource.importCxf(testUserId, testPayload) }
        coVerify(exactly = 1) { ciphersService.importCiphers(any()) }
        coVerify(exactly = 0) { syncManager.sync(any(), any()) }
    }

    @Test
    fun `when ciphersService importCiphers returns Invalid, should return Error`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = testUserId,
                payload = testPayload,
            )
        } returns mockSdkCipherList.asSuccess()

        coEvery {
            ciphersService.importCiphers(any())
        } returns ImportCiphersResponseJson
            .Invalid(validationErrors = emptyMap())
            .asSuccess()

        val result = importManager.importCxfPayload(testUserId, testPayload)

        val error = (result as? ImportCxfPayloadResult.Error)?.error
        assertNotNull(error)
        assertTrue(error is ImportCredentialsUnknownErrorException)
        coVerify(exactly = 1) { vaultSdkSource.importCxf(testUserId, testPayload) }
        coVerify(exactly = 1) { ciphersService.importCiphers(any()) }
        coVerify(exactly = 0) { syncManager.sync(any(), any()) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when ciphersService importCiphers is Success and syncManager sync fails, should return SyncFailed`() =
        runTest {
            coEvery {
                vaultSdkSource.importCxf(
                    userId = testUserId,
                    payload = testPayload,
                )
            } returns mockSdkCipherList.asSuccess()

            coEvery {
                ciphersService.importCiphers(any())
            } returns ImportCiphersResponseJson.Success.asSuccess()

            val syncException = RuntimeException("Sync failed")
            coEvery {
                syncManager.sync(userId = testUserId, forced = true)
            } returns SyncVaultDataResult.Error(syncException)

            val result = importManager.importCxfPayload(testUserId, testPayload)

            assertTrue(result is ImportCxfPayloadResult.SyncFailed)
            assertEquals(syncException, (result as ImportCxfPayloadResult.SyncFailed).error)
            coVerify(exactly = 1) { vaultSdkSource.importCxf(testUserId, testPayload) }
            coVerify(exactly = 1) { ciphersService.importCiphers(any()) }
            coVerify(exactly = 1) { syncManager.sync(testUserId, true) }
        }

    @Test
    fun `when all steps succeed, should return Success`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = testUserId,
                payload = testPayload,
            )
        } returns mockSdkCipherList.asSuccess()

        coEvery {
            ciphersService.importCiphers(any())
        } returns ImportCiphersResponseJson.Success.asSuccess()

        coEvery {
            syncManager.sync(userId = testUserId, forced = true)
        } returns SyncVaultDataResult.Success(itemsAvailable = true)

        val result = importManager.importCxfPayload(testUserId, testPayload)

        assertEquals(ImportCxfPayloadResult.Success, result)
        coVerify(exactly = 1) { vaultSdkSource.importCxf(testUserId, testPayload) }
        coVerify(exactly = 1) { ciphersService.importCiphers(any()) }
        coVerify(exactly = 1) { syncManager.sync(testUserId, true) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when importCxf returns empty cipher list, should skip importCiphers and sync and return NoItems`() =
        runTest {
            coEvery {
                vaultSdkSource.importCxf(
                    userId = testUserId,
                    payload = testPayload,
                )
            } returns emptyList<Cipher>().asSuccess()

            val result = importManager.importCxfPayload(testUserId, testPayload)

            assertEquals(ImportCxfPayloadResult.NoItems, result)
            coVerify(exactly = 1) { vaultSdkSource.importCxf(testUserId, testPayload) }
            coVerify(exactly = 0) { ciphersService.importCiphers(any()) }
            coVerify(exactly = 0) { syncManager.sync(testUserId, true) }
        }
}
