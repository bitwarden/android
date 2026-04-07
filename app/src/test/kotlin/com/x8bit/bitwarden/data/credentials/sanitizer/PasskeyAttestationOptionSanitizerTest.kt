package com.x8bit.bitwarden.data.credentials.sanitizer

import com.x8bit.bitwarden.data.credentials.model.PasskeyAttestationOptions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class PasskeyAttestationOptionSanitizerTest {

    private val sanitizer: PasskeyAttestationOptionsSanitizer =
        PasskeyAttestationOptionsSanitizerImpl

    @Test
    fun `Sanitization on matching RP ID and user ID with newline`() {
        val options = createOptions(rpId = "m.aliexpress.com", userId = "user123\n")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertEquals("user123", sanitizedOptions.user.id)
        assertNotSame(options, sanitizedOptions)
    }

    @Test
    fun `No sanitization on non matching RP ID`() {
        val options = createOptions(rpId = "some.other.rp", userId = "user123")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertSame(options, sanitizedOptions)
    }

    @Test
    fun `No sanitization when user ID does not end with newline`() {
        val options = createOptions(rpId = "m.aliexpress.com", userId = "user123")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertSame(options, sanitizedOptions)
    }

    @Test
    fun `No sanitization on matching RP ID and newline in middle of user ID`() {
        val options = createOptions(rpId = "m.aliexpress.com", userId = "user123")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertSame(options, sanitizedOptions)
    }

    @Test
    fun `Sanitization with multiple trailing newlines`() {
        val options = createOptions(rpId = "m.aliexpress.com", userId = "user123\n")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertEquals("user123", sanitizedOptions.user.id)
        assertNotSame(options, sanitizedOptions)
    }

    @Test
    fun `Sanitization with preceding trailing spaces and a newline`() {
        val options = createOptions(rpId = "m.aliexpress.com", userId = "user123\n")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertEquals("user123", sanitizedOptions.user.id)
        assertNotSame(options, sanitizedOptions)
    }

    @Test
    fun `No sanitization with an empty user ID`() {
        val options = createOptions(rpId = "m.aliexpress.com", userId = "")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertSame(options, sanitizedOptions)
    }

    @Test
    fun `Sanitization with user ID containing only a newline`() {
        val options = createOptions(rpId = "m.aliexpress.com", userId = "\n")

        val sanitizedOptions = sanitizer.sanitize(options)

        assertEquals("", sanitizedOptions.user.id)
        assertNotSame(options, sanitizedOptions)
    }

    private fun createOptions(
        rpId: String,
        userId: String,
    ): PasskeyAttestationOptions {
        return PasskeyAttestationOptions(
            relyingParty = PasskeyAttestationOptions.PublicKeyCredentialRpEntity(
                id = rpId,
                name = "RP",
            ),
            user = PasskeyAttestationOptions.PublicKeyCredentialUserEntity(
                id = userId,
                name = "User",
                displayName = "User",
            ),
            challenge = "challenge",
            pubKeyCredParams = emptyList(),
            authenticatorSelection = PasskeyAttestationOptions.AuthenticatorSelectionCriteria(),
        )
    }
}
