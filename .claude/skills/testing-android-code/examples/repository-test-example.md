/**
 * Complete Repository Test Example
 *
 * Key patterns demonstrated:
 * - Using FakeDispatcherManager
 * - Using fixed Clock for deterministic time
 * - Mocking services and data sources
 * - Testing Result types with .asSuccess() / .asFailure()
 * - Testing Flow emissions from repositories
 */
package com.bitwarden.example.data.repository

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class ExampleRepositoryTest {

    // Fixed clock for deterministic time-based tests
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    // Use FakeDispatcherManager for deterministic coroutine execution
    private val dispatcherManager = FakeDispatcherManager()

    // Mock dependencies
    private val mockService: ExampleService = mockk()
    private val mockDiskSource: ExampleDiskSource = mockk()

    // Mutable flow for testing reactive updates
    private val mutableDataFlow = MutableStateFlow<ExampleData?>(null)

    private lateinit var repository: ExampleRepositoryImpl

    @BeforeEach
    fun setup() {
        // Setup disk source with flow
        every { mockDiskSource.dataFlow } returns mutableDataFlow
        every { mockDiskSource.saveData(any()) } returns Unit

        repository = ExampleRepositoryImpl(
            clock = fixedClock,
            service = mockService,
            diskSource = mockDiskSource,
            dispatcherManager = dispatcherManager,
        )
    }

    /**
     * Test: Successful fetch returns data and saves to disk
     */
    @Test
    fun `fetchData should return success and save to disk when service succeeds`() = runTest {
        val expectedData = ExampleData(id = "1", name = "Test", updatedAt = fixedClock.instant())
        coEvery { mockService.getData() } returns expectedData.asSuccess()

        val result = repository.fetchData()

        assertTrue(result.isSuccess)
        assertEquals(expectedData, result.getOrNull())
        verify { mockDiskSource.saveData(expectedData) }
    }

    /**
     * Test: Failed fetch returns failure without saving
     */
    @Test
    fun `fetchData should return failure when service fails`() = runTest {
        val exception = Exception("Network error")
        coEvery { mockService.getData() } returns exception.asFailure()

        val result = repository.fetchData()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        verify(exactly = 0) { mockDiskSource.saveData(any()) }
    }

    /**
     * Test: Repository flow emits when disk source updates
     */
    @Test
    fun `dataFlow should emit when disk source updates`() = runTest {
        val data1 = ExampleData(id = "1", name = "First", updatedAt = fixedClock.instant())
        val data2 = ExampleData(id = "2", name = "Second", updatedAt = fixedClock.instant())

        repository.dataFlow.test {
            // Initial null value
            assertEquals(null, awaitItem())

            // Update disk source
            mutableDataFlow.value = data1
            assertEquals(data1, awaitItem())

            // Another update
            mutableDataFlow.value = data2
            assertEquals(data2, awaitItem())
        }
    }

    /**
     * Test: Refresh fetches and saves new data
     */
    @Test
    fun `refresh should fetch new data and update disk source`() = runTest {
        val newData = ExampleData(id = "new", name = "Fresh", updatedAt = fixedClock.instant())
        coEvery { mockService.getData() } returns newData.asSuccess()

        val result = repository.refresh()

        assertTrue(result.isSuccess)
        coVerify { mockService.getData() }
        verify { mockDiskSource.saveData(newData) }
    }

    /**
     * Test: Delete clears data from disk
     */
    @Test
    fun `deleteData should clear disk source`() = runTest {
        every { mockDiskSource.clearData() } returns Unit

        repository.deleteData()

        verify { mockDiskSource.clearData() }
    }

    /**
     * Test: Cached data returns from disk when available
     */
    @Test
    fun `getCachedData should return disk data without network call`() = runTest {
        val cachedData = ExampleData(
            id = "cached",
            name = "Cached",
            updatedAt = fixedClock.instant(),
        )
        every { mockDiskSource.getData() } returns cachedData

        val result = repository.getCachedData()

        assertEquals(cachedData, result)
        coVerify(exactly = 0) { mockService.getData() }
    }

    /**
     * Test: Update with transformation
     */
    @Test
    fun `updateData should transform and save data`() = runTest {
        val existingData = ExampleData(id = "1", name = "Old", updatedAt = fixedClock.instant())
        val expectedData = existingData.copy(name = "Updated")

        every { mockDiskSource.getData() } returns existingData

        repository.updateData { it.copy(name = "Updated") }

        verify { mockDiskSource.saveData(expectedData) }
    }
}

// Example types (normally in separate files)
data class ExampleData(
    val id: String,
    val name: String,
    val updatedAt: Instant,
)

interface ExampleService {
    suspend fun getData(): Result<ExampleData>
}

interface ExampleDiskSource {
    val dataFlow: kotlinx.coroutines.flow.Flow<ExampleData?>
    fun getData(): ExampleData?
    fun saveData(data: ExampleData)
    fun clearData()
}
