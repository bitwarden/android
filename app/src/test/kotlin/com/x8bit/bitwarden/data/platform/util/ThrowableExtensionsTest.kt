package com.x8bit.bitwarden.data.platform.util

import android.os.Build
import android.security.KeyStoreException
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.network.exception.CookieRedirectException
import com.bitwarden.network.exception.LocalNetworkAccessException
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.crypto.IllegalBlockSizeException

class ThrowableExtensionsTest {

    @BeforeEach
    fun setup() {
        mockkStatic(::isBuildVersionAtLeast)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `userFriendlyMessage should return message for CookieRedirectException`() {
        val message = "Your request was interrupted because the app needed to " +
            "re-authenticate. Please try again."
        val exception = CookieRedirectException(
            hostname = "example.com",
            message = message,
        )
        assertEquals(
            message,
            exception.userFriendlyMessage,
        )
    }

    @Test
    fun `userFriendlyMessage should return message for LocalNetworkAccessException`() {
        val message = "Fail!"
        val exception = LocalNetworkAccessException(message = message)
        assertEquals(message, exception.userFriendlyMessage)
    }

    @Test
    fun `userFriendlyMessage should return null for IOException`() {
        val exception = IOException("io error")
        assertNull(exception.userFriendlyMessage)
    }

    @Test
    fun `userFriendlyMessage should return null for RuntimeException`() {
        val exception = RuntimeException("runtime error")
        assertNull(exception.userFriendlyMessage)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isKeystoreUserAuthenticationRequired should return true when the cause reports user authentication required on API 33`() {
        mockBuildVersion(sdkInt = Build.VERSION_CODES.TIRAMISU)
        val (error, _) = createUserAuthenticationRequiredError()

        assertTrue(error.isKeystoreUserAuthenticationRequired())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isKeystoreUserAuthenticationRequired should return true for a nested user authentication required cause`() {
        mockBuildVersion(sdkInt = Build.VERSION_CODES.TIRAMISU)
        val (error, _) = createUserAuthenticationRequiredError(additionalWrapperCount = 2)

        assertTrue(error.isKeystoreUserAuthenticationRequired())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isKeystoreUserAuthenticationRequired should return true when the throwable itself is the keystore exception`() {
        mockBuildVersion(sdkInt = Build.VERSION_CODES.BAKLAVA)
        val keyStoreException = mockKeyStoreException(
            numericErrorCode = KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED,
        )

        assertTrue(keyStoreException.isKeystoreUserAuthenticationRequired())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isKeystoreUserAuthenticationRequired should return false for a nonmatching numeric code`() {
        mockBuildVersion(sdkInt = Build.VERSION_CODES.TIRAMISU)
        val keyStoreException = mockKeyStoreException(
            numericErrorCode = KeyStoreException.ERROR_KEY_CORRUPTED,
        )
        val error = wrapKeyStoreException(keyStoreException = keyStoreException)

        assertFalse(error.isKeystoreUserAuthenticationRequired())
    }

    @Test
    fun `isKeystoreUserAuthenticationRequired should return false when the cause is missing`() {
        mockBuildVersion(sdkInt = Build.VERSION_CODES.TIRAMISU)

        assertFalse(IllegalBlockSizeException().isKeystoreUserAuthenticationRequired())
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isKeystoreUserAuthenticationRequired should return false below API 33 without querying the numeric code`() {
        mockBuildVersion(sdkInt = Build.VERSION_CODES.S_V2)
        val (error, keyStoreException) = createUserAuthenticationRequiredError()

        assertFalse(error.isKeystoreUserAuthenticationRequired())
        verify(exactly = 0) { keyStoreException.numericErrorCode }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isKeystoreUserAuthenticationRequired should return false and stop when the cause chain cycles`() {
        mockBuildVersion(sdkInt = Build.VERSION_CODES.TIRAMISU)
        val keyStoreException = mockKeyStoreException(
            numericErrorCode = KeyStoreException.ERROR_KEY_CORRUPTED,
        )
        val wrapper = IllegalBlockSizeException()
        every { keyStoreException.cause } returns wrapper
        wrapper.initCause(keyStoreException)

        assertFalse(wrapper.isKeystoreUserAuthenticationRequired())
    }

    private fun mockBuildVersion(sdkInt: Int) {
        every { isBuildVersionAtLeast(any()) } answers { sdkInt >= firstArg<Int>() }
    }

    private fun mockKeyStoreException(numericErrorCode: Int): KeyStoreException {
        val keyStoreException = mockk<KeyStoreException>()
        every { keyStoreException.numericErrorCode } returns numericErrorCode
        every { keyStoreException.cause } returns null
        return keyStoreException
    }

    private fun wrapKeyStoreException(
        keyStoreException: KeyStoreException,
        additionalWrapperCount: Int = 0,
    ): IllegalBlockSizeException {
        var cause: Throwable = keyStoreException
        repeat(additionalWrapperCount) { index ->
            cause = RuntimeException("wrapper-$index", cause)
        }
        return IllegalBlockSizeException().apply { initCause(cause) }
    }

    private fun createUserAuthenticationRequiredError(
        additionalWrapperCount: Int = 0,
    ): Pair<IllegalBlockSizeException, KeyStoreException> {
        val keyStoreException = mockKeyStoreException(
            numericErrorCode = KeyStoreException.ERROR_USER_AUTHENTICATION_REQUIRED,
        )
        return wrapKeyStoreException(
            keyStoreException = keyStoreException,
            additionalWrapperCount = additionalWrapperCount,
        ) to keyStoreException
    }
}
