package com.bitwarden.network.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FillAssistFormsJsonTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize simple login form`() {
        assertEquals(
            SIMPLE_LOGIN_FORMS,
            json.decodeFromString<FillAssistFormsJson>(SIMPLE_LOGIN_JSON),
        )
    }

    @Test
    fun `deserialize host with null value is excluded host`() {
        val result = json.decodeFromString<FillAssistFormsJson>(NULL_HOST_JSON)
        assertNull(result.hosts?.get("excluded.com"))
    }

    @Test
    fun `deserialize null pathname is excluded path`() {
        val result = json.decodeFromString<FillAssistFormsJson>(NULL_PATHNAME_JSON)
        assertNull(result.hosts?.get("example.com")?.pathnames?.get("/excluded"))
    }

    @Test
    fun `deserialize composite OTP field`() {
        assertEquals(
            OTP_FORMS,
            json.decodeFromString<FillAssistFormsJson>(OTP_JSON),
        )
    }

    @Test
    fun `deserialize compound selector with multiple alternatives`() {
        assertEquals(
            HONEYPOT_FORMS,
            json.decodeFromString<FillAssistFormsJson>(HONEYPOT_JSON),
        )
    }

    @Test
    fun `deserialize form with container`() {
        assertEquals(
            CONTAINER_FORMS,
            json.decodeFromString<FillAssistFormsJson>(CONTAINER_JSON),
        )
    }

    @Test
    fun `deserialize form with actions field is graceful`() {
        // actions is intentionally excluded from FormJson — handled by ignoreUnknownKeys.
        assertEquals(
            SIMPLE_LOGIN_FORMS,
            json.decodeFromString<FillAssistFormsJson>(SIMPLE_LOGIN_WITH_ACTIONS_JSON),
        )
    }
}

private val SIMPLE_LOGIN_FORMS = FillAssistFormsJson(
    schemaVersion = "1.0.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = listOf(
                FillAssistFormsJson.FormJson(
                    category = "account-login",
                    container = null,
                    fields = mapOf(
                        "username" to JsonArray(listOf(JsonPrimitive("input#username"))),
                        "password" to JsonArray(listOf(JsonPrimitive("input#password"))),
                    ),
                ),
            ),
            pathnames = null,
        ),
    ),
)

private val OTP_FORMS = FillAssistFormsJson(
    schemaVersion = "1.0.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = null,
            pathnames = mapOf(
                "/login" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-login",
                            container = null,
                            fields = mapOf(
                                "oneTimeCode" to JsonArray(
                                    listOf(
                                        JsonArray(
                                            listOf(
                                                JsonPrimitive("input[name='otp-0']"),
                                                JsonPrimitive("input[name='otp-1']"),
                                                JsonPrimitive("input[name='otp-2']"),
                                                JsonPrimitive("input[name='otp-3']"),
                                                JsonPrimitive("input[name='otp-4']"),
                                                JsonPrimitive("input[name='otp-5']"),
                                            ),
                                        ),
                                    ),
                                ),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private val HONEYPOT_FORMS = FillAssistFormsJson(
    schemaVersion = "1.0.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = listOf(
                FillAssistFormsJson.FormJson(
                    category = "account-login",
                    container = null,
                    fields = mapOf(
                        "username" to JsonArray(
                            listOf(
                                JsonPrimitive("input#password[name='password']"),
                                JsonPrimitive("input[name='password']"),
                            ),
                        ),
                        "password" to JsonArray(
                            listOf(
                                JsonPrimitive("input#username[name='username']"),
                                JsonPrimitive("input[name='username']"),
                            ),
                        ),
                    ),
                ),
            ),
            pathnames = null,
        ),
    ),
)

private val CONTAINER_FORMS = FillAssistFormsJson(
    schemaVersion = "1.0.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = null,
            pathnames = mapOf(
                "/login" to FillAssistFormsJson.PathnameEntryJson(
                    forms = listOf(
                        FillAssistFormsJson.FormJson(
                            category = "account-login",
                            container = listOf("div#login-container"),
                            fields = mapOf(
                                "username" to JsonArray(listOf(JsonPrimitive("input#user"))),
                                "password" to JsonArray(listOf(JsonPrimitive("input#pass"))),
                            ),
                        ),
                    ),
                ),
            ),
        ),
    ),
)

private const val SIMPLE_LOGIN_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "example.com": {
      "forms": [
        {
          "category": "account-login",
          "fields": {
            "username": ["input#username"],
            "password": ["input#password"]
          }
        }
      ]
    }
  }
}
"""

private const val NULL_HOST_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "excluded.com": null
  }
}
"""

private const val NULL_PATHNAME_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "example.com": {
      "pathnames": {
        "/excluded": null
      }
    }
  }
}
"""

private const val OTP_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "example.com": {
      "pathnames": {
        "/login": {
          "forms": [
            {
              "category": "account-login",
              "fields": {
                "oneTimeCode": [
                  ["input[name='otp-0']","input[name='otp-1']","input[name='otp-2']",
                   "input[name='otp-3']","input[name='otp-4']","input[name='otp-5']"]
                ]
              }
            }
          ]
        }
      }
    }
  }
}
"""

private const val HONEYPOT_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "example.com": {
      "forms": [
        {
          "category": "account-login",
          "fields": {
            "username": ["input#password[name='password']", "input[name='password']"],
            "password": ["input#username[name='username']", "input[name='username']"]
          }
        }
      ]
    }
  }
}
"""

private const val CONTAINER_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "example.com": {
      "pathnames": {
        "/login": {
          "forms": [
            {
              "category": "account-login",
              "container": ["div#login-container"],
              "fields": {
                "username": ["input#user"],
                "password": ["input#pass"]
              }
            }
          ]
        }
      }
    }
  }
}
"""

// Same structure as SIMPLE_LOGIN_JSON with the actions field present — verifies that
// actions (intentionally omitted from FormJson) is handled by ignoreUnknownKeys = true.
private const val SIMPLE_LOGIN_WITH_ACTIONS_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "example.com": {
      "forms": [
        {
          "category": "account-login",
          "fields": {
            "username": ["input#username"],
            "password": ["input#password"]
          },
          "actions": {
            "submit": ["button#submit"]
          }
        }
      ]
    }
  }
}
"""
