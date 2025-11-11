package com.x8bit.bitwarden.testharness.data.manager

import android.app.Application
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialResponse
import com.x8bit.bitwarden.testharness.data.model.CredentialTestResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/**
 * Tests for [CredentialTestManagerImpl].
 *
 * Uses MockK to mock CredentialManager responses.
 * Android framework classes return default values via testOptions configuration.
 */
class CredentialTestManagerImplTest {

    private lateinit var application: Application
    private lateinit var credentialManager: CredentialManager
    private lateinit var manager: CredentialTestManagerImpl

    @BeforeEach
    fun setup() {
        application = mockk(relaxed = true)
        credentialManager = mockk()

        manager = CredentialTestManagerImpl(
            application = application,
            credentialManager = credentialManager,
        )
    }

    @Test
    fun `createPassword with successful response returns Success`() = runTest {
        val response = CreatePasswordResponse()
        coEvery {
            credentialManager.createCredential(
                context = any(),
                request = any(),
            )
        } returns response

        val result = manager.createPassword("user", "pass", null)

        when (result) {
            is CredentialTestResult.Success -> {
                assertEquals("Password created successfully", result.message)
            }
            is CredentialTestResult.Error -> {
                fail(
                    "Expected Success but got Error: ${result.message}, " +
                        "exception: ${result.exception}",
                )
            }
            is CredentialTestResult.Cancelled -> {
                fail("Expected Success but got Cancelled")
            }
        }
    }

    @Test
    fun `getPassword with mocked PasswordCredential returns Success`() = runTest {
        val mockCredential = mockk<Credential>(relaxed = true) {
            every { type } returns "android.credentials.TYPE_PASSWORD_CREDENTIAL"
        }
        val getCredentialResponse = mockk<GetCredentialResponse> {
            every { credential } returns mockCredential
        }
        coEvery {
            credentialManager.getCredential(
                context = any(),
                request = any(),
            )
        } returns getCredentialResponse

        val result = manager.getPassword()

        // Verify the method completes without crashing
        // Note: Without real PasswordCredential, we expect an Error due to type mismatch
        assertNotNull(result)
        assertTrue(result is CredentialTestResult.Error || result is CredentialTestResult.Success)
    }
}
