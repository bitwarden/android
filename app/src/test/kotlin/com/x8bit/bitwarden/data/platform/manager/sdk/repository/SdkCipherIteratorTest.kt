package com.x8bit.bitwarden.data.platform.manager.sdk.repository

import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.vault.Cipher
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class SdkCipherIteratorTest {

    private val vaultDiskSource: VaultDiskSource = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(SyncResponseJson.Cipher::toEncryptedSdkCipher)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(SyncResponseJson.Cipher::toEncryptedSdkCipher)
    }

    @Test
    fun `next should read each page after the id of the last cipher read`() = runTest {
        val (responses1, ciphers1) = mockCiphers("a", "b")
        val (responses2, ciphers2) = mockCiphers("c", "d")
        val (responses3, ciphers3) = mockCiphers("e")
        mockPage(afterCipherId = null, limit = 2, page = responses1)
        mockPage(afterCipherId = "b", limit = 2, page = responses2)
        mockPage(afterCipherId = "d", limit = 2, page = responses3)
        val iterator = createIterator(batchSize = 2)

        assertEquals(ciphers1, iterator.next())
        assertEquals(ciphers2, iterator.next())
        assertEquals(ciphers3, iterator.next())
        assertNull(iterator.next())

        // The short last page ends the iteration without another query.
        coVerify(exactly = 3) {
            vaultDiskSource.getCiphersPage(userId = USER_ID, afterCipherId = any(), limit = 2)
        }
    }

    @Test
    fun `next should end with an empty page when the last page is full`() = runTest {
        val (responses, ciphers) = mockCiphers("a", "b")
        mockPage(afterCipherId = null, limit = 2, page = responses)
        mockPage(afterCipherId = "b", limit = 2, page = emptyList())
        val iterator = createIterator(batchSize = 2)

        assertEquals(ciphers, iterator.next())
        assertNull(iterator.next())
        assertNull(iterator.next())

        coVerify(exactly = 1) {
            vaultDiskSource.getCiphersPage(userId = USER_ID, afterCipherId = null, limit = 2)
        }
        coVerify(exactly = 1) {
            vaultDiskSource.getCiphersPage(userId = USER_ID, afterCipherId = "b", limit = 2)
        }
    }

    @Test
    fun `next should return null when there are no ciphers`() = runTest {
        mockPage(afterCipherId = null, limit = 3, page = emptyList())
        val iterator = createIterator(batchSize = 3)

        assertNull(iterator.next())
        assertNull(iterator.next())

        coVerify(exactly = 1) {
            vaultDiskSource.getCiphersPage(userId = USER_ID, afterCipherId = null, limit = 3)
        }
    }

    private fun createIterator(batchSize: Int): SdkCipherIterator =
        SdkCipherIterator(
            userId = USER_ID,
            vaultDiskSource = vaultDiskSource,
            batchSize = batchSize,
        )

    private fun mockPage(
        afterCipherId: String?,
        limit: Int,
        page: List<SyncResponseJson.Cipher>,
    ) {
        coEvery {
            vaultDiskSource.getCiphersPage(
                userId = USER_ID,
                afterCipherId = afterCipherId,
                limit = limit,
            )
        } returns page
    }

    /**
     * Creates a stored cipher with each of the given [ids], and the SDK cipher each converts to.
     */
    private fun mockCiphers(
        vararg ids: String,
    ): Pair<List<SyncResponseJson.Cipher>, List<Cipher>> {
        val ciphers = ids.map { mockk<Cipher>() }
        val responses = ids.zip(ciphers).map { (cipherId, cipher) ->
            mockk<SyncResponseJson.Cipher> {
                every { id } returns cipherId
                every { toEncryptedSdkCipher() } returns cipher
            }
        }
        return responses to ciphers
    }
}

private const val USER_ID: String = "userId"
