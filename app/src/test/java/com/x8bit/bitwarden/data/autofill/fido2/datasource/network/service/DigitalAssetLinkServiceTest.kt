package com.x8bit.bitwarden.data.autofill.fido2.datasource.network.service

import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.api.DigitalAssetLinkApi
import com.x8bit.bitwarden.data.autofill.fido2.datasource.network.model.DigitalAssetLinkResponseJson
import com.x8bit.bitwarden.data.platform.base.BaseServiceTest
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import retrofit2.create

class DigitalAssetLinkServiceTest : BaseServiceTest() {

    private val digitalAssetLinkApi: DigitalAssetLinkApi = retrofit.create()

    private val digitalAssetLinkService: DigitalAssetLinkService = DigitalAssetLinkServiceImpl(
        digitalAssetLinkApi = digitalAssetLinkApi,
    )

    @Test
    fun `getDigitalAssetLinkForRp should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(GET_DIGITAL_ASSET_LINK_SUCCESS_JSON))
        val result = digitalAssetLinkService.getDigitalAssetLinkForRp(
            scheme = url.scheme,
            relyingParty = url.host,
        )
        assertEquals(
            createDigitalAssetLinkResponse(),
            result.getOrThrow(),
        )
    }
}

@Suppress("MaxLineLength")
private fun createDigitalAssetLinkResponse() = listOf(
    DigitalAssetLinkResponseJson(
        relation = listOf(
            "delegate_permission/common.get_login_creds",
            "delegate_permission/common.handle_all_urls",
        ),
        target = DigitalAssetLinkResponseJson.Target(
            namespace = "android_app",
            packageName = "com.mock.package",
            sha256CertFingerprints = listOf(
                "00:01:02:03:04:05:06:07:08:09:0A:0B:0C:0D:0E:0F:10:11:12:13:14:15:16:17:18:19:1A:1B:1C:1D:1E:1F",
            ),
        ),
    ),
)

private const val GET_DIGITAL_ASSET_LINK_SUCCESS_JSON = """
[
  {
    "relation": [
      "delegate_permission/common.get_login_creds",
      "delegate_permission/common.handle_all_urls"
    ],
    "target": {
      "namespace": "android_app",
      "package_name": "com.mock.package",
      "sha256_cert_fingerprints": [
        "00:01:02:03:04:05:06:07:08:09:0A:0B:0C:0D:0E:0F:10:11:12:13:14:15:16:17:18:19:1A:1B:1C:1D:1E:1F"
      ]
    }
  }
]
"""
