package com.bitwarden.authenticator.data.authenticator.manager

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.io.OutputStream

/**
 * Test class for [FileManagerImpl].
 */
class FileManagerTest {

    private val fakeDispatcherManager = FakeDispatcherManager()
    private val mockContentResolver = mockk<ContentResolver>()
    private val mockContext = mockk<Context> {
        every { contentResolver } returns mockContentResolver
    }
    private val mockUri = mockk<Uri>()

    private val fileManager = FileManagerImpl(
        context = mockContext,
        dispatcherManager = fakeDispatcherManager,
    )

    //region stringToUri Tests

    @Test
    fun `stringToUri with valid data should return true`() = runTest {
        val testString = "Test data"
        val mockOutputStream = createMockOutputStream()

        every { mockContentResolver.openOutputStream(mockUri) } returns mockOutputStream

        val result = fileManager.stringToUri(mockUri, testString)

        assertTrue(result)
    }

    @Test
    fun `stringToUri with write failure should return false`() = runTest {
        val testString = "Test data"

        every {
            mockContentResolver.openOutputStream(mockUri)
        } throws RuntimeException("Write failed")

        val result = fileManager.stringToUri(mockUri, testString)

        assertFalse(result)
    }

    @Test
    fun `stringToUri should convert string to bytes correctly`() = runTest {
        val testString = "Hello, World!"
        val capturedBytes = mutableListOf<Byte>()
        val mockOutputStream = createMockOutputStream(capturedBytes)

        every { mockContentResolver.openOutputStream(mockUri) } returns mockOutputStream

        fileManager.stringToUri(mockUri, testString)

        assertEquals(testString, String(capturedBytes.toByteArray()))
    }

    @Test
    fun `stringToUri with large string should write completely`() = runTest {
        val testString = "A".repeat(10000)
        val capturedBytes = mutableListOf<Byte>()
        val mockOutputStream = createMockOutputStream(capturedBytes)

        every { mockContentResolver.openOutputStream(mockUri) } returns mockOutputStream

        val result = fileManager.stringToUri(mockUri, testString)

        assertTrue(result)
        assertEquals(testString.length, capturedBytes.size)
        assertEquals(testString, String(capturedBytes.toByteArray()))
    }

    //endregion

    //region uriToByteArray Tests

    @Test
    fun `uriToByteArray with valid file should return Success`() = runTest {
        val testData = "Test content".toByteArray()
        val mockInputStream = createMockInputStream(testData)

        every { mockContentResolver.openInputStream(mockUri) } returns mockInputStream

        val result = fileManager.uriToByteArray(mockUri)

        assertTrue(result.isSuccess)
        assertEquals(String(testData), String(result.getOrNull()!!))
    }

    @Test
    fun `uriToByteArray with null InputStream should return Failure with Stream has crashed`() =
        runTest {
            every { mockContentResolver.openInputStream(mockUri) } returns null

            val result = fileManager.uriToByteArray(mockUri)

            assertTrue(result.isFailure)
            assertEquals("Stream has crashed", result.exceptionOrNull()?.message)
        }

    @Test
    fun `uriToByteArray with read exception should return Failure`() = runTest {
        every { mockContentResolver.openInputStream(mockUri) } throws
            RuntimeException("Read failed")

        val result = fileManager.uriToByteArray(mockUri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is RuntimeException)
    }

    @Test
    fun `uriToByteArray should read in 1024-byte buffers`() = runTest {
        val testData = "X".repeat(2048).toByteArray()
        var readCallCount = 0
        var maxBufferSize = 0

        val mockInputStream = mockk<InputStream> {
            var position = 0
            every { read(any<ByteArray>()) } answers {
                readCallCount++
                val buffer = firstArg<ByteArray>()
                maxBufferSize = maxOf(maxBufferSize, buffer.size)

                val remaining = testData.size - position
                if (remaining <= 0) return@answers -1

                val toRead = minOf(remaining, buffer.size)
                testData.copyInto(
                    destination = buffer,
                    destinationOffset = 0,
                    startIndex = position,
                    endIndex = position + toRead,
                )
                position += toRead
                toRead
            }
            every { close() } just runs
        }

        every { mockContentResolver.openInputStream(mockUri) } returns mockInputStream

        val result = fileManager.uriToByteArray(mockUri)

        assertTrue(result.isSuccess)
        assertEquals(1024, maxBufferSize)
    }

