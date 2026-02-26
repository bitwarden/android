package com.bitwarden.testharness.ui.platform.feature.getpassword

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.testharness.data.manager.CredentialTestManager
import com.bitwarden.testharness.data.model.CredentialTestResult
import com.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GetPasswordViewModelTest : BaseViewModelTest() {

    private val mockCredentialTestManager = mockk<CredentialTestManager>()
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel(): GetPasswordViewModel {
        return GetPasswordViewModel(
            credentialTestManager = mockCredentialTestManager,
            savedStateHandle = savedStateHandle,
            clock = CLOCK,
        )
    }

    @Test
    fun `initial state should have default values`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertTrue(state.resultText.contains("Ready to retrieve password"))
    }

    @Test
    fun `ExecuteClick action triggers password retrieval`() = runTest {
        coEvery { mockCredentialTestManager.getPassword() } returns CredentialTestResult.Success(
            data = "test-password-data",
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ExecuteClick)

        coVerify { mockCredentialTestManager.getPassword() }
    }

    @Test
    fun `ExecuteClick action sets loading state to true`() = runTest {
        coEvery { mockCredentialTestManager.getPassword() } coAnswers {
            kotlinx.coroutines.delay(100)
            CredentialTestResult.Success()
        }

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            skipItems(1) // Skip initial state

            viewModel.trySendAction(GetPasswordAction.ExecuteClick)

            // Should receive loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertTrue(loadingState.resultText.contains("Starting password retrieval"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success result updates state with success message`() = runTest {
        val successMessage = "Password retrieved successfully"
        val testData = "username: test@example.com"
        coEvery { mockCredentialTestManager.getPassword() } returns CredentialTestResult.Success(
            data = testData,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
        assertTrue(resultState.resultText.contains(testData))
    }

    @Test
    fun `Error result updates state with error message`() = runTest {
        val exception = Exception("Network error")
        coEvery { mockCredentialTestManager.getPassword() } returns CredentialTestResult.Error(
            exception = exception,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains("Network error"))
    }

    @Test
    fun `Cancelled result updates state with cancelled message`() = runTest {
        coEvery { mockCredentialTestManager.getPassword() } returns CredentialTestResult.Cancelled

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("CANCELLED"))
        assertTrue(resultState.resultText.contains("User cancelled"))
    }

    @Test
    fun `ClearResultClick action resets result text`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ClearResultClick)

        val clearedState = viewModel.stateFlow.value
        assertEquals("Result cleared.\n", clearedState.resultText)
    }

    @Test
    fun `Error result without exception does not crash`() = runTest {
        val errorMessage = "Unknown error"
        coEvery { mockCredentialTestManager.getPassword() } returns CredentialTestResult.Error(
            exception = null,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains(errorMessage))
    }

    @Test
    fun `Success result without data does not crash`() = runTest {
        val successMessage = "Password retrieved"
        coEvery { mockCredentialTestManager.getPassword() } returns CredentialTestResult.Success(
            data = null,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
    }

    @Test
    fun `state is persisted to SavedStateHandle`() = runTest {
        coEvery { mockCredentialTestManager.getPassword() } returns CredentialTestResult.Success()

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.ExecuteClick)

        // Wait for state to update
        testScheduler.advanceUntilIdle()

        val persistedState = savedStateHandle.get<GetPasswordState>("state")
        assertEquals(viewModel.stateFlow.value, persistedState)
    }

    @Test
    fun `BackClick action sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordAction.BackClick)

        viewModel.eventFlow.test {
            assertEquals(
                GetPasswordEvent.NavigateBack,
                awaitItem(),
            )
        }
    }
}

private val CLOCK = Clock.fixed(
    Instant.parse("2024-10-12T12:00:00Z"),
    ZoneOffset.UTC,
)
