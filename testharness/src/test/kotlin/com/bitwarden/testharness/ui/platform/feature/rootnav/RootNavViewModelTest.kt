package com.bitwarden.testharness.ui.platform.feature.rootnav

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class RootNavViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state should transition from Splash to Landing on init`() = runTest {
        val viewModel = RootNavViewModel()

        viewModel.stateFlow.test {
            // First emission is Landing (after init block updates from Splash)
            assertEquals(RootNavState.Landing, awaitItem())
        }
    }

    @Test
    fun `state flow emits correct Landing state`() = runTest {
        val viewModel = RootNavViewModel()

        viewModel.stateFlow.test {
            val state = awaitItem()
            assertEquals(RootNavState.Landing, state)
        }
    }

    @Test
    fun `event flow remains empty when no events are emitted`() = runTest {
        val viewModel = RootNavViewModel()

        viewModel.eventFlow.test {
            // Event flow should not emit anything since no actions produce events
            expectNoEvents()
        }
    }

    @Test
    fun `state is Landing after ViewModel construction`() = runTest {
        val viewModel = RootNavViewModel()

        // Verify the current state value directly
        assertEquals(RootNavState.Landing, viewModel.stateFlow.value)
    }
}
