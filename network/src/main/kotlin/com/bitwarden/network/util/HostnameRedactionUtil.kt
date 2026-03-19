package com.bitwarden.network.util

internal val UNKNOWN_HOST_REGEX = Regex("""Unable to resolve host "([^"]+)"""")
/**
 * List of official Bitwarden cloud hostnames that are safe to log.
 */
private val BITWARDEN_HOSTS = listOf("bitwarden.com", "bitwarden.eu", "bitwarden.pw")

private val URL_HOST_REGEX = Regex("""https?://([^/?#\s"]+)""")

// Matches hostnames as single-argument method calls, e.g. getCookies(vault.example.com)
private val METHOD_CALL_HOST_REGEX =
    Regex("""\b\w+\(([a-zA-Z0-9][a-zA-Z0-9.\-]*\.[a-zA-Z]{2,})\)""")

/**
 * Extracts hostnames from URLs, UnknownHostException messages, and method-call log patterns
 * present in this string, then redacts any self-hosted (non-Bitwarden) hostnames with
 * [REDACTED_SELF_HOST].
 *
 * Recognized patterns:
 * - Full URLs: `https://hostname/path`
 * - UnknownHostException: `Unable to resolve host "hostname"`
 * - Method-call logs: `methodName(hostname)`
 */
internal fun String.redactSelfHostedHostnames(): String {
    val urlHosts = URL_HOST_REGEX.findAll(this).map { it.groupValues[1] }
    val exceptionHosts = UNKNOWN_HOST_REGEX.findAll(this).map { it.groupValues[1] }
    val methodCallHosts = METHOD_CALL_HOST_REGEX.findAll(this).map { it.groupValues[1] }
    val extractedHosts = (urlHosts + exceptionHosts + methodCallHosts)
        .map { it.substringBefore(':') } // strip port if present
        .filter { host -> BITWARDEN_HOSTS.none { host.endsWith(it) } }
        .toSet()
    return this.redactHostnamesInMessage(extractedHosts)
}

/**
 * Redacts hostnames in a log message by replacing bare hostnames with [REDACTED_SELF_HOST].
 *
 * Only redacts hostnames that match [configuredHosts] AND are not official Bitwarden domains.
 * Preserves all Bitwarden domains (including QA/dev environments).
 *
 * @param configuredHosts Set of hostnames to redact
 * @return Message with hostnames redacted as [REDACTED_SELF_HOST]
 */
internal fun String.redactHostnamesInMessage(configuredHosts: Set<String>): String =
    configuredHosts.fold(this) { result, hostname ->
        val escapedHostname = Regex.escape(hostname)
        val bareHostnamePattern = Regex("""\b$escapedHostname\b""")
        bareHostnamePattern.replace(result) { hostname.redactIfSelfHosted() }
    }

private fun String.redactIfSelfHosted(): String {
    val isBitwardenHost = BITWARDEN_HOSTS.any { this.endsWith(it) }
    return if (isBitwardenHost) this else "[REDACTED_SELF_HOST]"
}
