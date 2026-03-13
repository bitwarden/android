package com.bitwarden.network.util

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class HostnameRedactionUtilTest {
    @Test
    fun `redactHostnamesInMessage redacts configured self-hosted URLs`() {
        val message = "--> GET https://vault.example.com/api/sync HTTP/1.1"
        val configuredHosts = setOf("vault.example.com")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals("--> GET https://[REDACTED_SELF_HOST]/api/sync HTTP/1.1", result)
    }

    @Test
    fun `redactHostnamesInMessage preserves non-configured URLs`() {
        val message = "--> GET https://vault.example.com/api/sync HTTP/1.1"
        val configuredHosts = setOf("api.bitwarden.com") // Different host

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(message, result) // Unchanged - not in configured hosts
    }

    @Test
    fun `redactHostnamesInMessage preserves Bitwarden URLs even if configured`() {
        val message = "--> GET https://vault.qa.bitwarden.pw/api/sync HTTP/1.1"
        val configuredHosts = setOf("vault.qa.bitwarden.pw")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(message, result) // Unchanged - Bitwarden domain preserved
    }

    @Test
    fun `redactHostnamesInMessage redacts quoted hostnames in error messages`() {
        val message = """Unable to resolve host "vault.example.com": No address"""
        val configuredHosts = setOf("vault.example.com")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals("""Unable to resolve host "[REDACTED_SELF_HOST]": No address""", result)
    }

    @Test
    fun `redactHostnamesInMessage handles multiple URLs in one message`() {
        val message = "Redirect from https://old.corp.com to https://new.corp.com"
        val configuredHosts = setOf("old.corp.com", "new.corp.com")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(
            "Redirect from https://[REDACTED_SELF_HOST] to https://[REDACTED_SELF_HOST]",
            result,
        )
    }

    @Test
    fun `redactHostnamesInMessage handles empty configured hosts`() {
        val message = "--> GET https://vault.example.com/api HTTP/1.1"
        val configuredHosts = emptySet<String>()

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(message, result) // Unchanged - no hosts to redact
    }

    @Test
    fun `redactHostnamesInMessage handles NetworkCookieManagerImpl getCookies pattern`() {
        val message = "2026-03-09 12:43:29:857 – DEBUG – NetworkCookieManagerImpl – " +
            "getCookies(vault.example.com): resolved=vault.example.com, count=0"
        val configuredHosts = setOf("vault.example.com")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(
            "2026-03-09 12:43:29:857 – DEBUG – NetworkCookieManagerImpl – " +
                "getCookies([REDACTED_SELF_HOST]): resolved=[REDACTED_SELF_HOST], count=0",
            result,
        )
    }

    @Test
    fun `redactHostnamesInMessage preserves Bitwarden domains in NetworkCookieManagerImpl logs`() {
        val message = "2026-03-09 12:43:29:857 – DEBUG – NetworkCookieManagerImpl – " +
            "getCookies(vault.example.com): resolved=vault.qa.bitwarden.pw, count=0"
        val configuredHosts = setOf("vault.example.com", "vault.qa.bitwarden.pw")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(
            "2026-03-09 12:43:29:857 – DEBUG – NetworkCookieManagerImpl – " +
                "getCookies([REDACTED_SELF_HOST]): resolved=vault.qa.bitwarden.pw, count=0",
            result,
        )
    }

    @Test
    fun `redactHostnamesInMessage handles UnknownHostException error message`() {
        val message = "DEBUG – BitwardenNetworkClient – <-- HTTP FAILED: " +
            "java.net.UnknownHostException: Unable to resolve host " +
            "\"vault.example.com\": No address associated with hostname."
        val configuredHosts = setOf("vault.example.com")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(
            "DEBUG – BitwardenNetworkClient – <-- HTTP FAILED: " +
                "java.net.UnknownHostException: Unable to resolve host " +
                "\"[REDACTED_SELF_HOST]\": No address associated with hostname.",
            result,
        )
    }

    @Test
    fun `redactHostnamesInMessage handles needsBootstrap pattern`() {
        val message = "2026-03-09 12:43:29:851 – DEBUG – NetworkCookieManagerImpl – " +
            "needsBootstrap(vault.example.com): false (cookieDomain=null)"
        val configuredHosts = setOf("vault.example.com")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(
            "2026-03-09 12:43:29:851 – DEBUG – NetworkCookieManagerImpl – " +
                "needsBootstrap([REDACTED_SELF_HOST]): false (cookieDomain=null)",
            result,
        )
    }

    @Test
    fun `redactHostnamesInMessage handles resolveHostname pattern`() {
        val message = "2026-03-09 12:43:29:855 – DEBUG – NetworkCookieManagerImpl – " +
            "resolveHostname(vault.example.com): no stored config found, using original"
        val configuredHosts = setOf("vault.example.com")

        val result = message.redactHostnamesInMessage(configuredHosts)

        assertEquals(
            "2026-03-09 12:43:29:855 – DEBUG – NetworkCookieManagerImpl – " +
                "resolveHostname([REDACTED_SELF_HOST]): no stored config found, using original",
            result,
        )
    }
}
