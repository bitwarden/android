package com.bitwarden.cxf.parser

import androidx.credentials.providerevents.exception.ImportCredentialsInvalidJsonException
import com.bitwarden.core.data.util.decodeFromStringOrNull
import com.bitwarden.cxf.model.CredentialExchangeExportResponse
import com.bitwarden.cxf.model.CredentialExchangePayload
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class CredentialExchangePayloadParserTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val parser = CredentialExchangePayloadParserImpl(json = json)

    @Nested
    inner class PayloadParsing {
        @Test
        fun `parse should return Importable when payload is valid with accounts`() {
            val result = parser.parse(VALID_PAYLOAD)

            assertTrue(result is CredentialExchangePayload.Importable)
            val importable = result as CredentialExchangePayload.Importable
            assertTrue(importable.accountsJson.isNotEmpty())
        }

        @Test
        fun `parse should return NoItems when payload has empty accounts`() {
            val result = parser.parse(VALID_PAYLOAD_EMPTY_ACCOUNTS)

            assertEquals(CredentialExchangePayload.NoItems, result)
        }

        @Test
        fun `parse should return Error when payload has unsupported major version`() {
            val result = parser.parse(PAYLOAD_UNSUPPORTED_MAJOR_VERSION)

            assertTrue(result is CredentialExchangePayload.Error)
            val error = (result as CredentialExchangePayload.Error).throwable
            assertTrue(error is ImportCredentialsInvalidJsonException)
            assertTrue(error.message?.contains("Unsupported CXF version") == true)
        }

        @Test
        fun `parse should return Error when payload has unsupported minor version`() {
            val result = parser.parse(PAYLOAD_UNSUPPORTED_MINOR_VERSION)

            assertTrue(result is CredentialExchangePayload.Error)
            val error = (result as CredentialExchangePayload.Error).throwable
            assertTrue(error is ImportCredentialsInvalidJsonException)
            assertTrue(error.message?.contains("Unsupported CXF version") == true)
        }
    }

    @Nested
    inner class InvalidPayloadParsing {
        @Test
        fun `parse should return Error when payload is completely invalid JSON`() {
            val result = parser.parse("not valid json")

            assertTrue(result is CredentialExchangePayload.Error)
            val error = (result as CredentialExchangePayload.Error).throwable
            assertTrue(error is ImportCredentialsInvalidJsonException)
            assertTrue(error.message?.contains("Invalid Credential Exchange JSON") == true)
        }

        @Test
        fun `parse should return Error when payload is empty string`() {
            val result = parser.parse("")

            assertTrue(result is CredentialExchangePayload.Error)
            val error = (result as CredentialExchangePayload.Error).throwable
            assertTrue(error is ImportCredentialsInvalidJsonException)
        }

        @Test
        fun `parse should return Error when payload is valid JSON but wrong structure`() {
            val result = parser.parse("""{"foo": "bar"}""")

            assertTrue(result is CredentialExchangePayload.Error)
            val error = (result as CredentialExchangePayload.Error).throwable
            assertTrue(error is ImportCredentialsInvalidJsonException)
        }
    }

    @Nested
    inner class SerializationErrorHandling {
        @Suppress("MaxLineLength")
        @Test
        fun `parse should return Error when account serialization fails`() {
            val mockJson = mockk<Json> {
                every {
                    decodeFromStringOrNull<CredentialExchangeExportResponse>(VALID_PAYLOAD)
                } returns MOCK_EXPORT_RESPONSE
                every {
                    encodeToString<CredentialExchangeExportResponse.Account?>(any(), any())
                } throws SerializationException("Mock serialization failure")
            }
            val parserWithMockJson = CredentialExchangePayloadParserImpl(json = mockJson)

            val result = parserWithMockJson.parse(VALID_PAYLOAD)

            assertTrue(result is CredentialExchangePayload.Error)
            val error = (result as CredentialExchangePayload.Error).throwable
            assertTrue(error is ImportCredentialsInvalidJsonException)
            assertTrue(error.message?.contains("Unable to serialize accounts") == true)
        }
    }
}

/**
 * Valid CXF payload (direct format) with version 1.0.
 */
private val VALID_PAYLOAD = """
{
  "version": {"major": 1, "minor": 0},
  "exporterRpId": "com.example.exporter",
  "exporterDisplayName": "Example Exporter",
  "timestamp": 1704067200,
  "accounts": [
    {
      "id": "account-123",
      "username": "user@example.com",
      "email": "user@example.com",
      "collections": [],
      "items": []
    }
  ]
}
""".trimIndent()

/**
 * Valid CXF payload with empty accounts list.
 */
private val VALID_PAYLOAD_EMPTY_ACCOUNTS = """
{
  "version": {"major": 1, "minor": 0},
  "exporterRpId": "com.example.exporter",
  "exporterDisplayName": "Example Exporter",
  "timestamp": 1704067200,
  "accounts": []
}
""".trimIndent()

/**
 * CXF payload with unsupported major version (2.0).
 */
private val PAYLOAD_UNSUPPORTED_MAJOR_VERSION = """
{
  "version": {"major": 2, "minor": 0},
  "exporterRpId": "com.example.exporter",
  "exporterDisplayName": "Example Exporter",
  "timestamp": 1704067200,
  "accounts": []
}
""".trimIndent()

/**
 * CXF payload with unsupported minor version (1.1).
 */
private val PAYLOAD_UNSUPPORTED_MINOR_VERSION = """
{
  "version": {"major": 1, "minor": 1},
  "exporterRpId": "com.example.exporter",
  "exporterDisplayName": "Example Exporter",
  "timestamp": 1704067200,
  "accounts": []
}
""".trimIndent()

/**
 * Mock export response for testing serialization failures.
 */
private val MOCK_EXPORT_RESPONSE = CredentialExchangeExportResponse(
    version = com.bitwarden.cxf.model.CredentialExchangeVersion(major = 1, minor = 0),
    exporterRpId = "com.example.exporter",
    exporterDisplayName = "Example Exporter",
    timestamp = 1704067200,
    accounts = listOf(
        CredentialExchangeExportResponse.Account(
            id = "account-123",
            username = "user@example.com",
            email = "user@example.com",
            collections = JsonArray(emptyList()),
            items = JsonArray(emptyList()),
        ),
    ),
)
