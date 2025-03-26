package com.x8bit.bitwarden.data.autofill.fido2.util

import android.content.Intent
import androidx.core.os.bundleOf
import androidx.credentials.provider.BeginGetCredentialRequest
import androidx.credentials.provider.PendingIntentHandler
import androidx.credentials.provider.ProviderCreateCredentialRequest
import androidx.credentials.provider.ProviderGetCredentialRequest
import com.x8bit.bitwarden.data.platform.util.isBuildVersionBelow
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CIPHER_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_CREDENTIAL_ID
import com.x8bit.bitwarden.ui.platform.manager.intent.EXTRA_KEY_USER_ID
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class Fido2IntentUtilsTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(::isBuildVersionBelow)
        mockkObject(
            PendingIntentHandler.Companion,
            BeginGetCredentialRequest.Companion,
            ProviderCreateCredentialRequest.Companion,
            ProviderGetCredentialRequest.Companion,
        )
        every { isBuildVersionBelow(any()) } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionBelow)
        unmockkObject(
            BeginGetCredentialRequest.Companion,
            PendingIntentHandler.Companion,
            ProviderCreateCredentialRequest.Companion,
            ProviderGetCredentialRequest.Companion,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2CreateCredentialRequestOrNull should return Fido2CreateCredentialRequest when present`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns "mockUserId"
        }

        every { ProviderCreateCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        } returns mockk()

        val createRequest = intent.getFido2CreateCredentialRequestOrNull()
        assertEquals(
            "mockUserId",
            createRequest?.userId,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2CreateCredentialRequestOrNull should return null when build version is below 34`() {
        val intent = mockk<Intent>()

        every { isBuildVersionBelow(34) } returns true

        assertNull(intent.getFido2CreateCredentialRequestOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2CreateCredentialRequestOrNull should return null when retrieveProviderCreateCredentialRequest is null`() {
        every { PendingIntentHandler.retrieveProviderCreateCredentialRequest(any()) } returns null
        assertNull(mockk<Intent>().getFido2CreateCredentialRequestOrNull())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2CreateCredentialRequestOrNull should return null when user id is not present in extras`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns null
        }
        every {
            PendingIntentHandler.retrieveProviderCreateCredentialRequest(intent)
        } returns mockk()

        assertNull(intent.getFido2CreateCredentialRequestOrNull())
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
        } returns mockk()

        val assertionRequest = intent.getFido2AssertionRequestOrNull()

        assertNotNull(assertionRequest)
        assertEquals("mockUserId", assertionRequest?.userId)
        assertEquals("mockCipherId", assertionRequest?.cipherId)
        assertEquals("mockCredentialId", assertionRequest?.credentialId)
    }

    @Test
    fun `getFido2AssertionRequestOrNull should return null when build version is below 34`() {
        val intent = mockk<Intent>()

        every { isBuildVersionBelow(34) } returns true

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
    fun `getFido2GetCredentialsRequestOrNull should return Fido2GetCredentialRequest when present`() {
        val intent = mockk<Intent> {
            every { getStringExtra("user_id") } returns "mockUserId"
        }

        every { BeginGetCredentialRequest.asBundle(any()) } returns bundleOf()
        every {
            PendingIntentHandler.retrieveBeginGetCredentialRequest(intent)
        } returns mockk()

        assertNotNull(intent.getFido2GetCredentialsRequestOrNull())
    }

    @Test
    fun `getGido2GetCredentialsRequestOrNull should return null when build version is below 34`() {
        val intent = mockk<Intent>()
        every { isBuildVersionBelow(34) } returns true
        val result = intent.getFido2GetCredentialsRequestOrNull()
        assertNull(result)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getFido2GetCredentialsRequestOrNull should return null when retrieveBeginGetCredentialRequest is null`() {
        val intent = mockk<Intent>()
        every { PendingIntentHandler.retrieveBeginGetCredentialRequest(intent) } returns null
        val result = intent.getFido2GetCredentialsRequestOrNull()
        assertNull(result)
    }

    @Test
    fun `getFido2GetCredentialRequestOrNull should return null when user id is not in extras`() {
        val intent = mockk<Intent> {
            every { getStringExtra(EXTRA_KEY_USER_ID) } returns null
        }
        every { PendingIntentHandler.retrieveBeginGetCredentialRequest(intent) } returns mockk()
        val result = intent.getFido2GetCredentialsRequestOrNull()
        assertNull(result)
    }
}
