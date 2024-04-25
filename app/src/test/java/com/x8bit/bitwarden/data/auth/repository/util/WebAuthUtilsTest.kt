package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Test

class WebAuthUtilsTest : BaseComposeTest() {

    @Test
    fun `generateUriForWebAuth should return valid Uri`() {
        val baseUrl = "https://vault.bitwarden.com"
        val actualUri = generateUriForWebAuth(
            baseUrl = baseUrl,
            data = JsonObject(emptyMap()),
            headerText = "header",
            buttonText = "button",
            returnButtonText = "returnButton",
        )
        val expectedUrl = baseUrl +
            "/webauthn-mobile-connector.html" +
            "?data=eyJjYWxsYmFja1VyaSI6ImJpdHdhcmRlbjovL3dlYmF1dGhuLWNhbGxiYWNrIiwiZ" +
            "GF0YSI6Int9IiwiaGVhZGVyVGV4dCI6ImhlYWRlciIsImJ0blRleHQiOiJidXR0b24iLCJi" +
            "dG5SZXR1cm5UZXh0IjoicmV0dXJuQnV0dG9uIn0=" +
            "&parent=bitwarden%3A%2F%2Fwebauthn-callback" +
            "&v=2"
        val expectedUri = Uri.parse(expectedUrl)
        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `getWebAuthResultOrNull should return null when data is null`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getWebAuthResultOrNull()
        assertEquals(null, result)
    }

    @Test
    fun `getWebAuthResultOrNull should return null when action is not Intent ACTION_VIEW`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_ANSWER
        }
        val result = intent.getWebAuthResultOrNull()
        assertEquals(null, result)
    }

    @Test
    fun `getWebAuthResultOrNull should return Failure with missing data parameter`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("data") } returns null
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "webauthn-callback"
        }
        val result = intent.getWebAuthResultOrNull()
        assertEquals(WebAuthResult.Failure, result)
    }

    @Test
    fun `getWebAuthResultOrNull should return Success when data query parameter is present`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("data") } returns "myToken"
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "webauthn-callback"
        }
        val result = intent.getWebAuthResultOrNull()
        assertEquals(WebAuthResult.Success("myToken"), result)
    }
}
