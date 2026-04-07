package com.bitwarden.testharness.ui.platform.feature.getpasskey

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

class GetPasskeyViewModelTest : BaseViewModelTest() {

    private val mockCredentialTestManager = mockk<CredentialTestManager>()
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel(): GetPasskeyViewModel {
        return GetPasskeyViewModel(
            credentialTestManager = mockCredentialTestManager,
            savedStateHandle = savedStateHandle,
            clock = CLOCK,
        )
    }

    @Test
    fun `initial state should have default values`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.stateFlow.value
        assertEquals("", state.rpId)
        assertEquals("", state.origin)
        assertFalse(state.isLoading)
        assertTrue(state.resultText.contains("Ready to authenticate passkey"))
    }

    @Test
    fun `RpIdChanged action updates rpId in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))

        val updatedState = viewModel.stateFlow.value
        assertEquals("example.com", updatedState.rpId)
    }

    @Test
    fun `OriginChanged action updates origin in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.OriginChanged("https://example.com"))

        val updatedState = viewModel.stateFlow.value
        assertEquals("https://example.com", updatedState.origin)
    }

    @Test
    fun `ExecuteClick with blank rpId shows validation error`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        val errorState = viewModel.stateFlow.value
        assertFalse(errorState.isLoading)
        assertTrue(errorState.resultText.contains("Validation Error"))
        assertTrue(errorState.resultText.contains("Relying Party ID is required"))
    }

    @Test
    fun `ExecuteClick with valid rpId triggers passkey authentication`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasskey(
                rpId = "example.com",
                origin = null,
            )
        } returns CredentialTestResult.Success(
            data = "test-passkey-data",
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        coVerify {
            mockCredentialTestManager.getPasskey(
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `ExecuteClick with rpId and origin passes both parameters`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasskey(
                rpId = "example.com",
                origin = "https://example.com",
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.OriginChanged("https://example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        coVerify {
            mockCredentialTestManager.getPasskey(
                rpId = "example.com",
                origin = "https://example.com",
            )
        }
    }

    @Test
    fun `ExecuteClick with blank origin passes null origin`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasskey(
                rpId = "example.com",
                origin = null,
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.OriginChanged(""))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        coVerify {
            mockCredentialTestManager.getPasskey(
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `ExecuteClick action sets loading state to true`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasskey(any(), any())
        } coAnswers {
            kotlinx.coroutines.delay(100)
            CredentialTestResult.Success()
        }

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            skipItems(1) // Skip initial state

            viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))

            skipItems(1) // Skip rpId update

            viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

            // Should receive loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertTrue(loadingState.resultText.contains("Starting passkey authentication"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success result updates state with success message`() = runTest {
        val successMessage = "Passkey authenticated successfully"
        val testData = "credential-id: test-credential-123"
        coEvery {
            mockCredentialTestManager.getPasskey(any(), any())
        } returns CredentialTestResult.Success(
            data = testData,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
        assertTrue(resultState.resultText.contains(testData))
    }

    @Test
    fun `Error result updates state with error message`() = runTest {
        val exception = Exception("Invalid passkey")
        coEvery {
            mockCredentialTestManager.getPasskey(any(), any())
        } returns CredentialTestResult.Error(
            exception = exception,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains("Invalid passkey"))
    }

    @Test
    fun `Cancelled result updates state with cancelled message`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasskey(any(), any())
        } returns CredentialTestResult.Cancelled

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("CANCELLED"))
        assertTrue(resultState.resultText.contains("User cancelled"))
    }

    @Test
    fun `ClearResultClick action resets result text`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.ClearResultClick)

        val clearedState = viewModel.stateFlow.value
        assertEquals("Result cleared.\n", clearedState.resultText)
    }

    @Test
    fun `Error result without exception does not crash`() = runTest {
        val errorMessage = "Unknown error"
        coEvery {
            mockCredentialTestManager.getPasskey(any(), any())
        } returns CredentialTestResult.Error(
            exception = null,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains(errorMessage))
    }

    @Test
    fun `Success result without data does not crash`() = runTest {
        val successMessage = "Passkey authenticated"
        coEvery {
            mockCredentialTestManager.getPasskey(any(), any())
        } returns CredentialTestResult.Success(
            data = null,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
    }

    @Test
    fun `state is persisted to SavedStateHandle`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasskey(any(), any())
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasskeyAction.ExecuteClick)

        // Wait for state to update
        testScheduler.advanceUntilIdle()

        val persistedState = savedStateHandle.get<GetPasskeyState>("state")
        assertEquals(viewModel.stateFlow.value, persistedState)
    }

    @Test
    fun `BackClick action sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasskeyAction.BackClick)

        viewModel.eventFlow.test {
            assertEquals(
                GetPasskeyEvent.NavigateBack,
                awaitItem(),
            )
        }
    }
}

private val CLOCK = Clock.fixed(
    Instant.parse("2024-10-12T12:00:00Z"),
    ZoneOffset.UTC,
)
