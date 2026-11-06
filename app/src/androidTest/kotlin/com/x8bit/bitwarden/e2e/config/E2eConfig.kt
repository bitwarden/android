package com.x8bit.bitwarden.e2e.config

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry

/**
 * Environment settings for end-to-end tests, read from instrumentation arguments
 * (`-e BW_TEST_SERVER_URL <url>`). Nothing is hard-coded, and [toString] never exposes the token.
 *
 * @property serverUrl Base URL of the self-hosted server under test.
 * @property seederUrl Base URL of the SeederApi that writes test data to that server's database.
 * @property seederToken SeederApi Basic credential (base64 of `username:password`).
 */
class E2eConfig(
    val serverUrl: String,
    val seederUrl: String,
    val seederToken: String,
) {
    override fun toString(): String = "E2eConfig(serverUrl=$serverUrl, seederUrl=$seederUrl)"

    companion object {
        /**
         * Reads the configuration from instrumentation arguments, failing fast when one is missing
         * or unsafe.
         */
        fun fromInstrumentationArguments(): E2eConfig {
            val arguments = InstrumentationRegistry.getArguments()
            fun required(key: String): String =
                requireNotNull(arguments.getString(key)?.takeIf { it.isNotBlank() }) {
                    "Missing instrumentation argument '$key'."
                }
            return E2eConfig(
                serverUrl = requireTestServer(required("BW_TEST_SERVER_URL")),
                seederUrl = requireHttps(required("BW_SEEDER_URL")).trimEnd('/'),
                seederToken = required("BW_SEEDER_TOKEN"),
            )
        }

        private fun requireHttps(url: String): String {
            require(url.startsWith("https://")) { "Expected an https URL, got '$url'." }
            return url
        }

        // These tests seed and log in throwaway accounts; never point them at production.
        private fun requireTestServer(url: String): String {
            val host = Uri.parse(requireHttps(url)).host.orEmpty().lowercase().trimEnd('.')
            require(PRODUCTION_DOMAINS.none { host == it || host.endsWith(".$it") }) {
                "Refusing to run end-to-end tests against production host '$host'."
            }
            return url
        }
    }
}

private val PRODUCTION_DOMAINS = listOf("bitwarden.com", "bitwarden.eu")
