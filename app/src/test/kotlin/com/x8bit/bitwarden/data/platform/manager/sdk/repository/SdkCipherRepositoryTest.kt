package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.sdk.CipherRepository
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipherResponse
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SdkCipherRepositoryTest {

    private val vaultDiskSource: VaultDiskSource = mockk()

    private val sdkCipherRepository: CipherRepository = SdkCipherRepository(
        userId = USER_ID,
        vaultDiskSource = vaultDiskSource,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(
            SyncResponseJson.Cipher::toEncryptedSdkCipher,
            Cipher::toEncryptedNetworkCipherResponse,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            SyncResponseJson.Cipher::toEncryptedSdkCipher,
            Cipher::toEncryptedNetworkCipherResponse,
        )
    }

    @Test
    fun `get should return null when not present`() = runTest {
        val cipherId = "cipherId"
        coEvery { vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId) } returns null

        val result = sdkCipherRepository.get(id = cipherId)

        assertNull(result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId)
        }
    }

    @Test
    fun `get should return an encrypted sdk cipher when present`() = runTest {
        val cipherId = "cipherId"
        val expected = mockk<Cipher>()
        val responseCipher = mockk<SyncResponseJson.Cipher> {
            every { toEncryptedSdkCipher() } returns expected
        }
        coEvery {
            vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId)
        } returns responseCipher

        val result = sdkCipherRepository.get(id = cipherId)

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId)
        }
    }

    @Test
    fun `has should return false when not present`() = runTest {
        val cipherId = "cipherId"
        coEvery { vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId) } returns null

        val result = sdkCipherRepository.has(id = cipherId)

        assertFalse(result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId)
        }
    }

    @Test
    fun `get should return true when present`() = runTest {
        val cipherId = "cipherId"
        val responseCipher = mockk<SyncResponseJson.Cipher> {
            every { toEncryptedSdkCipher() } returns mockk<Cipher>()
        }
        coEvery {
            vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId)
        } returns responseCipher

        val result = sdkCipherRepository.has(id = cipherId)

        assertTrue(result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCipher(userId = USER_ID, cipherId = cipherId)
        }
    }

    @Test
    fun `list should return empty list when nothing present`() = runTest {
        coEvery { vaultDiskSource.getCiphers(userId = USER_ID) } returns emptyList()

        val result = sdkCipherRepository.list()

        assertEquals(emptyList<Cipher>(), result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCiphers(userId = USER_ID)
        }
    }

    @Test
    fun `list should return encrypted sdk cipher list when present`() = runTest {
        val expectedCipher = mockk<Cipher>()
        val expected = listOf(expectedCipher)
        val responseCipher = mockk<SyncResponseJson.Cipher> {
            every { toEncryptedSdkCipher() } returns expectedCipher
        }
        coEvery { vaultDiskSource.getCiphers(userId = USER_ID) } returns listOf(responseCipher)

        val result = sdkCipherRepository.list()

        assertEquals(expected, result)
        coVerify(exactly = 1) {
            vaultDiskSource.getCiphers(userId = USER_ID)
        }
    }

    @Test
    fun `remove should call deleteCipher on the vaultDiskSource`() = runTest {
        val cipherId = "cipherId"
        coEvery { vaultDiskSource.deleteCipher(userId = USER_ID, cipherId = cipherId) } just runs

        sdkCipherRepository.remove(id = cipherId)

        coVerify(exactly = 1) {
            vaultDiskSource.deleteCipher(userId = USER_ID, cipherId = cipherId)
        }
    }

    @Test
    fun `set should do nothing if the ids don't match`() = runTest {
        val cipherId = "cipherId"
        val cipher = mockk<Cipher> {
            every { id } returns "differentCipherId"
        }

        sdkCipherRepository.set(id = cipherId, value = cipher)

        coVerify(exactly = 0) {
            vaultDiskSource.saveCipher(userId = any(), cipher = any())
        }
    }

    @Test
    fun `set should call saveCipher on vaultDiskSource if ids match`() = runTest {
        val cipherId = "cipherId"
        val responseCipher = mockk<SyncResponseJson.Cipher>()
        val cipher = mockk<Cipher> {
            every { id } returns cipherId
            every {
                toEncryptedNetworkCipherResponse(encryptedFor = USER_ID)
            } returns responseCipher
        }
        coEvery {
            vaultDiskSource.saveCipher(userId = USER_ID, cipher = responseCipher)
        } just runs

        sdkCipherRepository.set(id = cipherId, value = cipher)

        coVerify(exactly = 1) {
            vaultDiskSource.saveCipher(userId = USER_ID, cipher = responseCipher)
        }
    }
}

private const val USER_ID: String = "userId"
