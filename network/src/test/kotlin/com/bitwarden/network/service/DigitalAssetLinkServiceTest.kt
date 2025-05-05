package com.bitwarden.network.service

import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.api.DigitalAssetLinkApi
import com.bitwarden.network.base.BaseServiceTest
import com.bitwarden.network.model.DigitalAssetLinkCheckResponseJson
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
    fun `checkDigitalAssetLinksRelations should return the correct response`() = runTest {
        server.enqueue(MockResponse().setBody(CHECK_DIGITAL_ASSET_LINKS_RELATIONS_SUCCESS_JSON))
        assertEquals(
            DigitalAssetLinkCheckResponseJson(
                linked = true,
                maxAge = "47.535162130s",
                debugString = null,
            )
                .asSuccess(),
            digitalAssetLinkService.checkDigitalAssetLinksRelations(
                packageName = "com.x8bit.bitwarden",
                certificateFingerprint =
                    "00:01:02:03:04:05:06:07:08:09:0A:0B:0C:0D:0E:0F:10:11:12:13",
                relation = "delegate_permission/common.handle_all_urls",
            ),
        )
    }
}

private val CHECK_DIGITAL_ASSET_LINKS_RELATIONS_SUCCESS_JSON = """
{
    "linked": true,
    "maxAge": "47.535162130s"
}
""".trimIndent()
