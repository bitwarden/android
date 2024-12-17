package com.x8bit.bitwarden.data.platform.datasource.network.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.UnknownHostException
import java.security.cert.CertPathValidatorException
import javax.net.ssl.SSLHandshakeException

class NetworkUtilsTest {
    @Test
    fun `base64UrlEncode should Base64 encode the string and make the relevant replacements`() {
        // Checks replacement of + to - and removal of =
        assertEquals(
            "dis-ZA",
            "v+>d".base64UrlEncode(),
        )
        // Checks replacement of \ to _
        assertEquals(
            "NmI_ImE4",
            "6b?\"a8".base64UrlEncode(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `base64UrlDecodeOrNull should Base64 decode the string and make the relevant replacements`() {
        // Checks replacement of - to +
        assertEquals(
            "v+>d",
            "dis-ZA==".base64UrlDecodeOrNull(),
        )
        // Checks replacement of _ to \
        assertEquals(
            "6b?\"a8",
            "NmI_ImE4".base64UrlDecodeOrNull(),
        )
    }

    @Test
    fun `base64UrlDecodeOrNull should return null value a non-encoded String`() {
        assertNull(
            "*.*".base64UrlDecodeOrNull(),
        )
    }

    @Test
    fun `isNoConnectionError should return return true for UnknownHostException`() {
        assertEquals(
            true,
            UnknownHostException().isNoConnectionError(),
        )
    }

    @Test
    fun `isNoConnectionError should return return false for not UnknownHostException`() {
        assertEquals(
            false,
            IllegalStateException().isNoConnectionError(),
        )
    }

    @Test
    fun `isSslHandshakeError should return return true for SSLHandshakeException`() {
        assertEquals(
            true,
            SSLHandshakeException("whoops").isSslHandShakeError(),
        )
    }

    @Test
    fun `isSslHandshakeError should return return true for CertPathValidatorException`() {
        assertEquals(
            true,
            CertPathValidatorException("whoops").isSslHandShakeError(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isSslHandshakeError should return return true if exceptions cause is SSLHandshakeException`() {
        assertEquals(
            true,
            Exception(SSLHandshakeException("whoops")).isSslHandShakeError(),
        )
    }

    @Test
    fun `isSslHandshakeError should return return false for not IllegalStateException`() {
        assertEquals(
            false,
            IllegalStateException().isSslHandShakeError(),
        )
    }
}
