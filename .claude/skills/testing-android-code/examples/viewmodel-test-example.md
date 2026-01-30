/**
 * Complete ViewModel Test Example
 *
 * Key patterns demonstrated:
 * - Extending BaseViewModelTest
 * - Testing StateFlow with Turbine
 * - Testing EventFlow with Turbine
 * - Using stateEventFlow() for simultaneous testing
 * - MockK mocking patterns
 * - Test factory method design (accepts domain state, not SavedStateHandle)
 * - Complete state assertions (assert entire state objects)
 */
package com.bitwarden.example.feature

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ExampleViewModelTest : BaseViewModelTest() {

    // Mock dependencies
    private val mockRepository: ExampleRepository = mockk()
    private val mockAuthDiskSource: AuthDiskSource = mockk {
        every { userStateFlow } returns MutableStateFlow(null)
    }

    /**
     * StateFlow has replay=1, so first awaitItem() returns current state
     */
    @Test
    fun `initial state should be default state`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    /**
     * Test state transitions: initial -> loading -> success
     */
    @Test
    fun `LoadData action should update state from idle to loading to success`() = runTest {
        val expectedData = "loaded data"
        coEvery { mockRepository.fetchData(any()) } returns Result.success(expectedData)

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())

            viewModel.trySendAction(ExampleAction.LoadData)

            assertEquals(DEFAULT_STATE.copy(isLoading = true), awaitItem())
            assertEquals(DEFAULT_STATE.copy(isLoading = false, data = expectedData), awaitItem())
        }

        coVerify { mockRepository.fetchData(any()) }
    }

    /**
     * EventFlow has no replay - MUST call expectNoEvents() first
     */
    @Test
    fun `SubmitClick action should emit NavigateToNext event`() = runTest {
        coEvery { mockRepository.submitData(any()) } returns Result.success(Unit)

        val viewModel = createViewModel()

        viewModel.eventFlow.test {
            expectNoEvents() // CRITICAL for EventFlow
            viewModel.trySendAction(ExampleAction.SubmitClick)
            assertEquals(ExampleEvent.NavigateToNext, awaitItem())
        }
    }

    /**
     * Use stateEventFlow() helper for simultaneous testing
     */
    @Test
    fun `complex action should update state and emit event`() = runTest {
        coEvery { mockRepository.complexOperation(any()) } returns Result.success("result")

        val viewModel = createViewModel()

        viewModel.stateEventFlow(backgroundScope) { stateFlow, eventFlow ->
            assertEquals(DEFAULT_STATE, stateFlow.awaitItem())
            eventFlow.expectNoEvents()

            viewModel.trySendAction(ExampleAction.ComplexAction)

            assertEquals(DEFAULT_STATE.copy(isLoading = true), stateFlow.awaitItem())
            assertEquals(DEFAULT_STATE.copy(data = "result"), stateFlow.awaitItem())
            assertEquals(ExampleEvent.ShowToast("Success!"), eventFlow.awaitItem())
        }
    }

    /**
     * Test state restoration from saved state.
     * Note: Use initialState parameter, NOT SavedStateHandle directly.
     */
    @Test
    fun `initial state from saved state should be preserved`() = runTest {
        // Build complete expected state - always assert full objects
        val savedState = ExampleState(
            isLoading = false,
            data = "restored data",
            errorMessage = null,
        )

        val viewModel = createViewModel(initialState = savedState)

        viewModel.stateFlow.test {
            assertEquals(savedState, awaitItem())
        }
    }

    /**
     * Factory method accepts domain state, NOT SavedStateHandle.
     * This hides Android framework details from test logic.
     */
    private fun createViewModel(
        initialState: ExampleState? = null,
    ): ExampleViewModel = ExampleViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", initialState) },
        repository = mockRepository,
        authDiskSource = mockAuthDiskSource,
    )
}

private val DEFAULT_STATE = ExampleState(
    isLoading = false,
    data = null,
    errorMessage = null,
)

// Example types (normally in separate files)
data class ExampleState(
    val isLoading: Boolean = false,
    val data: String? = null,
    val errorMessage: String? = null,
)

sealed class ExampleAction {
    data object LoadData : ExampleAction()
    data object SubmitClick : ExampleAction()
    data object ComplexAction : ExampleAction()
}

sealed class ExampleEvent {
    data object NavigateToNext : ExampleEvent()
    data class ShowToast(val message: String) : ExampleEvent()
}
