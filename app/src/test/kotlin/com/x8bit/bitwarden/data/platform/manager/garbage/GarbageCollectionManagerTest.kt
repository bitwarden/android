package com.x8bit.bitwarden.data.platform.manager.garbage

import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.advanceTimeByAndRunCurrent
import kotlinx.coroutines.test.StandardTestDispatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class GarbageCollectionManagerTest {

    private var garbageCollectionCount = 0
    private val dispatcher = StandardTestDispatcher()

    private val garbageCollectionManager: GarbageCollectionManager = GarbageCollectionManagerImpl(
        garbageCollector = { garbageCollectionCount++ },
        dispatcherManager = FakeDispatcherManager(unconfined = dispatcher),
    )

    @Test
    fun `tryCollect should attempt to garbage collect 10 times in increasing intervals`() {
        garbageCollectionManager.tryCollect()

        // We do nothing right away
        dispatcher.scheduler.runCurrent()
        assertEquals(0, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 100L)
        assertEquals(1, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 200L)
        assertEquals(2, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 300L)
        assertEquals(3, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 400L)
        assertEquals(4, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 500L)
        assertEquals(5, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 600L)
        assertEquals(6, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 700L)
        assertEquals(7, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 800L)
        assertEquals(8, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 900L)
        assertEquals(9, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 1_000L)
        assertEquals(10, garbageCollectionCount)

        // We should stop at this point, even 10 seconds later we should not have run again
        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 10_000L)
        assertEquals(10, garbageCollectionCount)
    }

    @Test
    fun `tryCollect should restart the intervals when called multiple times`() {
        garbageCollectionManager.tryCollect()

        // We do nothing right away
        dispatcher.scheduler.runCurrent()
        assertEquals(0, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 100L)
        assertEquals(1, garbageCollectionCount)

        garbageCollectionManager.tryCollect()

        // We do nothing right away
        dispatcher.scheduler.runCurrent()
        assertEquals(1, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 100L)
        assertEquals(2, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 200L)
        assertEquals(3, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 300L)
        assertEquals(4, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 400L)
        assertEquals(5, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 500L)
        assertEquals(6, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 600L)
        assertEquals(7, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 700L)
        assertEquals(8, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 800L)
        assertEquals(9, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 900L)
        assertEquals(10, garbageCollectionCount)

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 1_000L)
        assertEquals(11, garbageCollectionCount)

        // We should stop at this point, even 10 seconds later we should not have run again
        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 10_000L)
        assertEquals(11, garbageCollectionCount)
    }
}
