package com.bitwarden.testharness.ui.platform.feature.createpasskey

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

class CreatePasskeyViewModelTest : BaseViewModelTest() {

    private val mockCredentialTestManager = mockk<CredentialTestManager>()
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel(): CreatePasskeyViewModel {
        return CreatePasskeyViewModel(
            credentialTestManager = mockCredentialTestManager,
            savedStateHandle = savedStateHandle,
            clock = CLOCK,
        )
    }

    @Test
    fun `initial state should have default values`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.stateFlow.value
        assertEquals("", state.username)
        assertEquals("", state.rpId)
        assertEquals("", state.origin)
        assertFalse(state.isLoading)
        assertTrue(state.resultText.contains("Ready to create passkey credential"))
    }

    @Test
    fun `UsernameChanged action updates username in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))

        val updatedState = viewModel.stateFlow.value
        assertEquals("test-user", updatedState.username)
    }

    @Test
    fun `RpIdChanged action updates rpId in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))

        val updatedState = viewModel.stateFlow.value
        assertEquals("example.com", updatedState.rpId)
    }

    @Test
    fun `OriginChanged action updates origin in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.OriginChanged("https://example.com"))

        val updatedState = viewModel.stateFlow.value
        assertEquals("https://example.com", updatedState.origin)
    }

    @Test
    fun `ExecuteClick action with blank username shows validation error`() = runTest {
        val viewModel = createViewModel()

        // Set rpId but leave username blank
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("Validation Error"))
        assertTrue(
            resultState.resultText.contains("Username and Relying Party ID are required"),
        )

        // Verify manager was never called
        coVerify(exactly = 0) {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        }
    }

    @Test
    fun `ExecuteClick action with blank rpId shows validation error`() = runTest {
        val viewModel = createViewModel()

        // Set username but leave rpId blank
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("Validation Error"))
        assertTrue(
            resultState.resultText.contains("Username and Relying Party ID are required"),
        )

        // Verify manager was never called
        coVerify(exactly = 0) {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        }
    }

    @Test
    fun `ExecuteClick action with blank username and rpId shows validation error`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("Validation Error"))
        assertTrue(
            resultState.resultText.contains("Username and Relying Party ID are required"),
        )

        // Verify manager was never called
        coVerify(exactly = 0) {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        }
    }

    @Test
    fun `ExecuteClick action triggers passkey creation with required fields`() = runTest {
        coEvery {
            mockCredentialTestManager.createPasskey(
                username = "test-user",
                rpId = "example.com",
                origin = null,
            )
        } returns CredentialTestResult.Success(
            data = "test-passkey-data",
        )

        val viewModel = createViewModel()

        // Set required fields
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))

        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        // Wait for async operation to complete
        testScheduler.advanceUntilIdle()

        coVerify {
            mockCredentialTestManager.createPasskey(
                username = "test-user",
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `ExecuteClick action triggers passkey creation with origin when provided`() = runTest {
        coEvery {
            mockCredentialTestManager.createPasskey(
                username = "test-user",
                rpId = "example.com",
                origin = "https://example.com",
            )
        } returns CredentialTestResult.Success(
            data = "test-passkey-data",
        )

        val viewModel = createViewModel()

        // Set all fields including optional origin
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.OriginChanged("https://example.com"))

        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        // Wait for async operation to complete
        testScheduler.advanceUntilIdle()

        coVerify {
            mockCredentialTestManager.createPasskey(
                username = "test-user",
                rpId = "example.com",
                origin = "https://example.com",
            )
        }
    }

    @Test
    fun `ExecuteClick action with blank origin passes null to manager`() = runTest {
        coEvery {
            mockCredentialTestManager.createPasskey(
                username = "test-user",
                rpId = "example.com",
                origin = null,
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()

        // Set required fields and explicitly blank origin
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.OriginChanged("   ")) // Whitespace only

        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        // Wait for async operation to complete
        testScheduler.advanceUntilIdle()

        coVerify {
            mockCredentialTestManager.createPasskey(
                username = "test-user",
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `ExecuteClick action sets loading state to true`() = runTest {
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } coAnswers {
            kotlinx.coroutines.delay(100)
            CredentialTestResult.Success()
        }

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            skipItems(1) // Skip initial state

            // Set required fields
            viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
            awaitItem() // Consume username change state

            viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
            awaitItem() // Consume rpId change state

            viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

            // Should receive loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertTrue(loadingState.resultText.contains("Creating passkey credential"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success result updates state with success message`() = runTest {
        val successMessage = "Passkey created successfully"
        val testData = "credential_id: ABC123"
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Success(
            data = testData,
        )

        val viewModel = createViewModel()

        // Set required fields
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
        assertTrue(resultState.resultText.contains(testData))
    }

    @Test
    fun `Error result updates state with error message`() = runTest {
        val exception = Exception("Invalid relying party")
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Error(
            exception = exception,
        )

        val viewModel = createViewModel()

        // Set required fields
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains("Invalid relying party"))
    }

    @Test
    fun `Cancelled result updates state with cancelled message`() = runTest {
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Cancelled

        val viewModel = createViewModel()

        // Set required fields
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("CANCELLED"))
        assertTrue(resultState.resultText.contains("User cancelled"))
    }

    @Test
    fun `ClearResultClick action resets result text`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.ClearResultClick)

        val clearedState = viewModel.stateFlow.value
        assertEquals("Result cleared.\n", clearedState.resultText)
    }

    @Test
    fun `ClearResultClick preserves input field values`() = runTest {
        val viewModel = createViewModel()

        // Set field values
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.OriginChanged("https://example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ClearResultClick)

        val clearedState = viewModel.stateFlow.value
        assertEquals("Result cleared.\n", clearedState.resultText)
        assertEquals("test-user", clearedState.username)
        assertEquals("example.com", clearedState.rpId)
        assertEquals("https://example.com", clearedState.origin)
    }

    @Test
    fun `Error result without exception does not crash`() = runTest {
        val errorMessage = "Unknown error"
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Error(
            exception = null,
        )

        val viewModel = createViewModel()

        // Set required fields
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains(errorMessage))
    }

    @Test
    fun `Success result without data does not crash`() = runTest {
        val successMessage = "Passkey created"
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Success(
            data = null,
        )

        val viewModel = createViewModel()

        // Set required fields
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
    }

    @Test
    fun `state is persisted to SavedStateHandle`() = runTest {
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(CreatePasskeyAction.OriginChanged("https://example.com"))
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        // Wait for state to update
        testScheduler.advanceUntilIdle()

        val persistedState = savedStateHandle.get<CreatePasskeyState>("state")
        assertEquals(viewModel.stateFlow.value, persistedState)
    }

    @Test
    fun `multiple field updates are all persisted`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("user1"))
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("user2"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))

        // Wait for state updates
        testScheduler.advanceUntilIdle()

        val persistedState = savedStateHandle.get<CreatePasskeyState>("state")
        assertEquals("user2", persistedState?.username)
        assertEquals("example.com", persistedState?.rpId)
    }

    @Test
    fun `validation error message appends to existing result text`() = runTest {
        val viewModel = createViewModel()

        val initialText = viewModel.stateFlow.value.resultText

        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)

        val errorState = viewModel.stateFlow.value
        assertTrue(errorState.resultText.contains(initialText))
        assertTrue(errorState.resultText.contains("Validation Error"))
    }

    @Test
    fun `multiple operations append to result text`() = runTest {
        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Success(
            data = "First operation",
        )

        val viewModel = createViewModel()

        // Set required fields
        viewModel.trySendAction(CreatePasskeyAction.UsernameChanged("test-user"))
        viewModel.trySendAction(CreatePasskeyAction.RpIdChanged("example.com"))

        // First operation
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)
        testScheduler.advanceUntilIdle()

        coEvery {
            mockCredentialTestManager.createPasskey(any(), any(), any())
        } returns CredentialTestResult.Success(
            data = "Second operation",
        )

        // Second operation
        viewModel.trySendAction(CreatePasskeyAction.ExecuteClick)
        testScheduler.advanceUntilIdle()

        val finalState = viewModel.stateFlow.value
        assertTrue(finalState.resultText.contains("First operation"))
        assertTrue(finalState.resultText.contains("Second operation"))
    }

    @Test
    fun `BackClick action sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasskeyAction.BackClick)

        viewModel.eventFlow.test {
            assertEquals(
                CreatePasskeyEvent.NavigateBack,
                awaitItem(),
            )
        }
    }
}

private val CLOCK = Clock.fixed(
    Instant.parse("2024-10-12T12:00:00Z"),
    ZoneOffset.UTC,
)
