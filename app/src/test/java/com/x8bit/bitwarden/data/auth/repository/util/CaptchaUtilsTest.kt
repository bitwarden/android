package com.x8bit.bitwarden.data.auth.repository.util

import android.content.Intent
import android.net.Uri
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptchaUtilsTest : BaseComposeTest() {

    @Test
    fun `generateIntentForCaptcha should return valid Uri`() {
        val actualUri = generateUriForCaptcha(captchaId = "testCaptchaId")
        val expectedUrl = "https://vault.bitwarden.com/captcha-mobile-connector.html" +
            "?data=eyJzaXRlS2V5IjoidGVzdENhcHRjaGFJZCIsImxvY2FsZSI6ImVuX1VTIiwiY2Fsb" +
            "GJhY2tVcmkiOiJiaXR3YXJkZW46Ly9jYXB0Y2hhLWNhbGxiYWNrIiwiY2FwdGNoYVJlcXVp" +
            "cmVkVGV4dCI6IkNhcHRjaGEgcmVxdWlyZWQifQ==&parent=bitwarden%3A%2F%2F" +
            "captcha-callback&v=1"
        val expectedUri = Uri.parse(expectedUrl)
        assertEquals(expectedUri, actualUri)
    }

    @Test
    fun `getCaptchaCallbackToken should return null when data is null`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_VIEW
        }
        val result = intent.getCaptchaCallbackTokenResult()
        assertEquals(null, result)
    }

    @Test
    fun `getCaptchaCallbackToken should return null when action is not Intent ACTION_VIEW`() {
        val intent = mockk<Intent> {
            every { data } returns null
            every { action } returns Intent.ACTION_ANSWER
        }
        val result = intent.getCaptchaCallbackTokenResult()
        assertEquals(null, result)
    }

    @Test
    fun `getCaptchaCallbackToken should return MissingToken with missing token parameter`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("token") } returns null
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "captcha-callback"
        }
        val result = intent.getCaptchaCallbackTokenResult()
        assertEquals(CaptchaCallbackTokenResult.MissingToken, result)
    }

    @Test
    fun `getCaptchaCallbackToken should return Success when token query parameter is present`() {
        val intent = mockk<Intent> {
            every { data?.getQueryParameter("token") } returns "myToken"
            every { action } returns Intent.ACTION_VIEW
            every { data?.host } returns "captcha-callback"
        }
        val result = intent.getCaptchaCallbackTokenResult()
        assertEquals(CaptchaCallbackTokenResult.Success("myToken"), result)
    }
}
