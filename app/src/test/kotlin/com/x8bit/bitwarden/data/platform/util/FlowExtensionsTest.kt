package com.x8bit.bitwarden.data.platform.util

import app.cash.turbine.test
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtensionsTest {

    @Test
    fun `firstWithTimeoutOrNull should return null when the defined timeout has be reached`() =
        runTest {
            val timeout = 1_000L
            val mutableSharedFlow = bufferedMutableSharedFlow<Unit>()

            val result = async {
                mutableSharedFlow.firstWithTimeoutOrNull(timeMillis = timeout)
            }

            testScheduler.runCurrent()
            assertFalse(result.isCompleted)
            testScheduler.advanceTimeBy(delayTimeMillis = timeout)
            testScheduler.runCurrent()

            assertTrue(result.isCompleted)
            assertNull(result.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `firstWithTimeoutOrNull should return a value when the value is emitted before the defined timeout has be reached`() =
        runTest {
            val timeout = 1_000L
            val mutableSharedFlow = bufferedMutableSharedFlow<Unit>()

            val result = async {
                mutableSharedFlow.firstWithTimeoutOrNull(timeMillis = timeout)
            }

            testScheduler.runCurrent()
            assertFalse(result.isCompleted)
            testScheduler.advanceTimeBy(delayTimeMillis = timeout / 2)
            testScheduler.runCurrent()
            assertFalse(result.isCompleted)

            mutableSharedFlow.tryEmit(Unit)
            testScheduler.runCurrent()

            assertTrue(result.isCompleted)
            assertNotNull(result.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `firstWithTimeoutOrNull with predicate should return null when the defined timeout has be reached`() =
        runTest {
            val timeout = 1_000L
            val mutableSharedFlow = bufferedMutableSharedFlow<Boolean>()

            val result = async {
                mutableSharedFlow.firstWithTimeoutOrNull(timeMillis = timeout) { it }
            }

            testScheduler.runCurrent()
            assertFalse(result.isCompleted)
            testScheduler.advanceTimeBy(delayTimeMillis = timeout / 2)
            testScheduler.runCurrent()
            assertFalse(result.isCompleted)

            mutableSharedFlow.tryEmit(false)
            testScheduler.advanceTimeBy(delayTimeMillis = timeout)
            testScheduler.runCurrent()

            assertTrue(result.isCompleted)
            assertNull(result.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `firstWithTimeoutOrNull with predicate should return a value when the value is emitted before the defined timeout has be reached`() =
        runTest {
            val timeout = 1_000L
            val mutableSharedFlow = bufferedMutableSharedFlow<Boolean>()

            val result = async {
                mutableSharedFlow.firstWithTimeoutOrNull(timeMillis = timeout) { it }
            }

            testScheduler.runCurrent()
            assertFalse(result.isCompleted)
            testScheduler.advanceTimeBy(delayTimeMillis = timeout / 2)
            testScheduler.runCurrent()
            assertFalse(result.isCompleted)

            mutableSharedFlow.tryEmit(false)
            testScheduler.runCurrent()

            assertFalse(result.isCompleted)

            mutableSharedFlow.tryEmit(true)
            testScheduler.runCurrent()

            assertTrue(result.isCompleted)
            assertNotNull(result.await())
        }

    @Test
    fun `scanPairs should emit nothing when upstream is empty`() = runTest {
        emptyFlow<String>().scanPairs().test {
            awaitComplete()
        }
    }

    @Test
    fun `scanPairs should emit null to first pair when upstream emits one value`() = runTest {
        flowOf("a").scanPairs().test {
            assertEquals(null to "a", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `scanPairs should emit successive previous-current pairs for multiple emissions`() =
        runTest {
            flowOf("a", "b", "c").scanPairs().test {
                assertEquals(null to "a", awaitItem())
                assertEquals("a" to "b", awaitItem())
                assertEquals("b" to "c", awaitItem())
                awaitComplete()
            }
        }

    @Test
    fun `scanPairs should propagate null emissions within the upstream`() = runTest {
        flowOf("a", null, "b").scanPairs().test {
            assertEquals(null to "a", awaitItem())
            assertEquals("a" to null, awaitItem())
            assertEquals(null to "b", awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun `scanPairs should seed the first pair's previous with the provided initial value`() =
        runTest {
            flowOf("b", "c").scanPairs(initial = "a").test {
                assertEquals("a" to "b", awaitItem())
                assertEquals("b" to "c", awaitItem())
                awaitComplete()
            }
        }
}
