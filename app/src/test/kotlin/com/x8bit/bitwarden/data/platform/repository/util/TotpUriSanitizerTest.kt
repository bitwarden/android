package com.x8bit.bitwarden.data.platform.repository.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TotpUriSanitizerTest {

    @Test
    fun `valid TOTP URI should remain unchanged`() {
        val validUri = "otpauth://totp/Company:test@test.com?secret=abcdef12345&issuer=Company"
        val result = validUri.sanitizeTotpUri("Company", "test@test.com")

        assertEquals(validUri, result)
    }

    @Test
    fun `manually entered TOTP with both issuer and account should be converted correctly`() {
        val rawTotp = "zdd3 jqnn"
        val expectedUri = "otpauth://totp/Company%3Atest%40test.com?secret=zdd3jqnn&issuer=Company"
        val result = rawTotp.sanitizeTotpUri("Company", "test@test.com")

        assertEquals(expectedUri, result)
    }

    @Test
    fun `only issuer provided should not be included in label but should be in query`() {
        val rawTotp = "secret123"
        val expectedUri = "otpauth://totp/?secret=secret123&issuer=Company"
        val result = rawTotp.sanitizeTotpUri("Company", null)

        assertEquals(expectedUri, result)
    }

    @Test
    fun `only account name provided should be included in label`() {
        val rawTotp = "secret456"
        val expectedUri = "otpauth://totp/test%40email.com?secret=secret456"
        val result = rawTotp.sanitizeTotpUri(null, "test@email.com")

        assertEquals(expectedUri, result)
    }

    @Test
    fun `both issuer and account missing should generate minimal valid URI`() {
        val rawTotp = "secret789"
        val expectedUri = "otpauth://totp/?secret=secret789"
        val result = rawTotp.sanitizeTotpUri(null, null)

        assertEquals(expectedUri, result)
    }

    @Test
    fun `extra spaces in TOTP secret should be removed`() {
        val rawTotp = "a b c d e f 1 2 3"
        val expectedUri = "otpauth://totp/Issuer%3Auser%40domain.com?secret=abcdef123&issuer=Issuer"
        val result = rawTotp.sanitizeTotpUri("Issuer", "user@domain.com")

        assertEquals(expectedUri, result)
    }

    @Test
    fun `null TOTP input should return null`() {
        val result = null.sanitizeTotpUri("Company", "test@test.com")
        assertNull(result)
    }

    @Test
    fun `empty TOTP should return null`() {
        val result = "".sanitizeTotpUri("Company", "test@test.com")
        assertNull(result)
    }

    @Test
    fun `invalid characters in issuer and account should be properly encoded`() {
        val rawTotp = "secure"
        val expected = "otpauth://totp/My%20App%3Auser%40example.com?secret=secure&issuer=My%20App"
        val result = rawTotp.sanitizeTotpUri("My App", "user@example.com")

        assertEquals(expected, result)
    }

    @Test
    fun `issuer with special characters should be encoded correctly`() {
        val rawTotp = "tokenvalue"
        val expected = "otpauth://totp/?secret=tokenvalue&issuer=Super%26Secure"
        val result = rawTotp.sanitizeTotpUri("Super&Secure", null)

        assertEquals(expected, result)
    }

    @Test
    fun `account name with special characters should be encoded correctly`() {
        val rawTotp = "secret999"
        val expected = "otpauth://totp/user%2Bname%40email.com?secret=secret999"
        val result = rawTotp.sanitizeTotpUri(null, "user+name@email.com")

        assertEquals(expected, result)
    }

    @Test
    fun `both issuer and account name empty should generate minimal valid URI`() {
        val rawTotp = "secretminimal"
        val expected = "otpauth://totp/?secret=secretminimal"
        val result = rawTotp.sanitizeTotpUri("", "")

        assertEquals(expected, result)
    }

    @Test
    fun `both issuer and account name are null should generate minimal valid URI`() {
        val rawTotp = "secretminimal"
        val expected = "otpauth://totp/?secret=secretminimal"
        val result = rawTotp.sanitizeTotpUri(null, null)

        assertEquals(expected, result)
    }

    @Test
    fun `account name with spaces should be properly encoded`() {
        val rawTotp = "secret1234"
        val expectedUri = "otpauth://totp/John%20Doe%40email.com?secret=secret1234"
        val result = rawTotp.sanitizeTotpUri(null, "John Doe@email.com")

        assertEquals(expectedUri, result)
    }

    @Test
    fun `TOTP secret with leading and trailing spaces should be sanitized`() {
        val rawTotp = "  a1b2c3d4  "
        val expected = "otpauth://totp/Company%3Atest%40test.com?secret=a1b2c3d4&issuer=Company"
        val result = rawTotp.sanitizeTotpUri("Company", "test@test.com")

        assertEquals(expected, result)
    }

    @Test
    fun `issuer and account name with trailing spaces should be trimmed before encoding`() {
        val rawTotp = "secure"
        val expected = "otpauth://totp/Company%3Auser%40secure.com?secret=secure&issuer=Company"
        val result = rawTotp.sanitizeTotpUri("  Company  ", "  user@secure.com  ")

        assertEquals(expected, result)
    }

    @Test
    fun `valid Steam URI should remain unchanged`() {
        val validSteamUri = "steam://abcdef12345"
        val result = validSteamUri.sanitizeTotpUri("Steam", "test@test.com")

        assertEquals(validSteamUri, result)
    }
}
