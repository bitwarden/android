package com.x8bit.bitwarden.data.credentials.util

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.BiometricPromptResult
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_CREDENTIAL_ID
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_USER_ID
import com.x8bit.bitwarden.data.credentials.manager.EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class CredentialManagerIntentUtilsTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(::isBuildVersionAtLeast)
        mockkObject(
            PendingIntentHandler.Companion,
            BeginGetCredentialRequest.Companion,
            ProviderCreateCredentialRequest.Companion,
            ProviderGetCredentialRequest.Companion,
        )
        every { isBuildVersionAtLeast(any()) } returns true
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
        unmockkObject(
            BeginGetCredentialRequest.Companion,
            PendingIntentHandler.Companion,
            ProviderCreateCredentialRequest.Companion,
            ProviderGetCredentialRequest.Companion,
        )
    }

    @Test
    fun `getCreateCredentialRequestOrNull should return CreateCredentialRequest when present`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
        }

        every { ProviderCreateCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        } returns mockk(relaxed = true) {
            every { biometricPromptResult } returns mockk(relaxed = true) {
                every { isSuccessful } returns false
            }
        }

        val createRequest = intent.getCreateCredentialRequestOrNull()
        assertEquals(
            "mockUserId",
            createRequest?.userId,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCreateCredentialRequestOrNull should set user verification based on biometric prompt result`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
        }
        val mockBiometricPromptResult = mockk<BiometricPromptResult>(relaxed = true) {
            every { isSuccessful } returns false
        }
        val mockProviderCreateCredentialRequest =
            mockk<ProviderCreateCredentialRequest>(relaxed = true) {
                every { biometricPromptResult } returns mockBiometricPromptResult
            }
        every { ProviderCreateCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        } returns mockProviderCreateCredentialRequest

        // Verify false is returned when biometric prompt is unsuccessful
        var createRequest = intent.getCreateCredentialRequestOrNull()
        assertFalse(createRequest!!.isUserPreVerified)

        // Verify true is returned when biometric prompt is successful
        every { mockBiometricPromptResult.isSuccessful } returns true
        createRequest = intent.getCreateCredentialRequestOrNull()
        assert(createRequest!!.isUserPreVerified)

        // Verify true is returned when biometric prompt result is null and intent extra is true
        every { mockProviderCreateCredentialRequest.biometricPromptResult } returns null
        every {
            intent.getBooleanExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, false)
        } returns true
        createRequest = intent.getCreateCredentialRequestOrNull()
        assertTrue(createRequest!!.isUserPreVerified)

        // Verify false is returned when biometric prompt result is null and intent extra is false
        every {
            intent.getBooleanExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, false)
        } returns false
        createRequest = intent.getCreateCredentialRequestOrNull()
        assertFalse(createRequest!!.isUserPreVerified)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCreateCredentialRequestOrNull should return null when build version is below 34`() {
        val intent = mockk<Intent>()

        every { isBuildVersionAtLeast(34) } returns false

        assertNull(intent.getCreateCredentialRequestOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCreateCredentialRequestOrNull should return null when retrieveProviderCreateCredentialRequest is null`() {
        every { PendingIntentHandler.retrieveProviderCreateCredentialRequest(any()) } returns null
        assertNull(mockk<Intent>().getCreateCredentialRequestOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getCreateCredentialRequestOrNull should return null when user id is not present in extras`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns null
        }
        every {
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        } returns mockk()

        assertNull(intent.getCreateCredentialRequestOrNull())
    }

    @Test
    fun `getFido2AssertionRequestOrNull should return Fido2AssertionRequest when present`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
            every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns "mockCipherId"
            every { getStringExtra(EXTRA_KEY_CREDENTIAL_ID) } returns "mockCredentialId"
        }

        every { ProviderGetCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        } returns mockk(relaxed = true) {
            every { biometricPromptResult } returns mockk(relaxed = true) {
                every { isSuccessful } returns false
            }
        }

        val assertionRequest = intent.getFido2AssertionRequestOrNull()

        assertNotNull(assertionRequest)
        assertEquals("mockUserId", assertionRequest.userId)
        assertEquals("mockCipherId", assertionRequest.cipherId)
        assertEquals("mockCredentialId", assertionRequest.credentialId)
        assertEquals(false, assertionRequest.isUserPreVerified)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2AssertionRequestOrNull should set user verification based on biometric prompt result`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
            every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns "mockCipherId"
            every { getStringExtra(EXTRA_KEY_CREDENTIAL_ID) } returns "mockCredentialId"
            every {
                getBooleanExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, false)
            } returns false
        }
        val mockBiometricPromptResult = mockk<BiometricPromptResult>(relaxed = true) {
            every { isSuccessful } returns false
        }
        val mockGetCredentialRequest =
            mockk<ProviderGetCredentialRequest>(relaxed = true) {
                every { biometricPromptResult } returns mockBiometricPromptResult
            }
        every { ProviderCreateCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        } returns mockGetCredentialRequest

        // Verify false is returned when biometric prompt is unsuccessful
        var assertionRequest = intent.getFido2AssertionRequestOrNull()
        assertFalse(assertionRequest!!.isUserPreVerified)

        // Verify true is returned when biometric prompt is successful
        every { mockBiometricPromptResult.isSuccessful } returns true
        assertionRequest = intent.getFido2AssertionRequestOrNull()
        assert(assertionRequest!!.isUserPreVerified)

        // Verify false is returned when biometric prompt result is null
        every { mockGetCredentialRequest.biometricPromptResult } returns null
        assertionRequest = intent.getFido2AssertionRequestOrNull()
        assertFalse(assertionRequest!!.isUserPreVerified)
    }

    @Test
    fun `getFido2AssertionRequestOrNull should return null when build version is below 34`() {
        val intent = mockk<Intent>()

        every { isBuildVersionAtLeast(34) } returns false

        val assertionRequest = intent.getFido2AssertionRequestOrNull()

        assertNull(assertionRequest)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2AssertionRequestOrNull should return null when retrieveProviderGetCredentialRequest is null`() {
        val intent = mockk<Intent>()

        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(any())
        } returns null

        val assertionRequest = intent.getFido2AssertionRequestOrNull()

        assertNull(assertionRequest)
    }

    @Test
    fun `getFido2AssertionRequestOrNull should return null when extras are not correctly set`() {
        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(any())
        } returns mockk()

        // Verify credential ID is required
        assertNull(
            mockk<Intent> {
                every { getStringExtra(EXTRA_KEY_CREDENTIAL_ID) } returns null
                every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns "mockCipherId"
                every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
            }
                .getFido2AssertionRequestOrNull(),
        )

        // Verify cipher ID is required
        assertNull(
            mockk<Intent> {
                every { getStringExtra(EXTRA_KEY_CREDENTIAL_ID) } returns "mockCredentialId"
                every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns null
                every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
            }
                .getFido2AssertionRequestOrNull(),
        )

        // Verify user ID is required
        assertNull(
            mockk<Intent> {
                every { getStringExtra(EXTRA_KEY_CREDENTIAL_ID) } returns "mockCredentialId"
                every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns "mockCipherId"
                every { getStringExtra(EXTRA_KEY_USER_ID) } returns null
            }
                .getFido2AssertionRequestOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getProviderGetPasswordRequestOrNull should return ProviderGetPasswordCredentialRequest when present`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
            every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns "mockCipherId"
        }

        every { ProviderGetCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        } returns mockk(relaxed = true) {
            every { biometricPromptResult } returns mockk(relaxed = true) {
                every { isSuccessful } returns false
            }
        }

        val assertionRequest = intent.getProviderGetPasswordRequestOrNull()

        assertNotNull(assertionRequest)
        assertEquals("mockUserId", assertionRequest.userId)
        assertEquals("mockCipherId", assertionRequest.cipherId)
        assertEquals(false, assertionRequest.isUserPreVerified)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getProviderGetPasswordRequestOrNull should set user verification based on biometric prompt result`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
            every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns "mockCipherId"
            every {
                getBooleanExtra(EXTRA_KEY_UV_PERFORMED_DURING_UNLOCK, false)
            } returns false
        }
        val mockBiometricPromptResult = mockk<BiometricPromptResult>(relaxed = true) {
            every { isSuccessful } returns false
        }
        val mockGetCredentialRequest =
            mockk<ProviderGetCredentialRequest>(relaxed = true) {
                every { biometricPromptResult } returns mockBiometricPromptResult
            }
        every { ProviderCreateCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(intent)
        } returns mockGetCredentialRequest

        // Verify false is returned when biometric prompt is unsuccessful
        var assertionRequest = intent.getProviderGetPasswordRequestOrNull()
        assertFalse(assertionRequest!!.isUserPreVerified)

        // Verify true is returned when biometric prompt is successful
        every { mockBiometricPromptResult.isSuccessful } returns true
        assertionRequest = intent.getProviderGetPasswordRequestOrNull()
        assert(assertionRequest!!.isUserPreVerified)

        // Verify false is returned when biometric prompt result is null
        every { mockGetCredentialRequest.biometricPromptResult } returns null
        assertionRequest = intent.getProviderGetPasswordRequestOrNull()
        assertFalse(assertionRequest!!.isUserPreVerified)
    }

    @Test
    fun `getProviderGetPasswordRequestOrNull should return null when build version is below 34`() {
        val intent = mockk<Intent>()

        every { isBuildVersionAtLeast(34) } returns false

        val assertionRequest = intent.getProviderGetPasswordRequestOrNull()

        assertNull(assertionRequest)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getProviderGetPasswordRequestOrNull should return null when retrieveProviderGetCredentialRequest is null`() {
        val intent = mockk<Intent>()

        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(any())
        } returns null

        val assertionRequest = intent.getProviderGetPasswordRequestOrNull()

        assertNull(assertionRequest)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getProviderGetPasswordRequestOrNull should return null when extras are not correctly set`() {
        every {
            PendingIntentHandler.retrieveProviderGetCredentialRequest(any())
        } returns mockk()

        // Verify cipher ID is required
        assertNull(
            mockk<Intent> {
                every { getStringExtra(EXTRA_KEY_CREDENTIAL_ID) } returns "mockCredentialId"
                every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns null
                every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
            }
                .getProviderGetPasswordRequestOrNull(),
        )

        // Verify user ID is required
        assertNull(
            mockk<Intent> {
                every { getStringExtra(EXTRA_KEY_CREDENTIAL_ID) } returns "mockCredentialId"
                every { getStringExtra(EXTRA_KEY_CIPHER_ID) } returns "mockCipherId"
                every { getStringExtra(EXTRA_KEY_USER_ID) } returns null
            }
                .getProviderGetPasswordRequestOrNull(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getGetCredentialsRequestOrNull should return GetCredentialRequest when present`() {
        val intent = mockk<Intent> {
            every { getStringExtra("user_id") } returns "mockUserId"
        }

        every { BeginGetCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
        } returns mockk()

        assertNotNull(intent.getGetCredentialsRequestOrNull())
    }

    @Test
    fun `getGetCredentialsRequestOrNull should return null when build version is below 34`() {
        val intent = mockk<Intent>()
        every { isBuildVersionAtLeast(34) } returns false
        val result = intent.getGetCredentialsRequestOrNull()
        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getGetCredentialsRequestOrNull should return null when retrieveBeginGetCredentialRequest is null`() {
        val intent = mockk<Intent>()
        every { PendingIntentHandler.retrieveBeginGetCredentialRequest(intent) } returns null
        val result = intent.getGetCredentialsRequestOrNull()
        assertNull(result)
    }

    @Test
    fun `getGetCredentialRequestOrNull should return null when user id is not in extras`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns null
        }
        every { PendingIntentHandler.retrieveBeginGetCredentialRequest(intent) } returns mockk()
        val result = intent.getGetCredentialsRequestOrNull()
        assertNull(result)
    }
}
