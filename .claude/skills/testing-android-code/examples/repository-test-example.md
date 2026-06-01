/**
 * Complete Repository Test Example
 *
 * Key patterns demonstrated:
 * - Fake for disk sources, Mock for network services
 * - Using FakeDispatcherManager for deterministic coroutines
 * - Using fixed Clock for deterministic time
 * - Testing Result types with .asSuccess() / .asFailure()
 * - Asserting actual objects (not isSuccess/isFailure) for better diagnostics
 * - Testing Flow emissions with Turbine
 */
package com.bitwarden.example.data.repository

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

        assertEquals(expectedData, result.getOrThrow())
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

        assertEquals(exception, result.exceptionOrNull())
        // Fake was not updated
        assertNull(fakeDiskSource.storedData)
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
            assertNull(awaitItem())

            // Update via Fake property setter (triggers emission)
            fakeDiskSource.storedData = data1
            assertEquals(data1, awaitItem())

            // Another update
            fakeDiskSource.storedData = data2
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

        assertEquals(Unit, result.getOrThrow())
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

        assertNull(fakeDiskSource.storedData)
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

    // ==================== ERROR PATH TESTS ====================

    /**
     * PATTERN: For error paths, reconfigure the class-level mock per-test.
     * Use coEvery to change mock behavior for each specific test case.
     */
    @Test
    fun `fetchData should return failure when service returns error`() = runTest {
        val exception = Exception("Server unavailable")
        coEvery { mockService.getData() } returns exception.asFailure()

        val result = repository.fetchData()

        assertEquals(exception, result.exceptionOrNull())
        // Fake state unchanged on failure
        assertNull(fakeDiskSource.storedData)
    }

    @Test
    fun `refresh should return failure and preserve cached data when service fails`() = runTest {
        // Pre-populate cache via Fake
        val cachedData = ExampleData(id = "cached", name = "Old", updatedAt = fixedClock.instant())
        fakeDiskSource.storedData = cachedData

        // Reconfigure mock to return failure
        coEvery { mockService.getData() } returns Exception("Network error").asFailure()

        val result = repository.refresh()

        assertTrue(result.isFailure)
        // Cached data preserved on failure
        assertEquals(cachedData, fakeDiskSource.storedData)
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
 * - Uses bufferedMutableSharedFlow(replay = 1) for proper replay behavior
 * - Uses .onSubscription { emit(state) } for immediate state emission
 * - Private storage with override property setter that emits to flow
 * - Test assertions done via the override property getter
 */
class FakeExampleDiskSource : ExampleDiskSource {
    private var storedDataValue: ExampleData? = null
    private val mutableDataFlow = bufferedMutableSharedFlow<ExampleData?>(replay = 1)

    /**
     * Override property with getter/setter. Setter emits to flow automatically.
     * Tests can read this property for assertions and write to trigger emissions.
     */
    var storedData: ExampleData?
        get() = storedDataValue
        set(value) {
            storedDataValue = value
            mutableDataFlow.tryEmit(value)
        }

    override val dataFlow: Flow<ExampleData?>
        get() = mutableDataFlow.onSubscription { emit(storedData) }

    override fun getData(): ExampleData? = storedData

    override fun saveData(data: ExampleData) {
        storedData = data
    }

    override fun clearData() {
        storedData = null
    }
}
