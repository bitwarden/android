package com.bitwarden.ui.platform.util

import android.net.Uri
import com.bitwarden.ui.platform.model.TotpData
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TotpUriUtilsTest {

    @Test
    fun `getTotpDataOrNull with incorrect scheme returns null`() {
        val uri = mockk<Uri> {
            every { scheme } returns "wrong"
        }

        assertNull(uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with incorrect host returns null`() {
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "hotp"
        }

        assertNull(uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull without secret returns null`() {
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "totp"
            every { getQueryParameter("secret") } returns null
        }

        assertNull(uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with invalid secret returns null`() {
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "totp"
            every { getQueryParameter("secret") } returns "1234567890qwertyuiop"
        }

        assertNull(uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with invalid digits returns null`() {
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "totp"
            every { getQueryParameter("secret") } returns "secret"
            every { getQueryParameter("digits") } returns "11"
        }

        assertNull(uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with invalid period returns null`() {
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "totp"
            every { getQueryParameter("secret") } returns "secret"
            every { getQueryParameter("digits") } returns "5"
            every { getQueryParameter("period") } returns "0"
        }

        assertNull(uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with invalid algorithm returns null`() {
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "totp"
            every { getQueryParameter("secret") } returns "secret"
            every { getQueryParameter("digits") } returns "5"
            every { getQueryParameter("period") } returns "10"
            every { getQueryParameter("algorithm") } returns "sha22"
        }

        assertNull(uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with minimum required values returns TotpData with defaults`() {
        val secret = "secret"
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "totp"
            every { pathSegments } returns emptyList()
            every { getQueryParameter("secret") } returns secret
            every { getQueryParameter("digits") } returns null
            every { getQueryParameter("issuer") } returns null
            every { getQueryParameter("period") } returns null
            every { getQueryParameter("algorithm") } returns null
        }

        val expectedResult = TotpData(
            uri = uri.toString(),
            issuer = null,
            accountName = null,
            secret = secret,
            digits = 6,
            period = 30,
            algorithm = TotpData.CryptoHashAlgorithm.SHA_1,
        )

        assertEquals(expectedResult, uri.getTotpDataOrNull())
    }

    @Test
    fun `getTotpDataOrNull with complete values returns custom TotpData`() {
        val secret = "secret"
        val digits = 8
        val issuer = "Bitwarden"
        val period = 25
        val algorithm = "sha256"
        val accountName = "test@bitwarden.com"
        val uri = mockk<Uri> {
            every { scheme } returns "otpauth"
            every { host } returns "totp"
            every { pathSegments } returns listOf("$issuer:$accountName")
            every { getQueryParameter("secret") } returns secret
            every { getQueryParameter("digits") } returns digits.toString()
            every { getQueryParameter("issuer") } returns issuer
            every { getQueryParameter("period") } returns period.toString()
            every { getQueryParameter("algorithm") } returns algorithm
        }
        val expectedResult = TotpData(
            uri = uri.toString(),
            issuer = issuer,
            accountName = accountName,
            secret = secret,
            digits = digits,
            period = period,
            algorithm = TotpData.CryptoHashAlgorithm.SHA_256,
        )

        assertEquals(expectedResult, uri.getTotpDataOrNull())
    }
}
