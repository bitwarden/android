package com.bitwarden.network.service

import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.api.ConfigApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.ConfigResponseJson
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create

class ConfigServiceTest : BaseServiceTest() {

    private val api: ConfigApi = retrofit.create()
    private val service = ConfigServiceImpl(api)

    @Test
    fun `getConfig should call ConfigApi`() = runTest {
        server.enqueue(MockResponse().setBody(CONFIG_RESPONSE_JSON))
        assertEquals(CONFIG_RESPONSE.asSuccess(), service.getConfig())
    }
}

private const val CONFIG_RESPONSE_JSON = """
{
  "object": "config",
  "version": "1",
  "gitHash": "gitHash",
  "server": {
    "name": "default",
    "url": "url"
  },
  "environment": {
    "cloudRegion": "US",
    "vault": "vaultUrl",
    "api": "apiUrl",
    "identity": "identityUrl",
    "notifications": "notificationsUrl",
    "sso": "ssoUrl"
  },
  "featureStates": {
    "feature one": false
  }
}
"""
private val CONFIG_RESPONSE = ConfigResponseJson(
    type = "config",
    version = "1",
    gitHash = "gitHash",
    server = ConfigResponseJson.ServerJson(
        name = "default",
        url = "url",
    ),
    environment = ConfigResponseJson.EnvironmentJson(
        cloudRegion = "US",
        vaultUrl = "vaultUrl",
        apiUrl = "apiUrl",
        notificationsUrl = "notificationsUrl",
        identityUrl = "identityUrl",
        ssoUrl = "ssoUrl",
    ),
    featureStates = mapOf(
        "feature one" to JsonPrimitive(false),
    ),
)
