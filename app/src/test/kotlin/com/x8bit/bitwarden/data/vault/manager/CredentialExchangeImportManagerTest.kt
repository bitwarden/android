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
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull

class CredentialExchangeImportManagerTest {

    private val vaultSdkSource: VaultSdkSource = mockk()
    private val ciphersService: CiphersService = mockk(relaxed = true)

    private val importManager = CredentialExchangeImportManagerImpl(
        vaultSdkSource = vaultSdkSource,
        ciphersService = ciphersService,
    )

    private val mockSdkCipher: Cipher = createMockSdkCipher(number = 1)
    private val mockSdkCipherList: List<Cipher> = listOf(mockSdkCipher)

    @Test
    fun `when vaultSdkSource importCxf fails, should return Error`() = runTest {
        val exception = RuntimeException("SDK import failed")
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_PAYLOAD,
            )
        } returns exception.asFailure()

        coEvery {
            ciphersService.importCiphers(any())
        } just awaits

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertEquals(ImportCxfPayloadResult.Error(exception), result)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_PAYLOAD)
        }
        coVerify(exactly = 0) {
            ciphersService.importCiphers(any())
        }
    }

    @Test
    fun `when ciphersService importCiphers fails, should return Error`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_PAYLOAD,
            )
        } returns mockSdkCipherList.asSuccess()

        val exception = RuntimeException("Network import failed")
        val capturedRequest = slot<ImportCiphersJsonRequest>()
        coEvery {
            ciphersService.importCiphers(capture(capturedRequest))
        } returns exception.asFailure()

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertEquals(ImportCxfPayloadResult.Error(exception), result)
        assertEquals(1, capturedRequest.captured.ciphers.size)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_PAYLOAD)
            ciphersService.importCiphers(any())
        }
    }

    @Test
    fun `when ciphersService importCiphers returns Invalid, should return Error`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_PAYLOAD,
            )
        } returns mockSdkCipherList.asSuccess()

        coEvery {
            ciphersService.importCiphers(any())
        } returns ImportCiphersResponseJson
            .Invalid(validationErrors = emptyMap())
            .asSuccess()

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        val error = (result as? ImportCxfPayloadResult.Error)?.error
        assertNotNull(error)
        assertTrue(error is ImportCredentialsUnknownErrorException)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_PAYLOAD)
            ciphersService.importCiphers(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when ciphersService importCiphers is Success should return Success`() =
        runTest {
            coEvery {
                vaultSdkSource.importCxf(
                    userId = DEFAULT_USER_ID,
                    payload = DEFAULT_PAYLOAD,
                )
            } returns mockSdkCipherList.asSuccess()

            coEvery {
                ciphersService.importCiphers(any())
            } returns ImportCiphersResponseJson.Success.asSuccess()

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertTrue(result is ImportCxfPayloadResult.Success)
            coVerify(exactly = 1) {
                vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_PAYLOAD)
                ciphersService.importCiphers(any())
            }
        }

    @Test
    fun `when all steps succeed, should return Success`() = runTest {
        coEvery {
            vaultSdkSource.importCxf(
                userId = DEFAULT_USER_ID,
                payload = DEFAULT_PAYLOAD,
            )
        } returns mockSdkCipherList.asSuccess()

        coEvery {
            ciphersService.importCiphers(any())
        } returns ImportCiphersResponseJson.Success.asSuccess()

        val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

        assertEquals(ImportCxfPayloadResult.Success, result)
        coVerify(exactly = 1) {
            vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_PAYLOAD)
            ciphersService.importCiphers(any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `when importCxf returns empty cipher list, should skip importCiphers and sync and return NoItems`() =
        runTest {
            coEvery {
                vaultSdkSource.importCxf(
                    userId = DEFAULT_USER_ID,
                    payload = DEFAULT_PAYLOAD,
                )
            } returns emptyList<Cipher>().asSuccess()
            coEvery {
                ciphersService.importCiphers(any())
            } just awaits

            val result = importManager.importCxfPayload(DEFAULT_USER_ID, DEFAULT_PAYLOAD)

            assertEquals(ImportCxfPayloadResult.NoItems, result)
            coVerify(exactly = 1) {
                vaultSdkSource.importCxf(DEFAULT_USER_ID, DEFAULT_PAYLOAD)
            }
            coVerify(exactly = 0) {
                ciphersService.importCiphers(any())
            }
        }
}

private const val DEFAULT_USER_ID = "mockId-1"
private const val DEFAULT_PAYLOAD = "mockPayload-1"
