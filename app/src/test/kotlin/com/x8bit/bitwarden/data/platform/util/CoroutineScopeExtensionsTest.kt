package com.x8bit.bitwarden.data.platform.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CoroutineScopeExtensionsTest {
    @Test
    fun `launchWithTimeout should skip timeout block when main block finishes`() = runTest {
        // Setup
        val timeoutDuration = 1000L
        var timeOutBlockInvoked = false
        var mainBlockInvoked = false

        // Test
        this
            .launchWithTimeout(
                timeoutBlock = { timeOutBlockInvoked = true },
                timeoutDuration = timeoutDuration,
            ) {
                mainBlockInvoked = true
            }
            .invokeOnCompletion {
                // Verify
                assertTrue(mainBlockInvoked)
                assertFalse(timeOutBlockInvoked)
            }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `launchWithTimeout should invoke timeout block when timeout is elapsed`() = runTest {
        // Setup
        val timeoutDuration = 1000L
        var timeOutBlockInvoked = false
        var mainBlockStarted = false
        var mainBlockFinished = false

        // Test
        this
            .launchWithTimeout(
                timeoutBlock = { timeOutBlockInvoked = true },
                timeoutDuration = timeoutDuration,
            ) {
                mainBlockStarted = true
                delay(2000)
                mainBlockFinished = true
            }
            .invokeOnCompletion {
                // Verify
                assertTrue(mainBlockStarted)
                assertFalse(mainBlockFinished)
                assertTrue(timeOutBlockInvoked)
            }

        advanceTimeBy(2000)
    }
}
