/**
 * Complete ViewModel Test Example
 *
 * Key patterns demonstrated:
 * - Extending BaseViewModelTest
 * - Testing StateFlow with Turbine
 * - Testing EventFlow with Turbine
 * - Using stateEventFlow() for simultaneous testing
 * - MockK mocking patterns
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

    // SavedStateHandle with initial state
    private val savedStateHandle = SavedStateHandle(
        mapOf("exampleArg" to "initialValue"),
    )

    private val defaultState = ExampleState(
        isLoading = false,
        data = null,
        errorMessage = null,
    )

    /**
     * StateFlow has replay=1, so first awaitItem() returns current state
     */
    @Test
    fun `initial state should be default state`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            assertEquals(defaultState, awaitItem())
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
            assertEquals(defaultState, awaitItem())

            viewModel.trySendAction(ExampleAction.LoadData)

            assertEquals(defaultState.copy(isLoading = true), awaitItem())
            assertEquals(defaultState.copy(isLoading = false, data = expectedData), awaitItem())
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
            assertEquals(defaultState, stateFlow.awaitItem())
            eventFlow.expectNoEvents()

            viewModel.trySendAction(ExampleAction.ComplexAction)

            assertEquals(defaultState.copy(isLoading = true), stateFlow.awaitItem())
            assertEquals(defaultState.copy(data = "result"), stateFlow.awaitItem())
            assertEquals(ExampleEvent.ShowToast("Success!"), eventFlow.awaitItem())
        }
    }

    private fun createViewModel(): ExampleViewModel = ExampleViewModel(
        savedStateHandle = savedStateHandle,
        repository = mockRepository,
        authDiskSource = mockAuthDiskSource,
    )
}

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