    @Test
    fun `uriToByteArray with empty file should return empty ByteArray`() = runTest {
        val testData = ByteArray(0)
        val mockInputStream = createMockInputStream(testData)

        every { mockContentResolver.openInputStream(mockUri) } returns mockInputStream

        val result = fileManager.uriToByteArray(mockUri)

        assertTrue(result.isSuccess)
        assertEquals(0, result.getOrNull()?.size)
    }

    @Test
    fun `uriToByteArray with small file should read completely`() = runTest {
        val testData = "Small file".toByteArray()
        val mockInputStream = createMockInputStream(testData)

        every { mockContentResolver.openInputStream(mockUri) } returns mockInputStream

        val result = fileManager.uriToByteArray(mockUri)

        assertTrue(result.isSuccess)
        assertEquals(String(testData), String(result.getOrNull()!!))
    }

    @Test
    fun `uriToByteArray with large file should read completely`() = runTest {
        val testData = "L".repeat(5000).toByteArray()
        val mockInputStream = createMockInputStream(testData)

        every { mockContentResolver.openInputStream(mockUri) } returns mockInputStream

        val result = fileManager.uriToByteArray(mockUri)

        assertTrue(result.isSuccess)
        assertEquals(testData.size, result.getOrNull()?.size)
        assertEquals(String(testData), String(result.getOrNull()!!))
    }

    @Test
    fun `uriToByteArray should handle partial reads`() = runTest {
        val testData = "Partial read test".toByteArray()
        var readCallCount = 0

        val mockInputStream = mockk<InputStream> {
            var position = 0
            every { read(any<ByteArray>()) } answers {
                readCallCount++
                val buffer = firstArg<ByteArray>()

                val remaining = testData.size - position
                if (remaining <= 0) return@answers -1

                // Simulate partial reads by only reading half the buffer size
                val toRead = minOf(remaining, buffer.size / 2, 5)
                testData.copyInto(
                    destination = buffer,
                    destinationOffset = 0,
                    startIndex = position,
                    endIndex = position + toRead,
                )
                position += toRead
                toRead
            }
            every { close() } just runs
        }

        every { mockContentResolver.openInputStream(mockUri) } returns mockInputStream

        val result = fileManager.uriToByteArray(mockUri)

        assertTrue(result.isSuccess)
        assertEquals(String(testData), String(result.getOrNull()!!))
        assertTrue(readCallCount > testData.size / 5) // Multiple small reads
    }

    //endregion

    //region Helper Methods

    /**
     * Creates a mock OutputStream that captures written bytes.
     */
    private fun createMockOutputStream(
        capturedBytes: MutableList<Byte> = mutableListOf(),
    ): OutputStream = mockk {
        every { write(any<ByteArray>()) } answers {
            capturedBytes.addAll(firstArg<ByteArray>().toList())
        }
        every { close() } just runs
    }

    /**
     * Creates a mock InputStream that reads from testData.
     */
    private fun createMockInputStream(testData: ByteArray): InputStream = mockk {
        var position = 0
        every { read(any<ByteArray>()) } answers {
            val buffer = firstArg<ByteArray>()
            val remaining = testData.size - position
            if (remaining <= 0) return@answers -1

            val toRead = minOf(remaining, buffer.size)
            testData.copyInto(
                destination = buffer,
                destinationOffset = 0,
                startIndex = position,
                endIndex = position + toRead,
            )
            position += toRead
            toRead
        }
        every { close() } just runs
    }

    //endregion
}
