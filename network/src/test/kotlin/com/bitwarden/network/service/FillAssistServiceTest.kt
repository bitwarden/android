package com.bitwarden.network.service

import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.api.FillAssistApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.FillAssistFormsJson
import com.bitwarden.network.model.FillAssistManifestJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import retrofit2.create

class FillAssistServiceTest : BaseServiceTest() {

    private val api: FillAssistApi = retrofit.create()
    private val service = FillAssistServiceImpl(api = api)

    @Test
    fun `getManifest should parse manifest response`() = runTest {
        server.enqueue(MockResponse().setBody(MANIFEST_JSON))
        assertEquals(MANIFEST.asSuccess(), service.getManifest(url = "$urlPrefix/manifest.json"))
    }

    @Test
    fun `getManifest should return failure on server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        assertTrue(service.getManifest(url = "$urlPrefix/manifest.json").isFailure)
    }

    @Test
    fun `getForms should parse and return forms`() = runTest {
        server.enqueue(MockResponse().setBody(FORMS_V1_JSON))
        assertEquals(FORMS_V1.asSuccess(), service.getForms(formsUrl = "$urlPrefix/forms.v1.json"))
    }

    @Test
    fun `getForms should return failure on server error`() = runTest {
        server.enqueue(MockResponse().setResponseCode(404))
        assertTrue(service.getForms(formsUrl = "$urlPrefix/forms.v1.json").isFailure)
    }
}

private val MANIFEST = FillAssistManifestJson(
    buildId = "local-build",
    timestamp = "2026-05-20T15:01:02.956Z",
    gitSha = "abc123",
    maps = FillAssistManifestJson.MapsJson(
        forms = mapOf(
            "v1" to FillAssistManifestJson.FileEntryJson(
                filename = "forms.v1.json",
                cid = "sha256:5b8f688d24bb9c38b4094838fa2baacb3cc4ab302e3545adf016b05f6b6b96db",
                schema = "forms.v1.schema.json",
                deprecated = null,
            ),
        ),
    ),
)

private val FORMS_V1 = FillAssistFormsJson(
    schemaVersion = "1.0.0",
    hosts = mapOf(
        "example.com" to FillAssistFormsJson.HostEntryJson(
            forms = listOf(
                FillAssistFormsJson.FormJson(
                    category = "account-login",
                    container = null,
                    fields = mapOf(
                        "username" to JsonArray(listOf(JsonPrimitive("input#user"))),
                        "password" to JsonArray(listOf(JsonPrimitive("input#pass"))),
                    ),
                ),
            ),
            pathnames = null,
        ),
    ),
)

private const val MANIFEST_JSON = """
{
  "buildId": "local-build",
  "timestamp": "2026-05-20T15:01:02.956Z",
  "gitSha": "abc123",
  "maps": {
    "forms": {
      "v1": {
        "filename": "forms.v1.json",
        "cid": "sha256:5b8f688d24bb9c38b4094838fa2baacb3cc4ab302e3545adf016b05f6b6b96db",
        "schema": "forms.v1.schema.json"
      }
    }
  }
}
"""

private const val FORMS_V1_JSON = """
{
  "schemaVersion": "1.0.0",
  "hosts": {
    "example.com": {
      "forms": [
        {
          "category": "account-login",
          "fields": {
            "username": ["input#user"],
            "password": ["input#pass"]
          }
        }
      ]
    }
  }
}
"""
