package com.bitwarden.testharness.data.manager

import android.app.Application
import androidx.credentials.CreatePasswordResponse
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialResponse
import androidx.credentials.PasswordCredential
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.bitwarden.testharness.data.model.CredentialTestResult
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.unmockkConstructor
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CredentialTestManagerTest {

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

    @Nested
    inner class CreatePassword {

        @Test
        fun `success without origin returns Success`() = runTest {
            val response = CreatePasswordResponse()
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.createPassword("user@example.com", "SecurePass123!", null)

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("user@example.com") == true)
                    assertTrue(result.data?.contains("Origin: null") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `success with origin returns Success`() = runTest {
            val response = CreatePasswordResponse()
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.createPassword(
                username = "user@example.com",
                password = "SecurePass123!",
                origin = "https://example.com",
            )

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("https://example.com") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `cancellation exception returns Cancelled`() = runTest {
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } throws CreateCredentialCancellationException("User cancelled")

            val result = manager.createPassword("user", "pass", null)

            assertTrue(result is CredentialTestResult.Cancelled)
        }

        @Test
        fun `create exception returns Error`() = runTest {
            val exception = mockk<CreateCredentialException>(relaxed = true) {
                every { message } returns "Failed to create"
            }
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } throws exception

            val result = manager.createPassword("user", "pass", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertEquals(exception, result.exception)
                }

                else -> fail("Expected Error but got $result")
            }
        }

        @Test
        fun `unexpected response type returns Error`() = runTest {
            val unexpectedResponse = mockk<CreatePublicKeyCredentialResponse>()
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } returns unexpectedResponse

            val result = manager.createPassword("user", "pass", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertTrue(result.exception is IllegalStateException)
                    assertTrue(
                        result.exception?.message?.contains("Unexpected response type") == true,
                    )
                }

                else -> fail("Expected Error but got $result")
            }
        }
    }

    @Nested
    inner class GetPassword {

        @Test
        fun `success with PasswordCredential returns Success`() = runTest {
            val passwordCredential = mockk<PasswordCredential> {
                every { id } returns "user@example.com"
                every { password } returns "SecurePass123!"
            }
            val response = mockk<GetCredentialResponse> {
                every { credential } returns passwordCredential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPassword()

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("user@example.com") == true)
                    assertTrue(result.data?.contains("SecurePass123!") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `cancellation exception returns Cancelled`() = runTest {
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } throws GetCredentialCancellationException("User cancelled")

            val result = manager.getPassword()

            assertTrue(result is CredentialTestResult.Cancelled)
        }

        @Test
        fun `get exception returns Error`() = runTest {
            val exception = mockk<GetCredentialException>(relaxed = true) {
                every { message } returns "Failed to get credential"
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } throws exception

            val result = manager.getPassword()

            when (result) {
                is CredentialTestResult.Error -> {
                    assertEquals(exception, result.exception)
                }

                else -> fail("Expected Error but got $result")
            }
        }

        @Test
        fun `unexpected credential type returns Error`() = runTest {
            val unexpectedCredential = mockk<PublicKeyCredential>()
            val response = mockk<GetCredentialResponse> {
                every { credential } returns unexpectedCredential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPassword()

            when (result) {
                is CredentialTestResult.Error -> {
                    assertTrue(result.exception is IllegalStateException)
                    assertTrue(
                        result.exception?.message?.contains("Unexpected credential type") == true,
                    )
                }

                else -> fail("Expected Error but got $result")
            }
        }
    }

    @Nested
    inner class CreatePasskey {

        @BeforeEach
        fun setupConstructorMock() {
            // Mock CreatePublicKeyCredentialRequest constructor to bypass JSON validation
            mockkConstructor(JSONObject::class)
            every { anyConstructed<JSONObject>().getJSONObject(any()) } returns mockk {
                every { getString("name") } returns "user"
                every { getString("displayName") } returns "user"
                every { isNull("displayName") } returns false
            }
        }

        @AfterEach
        fun teardownConstructorMock() {
            unmockkConstructor(CreatePublicKeyCredentialRequest::class)
        }

        @Test
        fun `success without origin returns Success`() = runTest {
            val response = mockk<CreatePublicKeyCredentialResponse> {
                every { registrationResponseJson } returns """{"id":"test-credential-id"}"""
            }
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.createPasskey(
                username = "user@example.com",
                rpId = "example.com",
                origin = null,
            )

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("example.com") == true)
                    assertTrue(result.data?.contains("Origin: null") == true)
                    assertTrue(result.data?.contains("test-credential-id") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `success with origin returns Success`() = runTest {
            val response = mockk<CreatePublicKeyCredentialResponse> {
                every { registrationResponseJson } returns """{"id":"test-credential-id"}"""
            }
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.createPasskey(
                username = "user@example.com",
                rpId = "example.com",
                origin = "https://example.com",
            )

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("https://example.com") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `cancellation exception returns Cancelled`() = runTest {
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } throws CreateCredentialCancellationException("User cancelled")

            val result = manager.createPasskey("user", "example.com", null)

            assertTrue(result is CredentialTestResult.Cancelled)
        }

        @Test
        fun `create exception returns Error`() = runTest {
            val exception = mockk<CreateCredentialException>(relaxed = true) {
                every { message } returns "Failed to create passkey"
            }
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } throws exception

            val result = manager.createPasskey("user", "example.com", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertEquals(exception, result.exception)
                }

                else -> fail("Expected Error but got $result")
            }
        }

        @Test
        fun `unexpected response type returns Error`() = runTest {
            val unexpectedResponse = CreatePasswordResponse()
            coEvery {
                credentialManager.createCredential(
                    context = any(),
                    request = any(),
                )
            } returns unexpectedResponse

            val result = manager.createPasskey("user", "example.com", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertTrue(result.exception is IllegalStateException)
                    assertTrue(
                        result.exception?.message?.contains("Unexpected response type") == true,
                    )
                }

                else -> fail("Expected Error but got $result")
            }
        }
    }

    @Nested
    inner class GetPasskey {

        @Test
        fun `success without origin returns Success`() = runTest {
            val credential = mockk<PublicKeyCredential> {
                every { authenticationResponseJson } returns """{"id":"auth-response"}"""
            }
            val response = mockk<GetCredentialResponse> {
                every { this@mockk.credential } returns credential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasskey(rpId = "example.com", origin = null)

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("example.com") == true)
                    assertTrue(result.data?.contains("Origin: null") == true)
                    assertTrue(result.data?.contains("auth-response") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `success with origin returns Success`() = runTest {
            val credential = mockk<PublicKeyCredential> {
                every { authenticationResponseJson } returns """{"id":"auth-response"}"""
            }
            val response = mockk<GetCredentialResponse> {
                every { this@mockk.credential } returns credential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasskey(
                rpId = "example.com",
                origin = "https://example.com",
            )

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("https://example.com") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `cancellation exception returns Cancelled`() = runTest {
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } throws GetCredentialCancellationException("User cancelled")

            val result = manager.getPasskey("example.com", null)

            assertTrue(result is CredentialTestResult.Cancelled)
        }

        @Test
        fun `get exception returns Error`() = runTest {
            val exception = mockk<GetCredentialException>(relaxed = true) {
                every { message } returns "Failed to authenticate"
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } throws exception

            val result = manager.getPasskey("example.com", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertEquals(exception, result.exception)
                }

                else -> fail("Expected Error but got $result")
            }
        }

        @Test
        fun `unexpected credential type returns Error`() = runTest {
            val unexpectedCredential = mockk<PasswordCredential>()
            val response = mockk<GetCredentialResponse> {
                every { credential } returns unexpectedCredential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasskey("example.com", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertTrue(result.exception is IllegalStateException)
                    assertTrue(
                        result.exception?.message?.contains("Unexpected credential type") == true,
                    )
                }

                else -> fail("Expected Error but got $result")
            }
        }
    }

    @Nested
    inner class GetPasswordOrPasskey {

        @Test
        fun `success with PasswordCredential without origin returns Success`() = runTest {
            val credential = mockk<PasswordCredential> {
                every { id } returns "user@example.com"
                every { password } returns "SecurePass123!"
            }
            val response = mockk<GetCredentialResponse> {
                every { this@mockk.credential } returns credential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasswordOrPasskey(rpId = "example.com", origin = null)

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("Type: PASSWORD") == true)
                    assertTrue(result.data?.contains("user@example.com") == true)
                    assertTrue(result.data?.contains("SecurePass123!") == true)
                    assertTrue(result.data?.contains("Origin: null") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `success with PasswordCredential with origin returns Success`() = runTest {
            val credential = mockk<PasswordCredential> {
                every { id } returns "user@example.com"
                every { password } returns "SecurePass123!"
            }
            val response = mockk<GetCredentialResponse> {
                every { this@mockk.credential } returns credential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = "https://example.com",
            )

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("https://example.com") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `success with PublicKeyCredential without origin returns Success`() = runTest {
            val credential = mockk<PublicKeyCredential> {
                every { authenticationResponseJson } returns """{"id":"passkey-auth"}"""
            }
            val response = mockk<GetCredentialResponse> {
                every { this@mockk.credential } returns credential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasswordOrPasskey(rpId = "example.com", origin = null)

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("Type: PASSKEY") == true)
                    assertTrue(result.data?.contains("Origin: null") == true)
                    assertTrue(result.data?.contains("passkey-auth") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `success with PublicKeyCredential with origin returns Success`() = runTest {
            val credential = mockk<PublicKeyCredential> {
                every { authenticationResponseJson } returns """{"id":"passkey-auth"}"""
            }
            val response = mockk<GetCredentialResponse> {
                every { this@mockk.credential } returns credential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasswordOrPasskey(
                rpId = "example.com",
                origin = "https://example.com",
            )

            when (result) {
                is CredentialTestResult.Success -> {
                    assertTrue(result.data?.contains("https://example.com") == true)
                }

                else -> fail("Expected Success but got $result")
            }
        }

        @Test
        fun `cancellation exception returns Cancelled`() = runTest {
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } throws GetCredentialCancellationException("User cancelled")

            val result = manager.getPasswordOrPasskey("example.com", null)

            assertTrue(result is CredentialTestResult.Cancelled)
        }

        @Test
        fun `get exception returns Error`() = runTest {
            val exception = mockk<GetCredentialException>(relaxed = true) {
                every { message } returns "Failed to get credential"
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } throws exception

            val result = manager.getPasswordOrPasskey("example.com", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertEquals(exception, result.exception)
                }

                else -> fail("Expected Error but got $result")
            }
        }

        @Test
        fun `unexpected credential type returns Error`() = runTest {
            val unexpectedCredential = mockk<Credential>()
            val response = mockk<GetCredentialResponse> {
                every { credential } returns unexpectedCredential
            }
            coEvery {
                credentialManager.getCredential(
                    context = any(),
                    request = any(),
                )
            } returns response

            val result = manager.getPasswordOrPasskey("example.com", null)

            when (result) {
                is CredentialTestResult.Error -> {
                    assertTrue(result.exception is IllegalStateException)
                    assertTrue(
                        result.exception?.message?.contains("Unexpected credential type") == true,
                    )
                }

                else -> fail("Expected Error but got $result")
            }
        }
    }
}
