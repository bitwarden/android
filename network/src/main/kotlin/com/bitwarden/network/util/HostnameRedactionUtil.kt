package com.bitwarden.network.util

/**
 * List of official Bitwarden cloud hostnames that are safe to log.
 */
private val BITWARDEN_HOSTS = listOf("bitwarden.com", "bitwarden.eu", "bitwarden.pw")

/**
 * Redacts hostnames in a log message by replacing bare hostnames with [REDACTED_SELF_HOST].
 *
 * Only redacts hostnames that match [configuredHosts] AND are not official Bitwarden domains.
 * Preserves all Bitwarden domains (including QA/dev environments).
 *
 * @param configuredHosts Set of hostnames to redact
 * @return Message with hostnames redacted as [REDACTED_SELF_HOST]
 */
fun String.redactHostnamesInMessage(configuredHosts: Set<String>): String =
    configuredHosts.fold(this) { result, hostname ->
        val escapedHostname = Regex.escape(hostname)
        val bareHostnamePattern = Regex("""\b$escapedHostname\b""")
        bareHostnamePattern.replace(result) { hostname.redactIfSelfHosted() }
    }

private fun String.redactIfSelfHosted(): String {
    val isBitwardenHost = BITWARDEN_HOSTS.any { this.endsWith(it) }
    return if (isBitwardenHost) this else "[REDACTED_SELF_HOST]"
}
