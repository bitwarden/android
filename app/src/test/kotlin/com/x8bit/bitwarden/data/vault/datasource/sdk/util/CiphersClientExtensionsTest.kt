package com.x8bit.bitwarden.data.vault.datasource.sdk.util

import app.cash.turbine.test
import com.bitwarden.sdk.BitwardenException
import com.bitwarden.sdk.CipherListItem
import com.bitwarden.sdk.CipherListStream
import com.bitwarden.sdk.CiphersClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CiphersClientExtensionsTest {

    private val stream = mockk<CipherListStream> {
        every { close() } just runs
    }
    private val ciphersClient = mockk<CiphersClient> {
        every { listStream(chunkSize = CHUNK_SIZE) } returns stream
    }

    @Test
    fun `listStreamFlow should emit the items of every chunk in order and close the stream`() =
        runTest {
            val items = List(5) { mockk<CipherListItem>() }
            coEvery {
                stream.nextChunk()
            } returnsMany listOf(items.subList(0, 2), emptyList(), items.subList(2, 5), null)

            val result = ciphersClient.listStreamFlow(chunkSize = CHUNK_SIZE).toList()

            assertEquals(items, result)
            coVerify(exactly = 4) { stream.nextChunk() }
            verify(exactly = 1) { stream.close() }
        }

    @Test
    fun `listStreamFlow should complete without items when the first chunk is null`() = runTest {
        coEvery { stream.nextChunk() } returns null

        val result = ciphersClient.listStreamFlow(chunkSize = CHUNK_SIZE).toList()

        assertEquals(emptyList<CipherListItem>(), result)
        verify(exactly = 1) { stream.close() }
    }

    @Test
    fun `listStreamFlow should fail with the error of the stream and close the stream`() =
        runTest {
            val item = mockk<CipherListItem>()
            val error = mockk<BitwardenException.DecryptStream>()
            coEvery { stream.nextChunk() } returns listOf(item) andThenThrows error

            ciphersClient.listStreamFlow(chunkSize = CHUNK_SIZE).test {
                assertEquals(item, awaitItem())
                assertEquals(error, awaitError())
            }

            coVerify(exactly = 2) { stream.nextChunk() }
            verify(exactly = 1) { stream.close() }
        }

    @Test
    fun `listStreamFlow should stop reading chunks when the collector stops`() = runTest {
        val items = List(2) { mockk<CipherListItem>() }
        coEvery { stream.nextChunk() } returns items

        val result = ciphersClient.listStreamFlow(chunkSize = CHUNK_SIZE).take(1).toList()

        assertEquals(items.take(1), result)
        coVerify(exactly = 1) { stream.nextChunk() }
        verify(exactly = 1) { stream.close() }
    }

    @Test
    fun `listStreamFlow should open a new stream for each collection`() = runTest {
        coEvery { stream.nextChunk() } returns null
        val flow = ciphersClient.listStreamFlow(chunkSize = CHUNK_SIZE)

        flow.toList()
        flow.toList()

        verify(exactly = 2) { ciphersClient.listStream(chunkSize = CHUNK_SIZE) }
    }
}

private const val CHUNK_SIZE: UInt = 2u
