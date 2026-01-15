/**
 * Complete Repository Test Example
 *
 * Key patterns demonstrated:
 * - Fake vs Mock strategy: Fakes for happy paths, Mocks for error paths
 * - Using FakeDispatcherManager for deterministic coroutines
 * - Using fixed Clock for deterministic time
 * - Testing Result types with .asSuccess() / .asFailure()
 * - Testing Flow emissions with Turbine
 * - Isolated mock instances for error path testing
 */
package com.bitwarden.example.data.repository

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onSubscription
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

    // Mock service (network layer is always mocked)
    private val mockService: ExampleService = mockk()

    /**
     * PATTERN: Use Fake for disk source in happy path tests.
     * This is the Bitwarden convention for repository testing.
     */
    private val fakeDiskSource = FakeExampleDiskSource()

    private lateinit var repository: ExampleRepositoryImpl

    @BeforeEach
    fun setup() {
        repository = ExampleRepositoryImpl(
            clock = fixedClock,
            service = mockService,
            diskSource = fakeDiskSource,
            dispatcherManager = dispatcherManager,
        )
    }

    // ==================== HAPPY PATH TESTS (use Fake) ====================

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
        // Fake automatically stores the data - verify it's there
        assertEquals(expectedData, fakeDiskSource.storedData)
    }

    /**
     * Test: Service failure returns failure without saving
     */
    @Test
    fun `fetchData should return failure when service fails`() = runTest {
        val exception = Exception("Network error")
        coEvery { mockService.getData() } returns exception.asFailure()

        val result = repository.fetchData()

        assertTrue(result.isFailure)
        assertEquals(exception, result.exceptionOrNull())
        // Fake was not updated
        assertEquals(null, fakeDiskSource.storedData)
    }

    /**
     * Test: Repository flow emits when disk source updates
     */
    @Test
    fun `dataFlow should emit when disk source updates`() = runTest {
        val data1 = ExampleData(id = "1", name = "First", updatedAt = fixedClock.instant())
        val data2 = ExampleData(id = "2", name = "Second", updatedAt = fixedClock.instant())

        repository.dataFlow.test {
            // Initial null value from Fake
            assertEquals(null, awaitItem())

            // Update via Fake
            fakeDiskSource.emitData(data1)
            assertEquals(data1, awaitItem())

            // Another update
            fakeDiskSource.emitData(data2)
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
        assertEquals(newData, fakeDiskSource.storedData)
    }

    /**
     * Test: Delete clears data from disk
     */
    @Test
    fun `deleteData should clear disk source`() = runTest {
        // Pre-populate the fake
        fakeDiskSource.storedData = ExampleData(id = "1", name = "Test", updatedAt = fixedClock.instant())

        repository.deleteData()

        assertEquals(null, fakeDiskSource.storedData)
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
        fakeDiskSource.storedData = cachedData

        val result = repository.getCachedData()

        assertEquals(cachedData, result)
        coVerify(exactly = 0) { mockService.getData() }
    }

    // ==================== ERROR PATH TESTS (use isolated Mock) ====================

    /**
     * PATTERN: For error paths that require exceptions, create isolated
     * repository instances with mocked dependencies.
     */
    @Test
    fun `saveData should return Error when disk source throws exception`() = runTest {
        // Create isolated mock that throws
        val mockDiskSource = mockk<ExampleDiskSource> {
            every { dataFlow } returns MutableStateFlow(null)
            every { saveData(any()) } throws RuntimeException("Disk full")
        }

        // Create isolated repository with the throwing mock
        val repository = ExampleRepositoryImpl(
            clock = fixedClock,
            service = mockService,
            diskSource = mockDiskSource,
            dispatcherManager = dispatcherManager,
        )

        val testData = ExampleData(id = "1", name = "Test", updatedAt = fixedClock.instant())
        val result = repository.saveData(testData)

        assertTrue(result.isFailure)
    }

    @Test
    fun `getCachedData should return null when disk source throws exception`() = runTest {
        val mockDiskSource = mockk<ExampleDiskSource> {
            every { dataFlow } returns MutableStateFlow(null)
            every { getData() } throws RuntimeException("Database corrupted")
        }

        val repository = ExampleRepositoryImpl(
            clock = fixedClock,
            service = mockService,
            diskSource = mockDiskSource,
            dispatcherManager = dispatcherManager,
        )

        val result = repository.getCachedData()

        assertEquals(null, result)
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

/**
 * PATTERN: Fake implementation for happy path testing.
 *
 * Key characteristics:
 * - Uses bufferedMutableSharedFlow() for proper replay behavior
 * - Uses .onSubscription { emit(state) } for immediate state emission
 * - Exposes internal state for test assertions
 */
class FakeExampleDiskSource : ExampleDiskSource {
    private val mutableDataFlow = bufferedMutableSharedFlow<ExampleData?>()

    // Expose for test assertions
    var storedData: ExampleData? = null

    override val dataFlow: Flow<ExampleData?>
        get() = mutableDataFlow.onSubscription { emit(storedData) }

    override fun getData(): ExampleData? = storedData

    override fun saveData(data: ExampleData) {
        storedData = data
        mutableDataFlow.tryEmit(data)
    }

    override fun clearData() {
        storedData = null
        mutableDataFlow.tryEmit(null)
    }

    // Test helper to emit data without saving
    fun emitData(data: ExampleData?) {
        storedData = data
        mutableDataFlow.tryEmit(data)
    }
}
