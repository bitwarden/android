package com.bitwarden.testharness.ui.platform.feature.createpassword

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

class CreatePasswordViewModelTest : BaseViewModelTest() {

    private val mockCredentialTestManager = mockk<CredentialTestManager>()

    @Test
    fun `initial state should have default values`() = runTest {
        val viewModel = createViewModel()

        val state = viewModel.stateFlow.value
        assertFalse(state.isLoading)
        assertEquals("", state.username)
        assertEquals("", state.password)
        assertTrue(state.resultText.contains("Ready to create password credential"))
    }

    @Test
    fun `UsernameChanged action updates username state`() = runTest {
        val viewModel = createViewModel()
        val testUsername = "test@example.com"

        viewModel.stateFlow.test {
            // Verify the username is initially empty
            assertEquals("", awaitItem().username)

            viewModel.trySendAction(CreatePasswordAction.UsernameChanged(testUsername))

            val updatedState = awaitItem()
            assertEquals(testUsername, updatedState.username)
        }
    }

    @Test
    fun `PasswordChanged action updates password state`() = runTest {
        val viewModel = createViewModel()
        val testPassword = "SecurePassword123!"

        viewModel.stateFlow.test {
            // Verify the password is initially empty
            assertEquals("", awaitItem().password)

            viewModel.trySendAction(CreatePasswordAction.PasswordChanged(testPassword))

            val updatedState = awaitItem()
            assertEquals(testPassword, updatedState.password)
        }
    }

    @Test
    fun `ExecuteClick with blank username shows validation error`() = runTest {
        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = "",
                password = "password123",
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(
            resultState.resultText.contains(
                "Validation Error: Username and password are required",
            ),
        )
    }

    @Test
    fun `ExecuteClick with blank password shows validation error`() = runTest {
        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = "test@example.com",
                password = "",
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(
            resultState.resultText.contains(
                "Validation Error: Username and password are required",
            ),
        )
    }

    @Test
    fun `ExecuteClick with blank username and password shows validation error`() = runTest {
        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = "",
                password = "",
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("Validation Error"))
        assertTrue(resultState.resultText.contains("Username and password are required"))
    }

    @Test
    fun `ExecuteClick with valid inputs triggers password creation`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Success(
            data = "test-credential-data",
        )

        val viewModel = createViewModel()

        // Set username and password
        viewModel.trySendAction(CreatePasswordAction.UsernameChanged(testUsername))
        viewModel.trySendAction(CreatePasswordAction.PasswordChanged(testPassword))

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        coVerify {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        }
    }

    @Test
    fun `ExecuteClick action sets loading state to true`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = testUsername,
                password = testPassword,
            ),
        )
        viewModel.stateFlow.test {

            skipItems(1)
            viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

            // Should receive loading state
            val loadingState = awaitItem()
            assertTrue(loadingState.isLoading)
            assertTrue(loadingState.resultText.contains("Creating password credential"))

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `Success result updates state with success message`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"
        val testData = "credential-id: 12345"

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Success(
            data = testData,
        )

        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = testUsername,
                password = testPassword,
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
        assertTrue(resultState.resultText.contains(testData))
    }

    @Test
    fun `Error result updates state with error message`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"
        val exception = Exception("Network timeout")

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Error(
            exception = exception,
        )

        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = testUsername,
                password = testPassword,
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
        assertTrue(resultState.resultText.contains("Network timeout"))
    }

    @Test
    fun `Cancelled result updates state with cancelled message`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Cancelled

        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = testUsername,
                password = testPassword,
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("CANCELLED"))
        assertTrue(resultState.resultText.contains("User cancelled"))
    }

    @Test
    fun `ClearResultClick action resets result text`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasswordAction.ClearResultClick)

        val clearedState = viewModel.stateFlow.value
        assertEquals("Result cleared.\n", clearedState.resultText)
    }

    @Test
    fun `Error result without exception does not crash`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Error(
            exception = null,
        )

        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = testUsername,
                password = testPassword,
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("ERROR"))
    }

    @Test
    fun `Success result without data does not crash`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Success(
            data = null,
        )

        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = testUsername,
                password = testPassword,
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("SUCCESS"))
    }

    @Test
    fun `multiple input changes are reflected in state`() = runTest {
        val viewModel = createViewModel()

        viewModel.stateFlow.test {
            skipItems(1) // Skip initial state

            // Change username multiple times
            viewModel.trySendAction(CreatePasswordAction.UsernameChanged("first"))
            assertEquals("first", awaitItem().username)

            viewModel.trySendAction(CreatePasswordAction.UsernameChanged("second"))
            assertEquals("second", awaitItem().username)

            // Change password multiple times
            viewModel.trySendAction(CreatePasswordAction.PasswordChanged("pass1"))
            assertEquals("pass1", awaitItem().password)

            viewModel.trySendAction(CreatePasswordAction.PasswordChanged("pass2"))
            assertEquals("pass2", awaitItem().password)
        }
    }

    @Test
    fun `ExecuteClick does not trigger operation when validation fails`() = runTest {
        val viewModel = createViewModel()

        // Leave username and password blank
        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        // Verify createPassword was never called
        coVerify(exactly = 0) {
            mockCredentialTestManager.createPassword(any(), any(), any())
        }
    }

    @Test
    fun `username with whitespace only fails validation`() = runTest {
        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = "   ",
                password = "ValidPassword123!",
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("Validation Error"))
    }

    @Test
    fun `password with whitespace only fails validation`() = runTest {
        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = "valid@example.com",
                password = "   ",
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        assertFalse(resultState.isLoading)
        assertTrue(resultState.resultText.contains("Validation Error"))
    }

    @Test
    fun `successful operation preserves username and password in state`() = runTest {
        val testUsername = "test@example.com"
        val testPassword = "SecurePassword123!"

        coEvery {
            mockCredentialTestManager.createPassword(
                username = testUsername,
                password = testPassword,
                origin = null,
            )
        } returns CredentialTestResult.Success()

        val viewModel = createViewModel(
            initialState = CreatePasswordState(
                username = testUsername,
                password = testPassword,
            ),
        )

        viewModel.trySendAction(CreatePasswordAction.ExecuteClick)

        val resultState = viewModel.stateFlow.value
        // Verify username and password are still in state after operation
        assertEquals(testUsername, resultState.username)
        assertEquals(testPassword, resultState.password)
    }

    @Test
    fun `BackClick action sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()

        viewModel.trySendAction(CreatePasswordAction.BackClick)

        viewModel.eventFlow.test {
            assertEquals(
                CreatePasswordEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        initialState: CreatePasswordState? = null,
    ): CreatePasswordViewModel {
        return CreatePasswordViewModel(
            credentialTestManager = mockCredentialTestManager,
            savedStateHandle = SavedStateHandle().apply { this["state"] = initialState },
            clock = CLOCK,
        )
    }
}

private val CLOCK = Clock.fixed(
    Instant.parse("2024-10-12T12:00:00Z"),
    ZoneOffset.UTC,
)
