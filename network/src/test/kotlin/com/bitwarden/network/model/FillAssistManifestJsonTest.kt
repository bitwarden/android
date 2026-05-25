package com.bitwarden.network.model

import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class FillAssistManifestJsonTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `deserialize full manifest with v0 entry`() {
        assertEquals(
            FULL_MANIFEST,
            json.decodeFromString<FillAssistManifestJson>(MANIFEST_JSON),
        )
    }

    @Test
    fun `deserialize manifest with multiple version entries`() {
        assertEquals(
            MULTI_VERSION_MANIFEST,
            json.decodeFromString<FillAssistManifestJson>(MANIFEST_MULTI_VERSION_JSON),
        )
    }

    @Test
    fun `deserialize manifest with unknown top-level fields is graceful`() {
        assertEquals(
            FULL_MANIFEST,
            json.decodeFromString<FillAssistManifestJson>(MANIFEST_EXTRA_FIELDS_JSON),
        )
    }

    @Test
    fun `deserialize minimal manifest with null fields`() {
        val result = json.decodeFromString<FillAssistManifestJson>("{}")
        assertNull(result.buildId)
        assertNull(result.maps)
    }

    @Test
    fun `deserialize manifest with deprecated version entry`() {
        assertEquals(
            DEPRECATED_MANIFEST,
            json.decodeFromString<FillAssistManifestJson>(MANIFEST_DEPRECATED_JSON),
        )
    }
}

private val FULL_MANIFEST = FillAssistManifestJson(
    buildId = "local-build",
    timestamp = "2026-05-20T15:01:02.956Z",
    gitSha = "abc123",
    maps = FillAssistManifestJson.MapsJson(
        forms = mapOf(
            "v0" to FillAssistManifestJson.FileEntryJson(
                filename = "forms.v0.json",
                cid = "sha256:abc123def456",
                schema = "forms.v0.schema.json",
            ),
        ),
    ),
)

private val MULTI_VERSION_MANIFEST = FillAssistManifestJson(
    buildId = "local-build",
    timestamp = null,
    gitSha = null,
    maps = FillAssistManifestJson.MapsJson(
        forms = mapOf(
            "v0" to FillAssistManifestJson.FileEntryJson(
                filename = "forms.v0.json",
                cid = "sha256:aaa",
                schema = "forms.v0.schema.json",
            ),
            "v1" to FillAssistManifestJson.FileEntryJson(
                filename = "forms.v1.json",
                cid = "sha256:bbb",
                schema = "forms.v1.schema.json",
            ),
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
      "v0": {
        "filename": "forms.v0.json",
        "cid": "sha256:abc123def456",
        "schema": "forms.v0.schema.json"
      }
    }
  }
}
"""

private const val MANIFEST_MULTI_VERSION_JSON = """
{
  "buildId": "local-build",
  "maps": {
    "forms": {
      "v0": { "filename": "forms.v0.json", "cid": "sha256:aaa", "schema": "forms.v0.schema.json" },
      "v1": { "filename": "forms.v1.json", "cid": "sha256:bbb", "schema": "forms.v1.schema.json" }
    }
  }
}
"""

private val DEPRECATED_MANIFEST = FillAssistManifestJson(
    buildId = "local-build",
    timestamp = null,
    gitSha = null,
    maps = FillAssistManifestJson.MapsJson(
        forms = mapOf(
            "v0" to FillAssistManifestJson.FileEntryJson(
                filename = "forms.v0.json",
                cid = "sha256:abc123def456",
                schema = "forms.v0.schema.json",
                deprecated = true,
            ),
        ),
    ),
)

private const val MANIFEST_DEPRECATED_JSON = """
{
  "buildId": "local-build",
  "maps": {
    "forms": {
      "v0": {
        "filename": "forms.v0.json",
        "cid": "sha256:abc123def456",
        "schema": "forms.v0.schema.json",
        "deprecated": true
      }
    }
  }
}
"""

// Same structure as MANIFEST_JSON with an extra unknown key — verifies ignoreUnknownKeys = true.
private const val MANIFEST_EXTRA_FIELDS_JSON = """
{
  "buildId": "local-build",
  "timestamp": "2026-05-20T15:01:02.956Z",
  "gitSha": "abc123",
  "checksums": "ignored",
  "maps": {
    "forms": {
      "v0": {
        "filename": "forms.v0.json",
        "cid": "sha256:abc123def456",
        "schema": "forms.v0.schema.json"
      }
    }
  }
}
"""
