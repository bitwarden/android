package com.bitwarden.testharness.ui.platform.feature.getpasswordorpasskey

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

class GetPasswordOrPasskeyViewModelTest : BaseViewModelTest() {

    private val mockCredentialTestManager = mockk<CredentialTestManager>()
    private val savedStateHandle = SavedStateHandle()

    private fun createViewModel(): GetPasswordOrPasskeyViewModel {
        return GetPasswordOrPasskeyViewModel(
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
        assertTrue(state.resultText.contains("Ready to retrieve password or passkey"))
    }

    @Test
    fun `RpIdChanged action updates rpId in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))

        val state = viewModel.stateFlow.value
        assertEquals("example.com", state.rpId)
    }

    @Test
    fun `OriginChanged action updates origin in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.OriginChanged("https://app.example.com"))

        val state = viewModel.stateFlow.value
        assertEquals("https://app.example.com", state.origin)
    }

    @Test
    fun `ExecuteClick with blank rpId shows validation error`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        val errorState = viewModel.stateFlow.value
        assertFalse(errorState.isLoading)
        assertTrue(errorState.resultText.contains("Validation Error"))
        assertTrue(errorState.resultText.contains("Relying Party ID is required"))
    }

    @Test
    fun `ExecuteClick with blank rpId does not call credentialTestManager`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        testScheduler.advanceUntilIdle()

        coVerify(exactly = 0) { mockCredentialTestManager.getPasswordOrPasskey(any(), any()) }
    }

    @Test
    fun `ExecuteClick with valid rpId triggers credential retrieval`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = null,
            )
        } returns CredentialTestResult.Success(
            data = "test-credential-data",
        )

        val viewModel = createViewModel()
        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        testScheduler.advanceUntilIdle()

        coVerify {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `ExecuteClick with valid rpId and origin passes both parameters`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = "https://app.example.com",
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()
        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.OriginChanged("https://app.example.com"))

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        testScheduler.advanceUntilIdle()

        coVerify {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = "https://app.example.com",
            )
        }
    }

    @Test
    fun `ExecuteClick with blank origin passes null to credentialTestManager`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = null,
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()
        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.OriginChanged(""))

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        testScheduler.advanceUntilIdle()

        coVerify {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `ExecuteClick action sets loading state to true`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } coAnswers {
            kotlinx.coroutines.delay(100)
            CredentialTestResult.Success()
        }

        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            skipItems(1) // Skip initial state

            viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
            awaitItem() // Consume rpId change

            viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

            // Should receive loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertTrue(loadingState.resultText.contains("Starting credential retrieval"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success result updates state with success message`() = runTest {
        val successMessage = "Credential retrieved successfully"
        val testData = "type: password, username: test@example.com"
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } returns CredentialTestResult.Success(
            data = testData,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
        assertTrue(resultState.resultText.contains(testData))
    }

    @Test
    fun `Error result updates state with error message`() = runTest {
        val exception = Exception("Network error")
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } returns CredentialTestResult.Error(
            exception = exception,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains("Network error"))
    }

    @Test
    fun `Cancelled result updates state with cancelled message`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } returns CredentialTestResult.Cancelled

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("CANCELLED"))
        assertTrue(resultState.resultText.contains("User cancelled"))
    }

    @Test
    fun `ClearResultClick action resets result text`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ClearResultClick)

        val clearedState = viewModel.stateFlow.value
        assertEquals("Result cleared.\n", clearedState.resultText)
    }

    @Test
    fun `Error result without exception does not crash`() = runTest {
        val errorMessage = "Unknown error"
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } returns CredentialTestResult.Error(
            exception = null,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains(errorMessage))
    }

    @Test
    fun `Success result without data does not crash`() = runTest {
        val successMessage = "Credential retrieved"
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } returns CredentialTestResult.Success(
            data = null,
        )

        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(successMessage))
    }

    @Test
    fun `state is persisted to SavedStateHandle`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()
        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        // Wait for state to update
        testScheduler.advanceUntilIdle()

        val persistedState = savedStateHandle.get<GetPasswordOrPasskeyState>("state")
        assertEquals(viewModel.stateFlow.value, persistedState)
    }

    @Test
    fun `multiple input changes update state correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.OriginChanged("https://app.example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("newdomain.com"))

        val state = viewModel.stateFlow.value
        assertEquals("newdomain.com", state.rpId)
        assertEquals("https://app.example.com", state.origin)
    }

    @Test
    fun `validation error appends to existing result text`() = runTest {
        val viewModel = createViewModel()

        // First set some existing result text
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ClearResultClick)

        // Try to execute without rpId
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        val errorState = viewModel.stateFlow.value
        assertTrue(errorState.resultText.contains("Result cleared"))
        assertTrue(errorState.resultText.contains("Validation Error"))
    }

    @Test
    fun `successful execution after validation error works correctly`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(any(), any())
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()

        // First try without rpId - should fail validation
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)
        testScheduler.advanceUntilIdle()

        // Now set rpId and try again - should succeed
        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)
        testScheduler.advanceUntilIdle()

        coVerify(exactly = 1) {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `whitespace-only origin is treated as null`() = runTest {
        coEvery {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = null,
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel()
        viewModel.trySendAction(GetPasswordOrPasskeyAction.RpIdChanged("example.com"))
        viewModel.trySendAction(GetPasswordOrPasskeyAction.OriginChanged("   "))

        viewModel.trySendAction(GetPasswordOrPasskeyAction.ExecuteClick)

        testScheduler.advanceUntilIdle()

        coVerify {
            mockCredentialTestManager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = null,
            )
        }
    }

    @Test
    fun `BackClick action sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(GetPasswordOrPasskeyAction.BackClick)

        viewModel.eventFlow.test {
            assertEquals(
                GetPasswordOrPasskeyEvent.NavigateBack,
                awaitItem(),
            )
        }
    }
}

private val CLOCK = Clock.fixed(
    Instant.parse("2024-10-12T12:00:00Z"),
    ZoneOffset.UTC,
)
