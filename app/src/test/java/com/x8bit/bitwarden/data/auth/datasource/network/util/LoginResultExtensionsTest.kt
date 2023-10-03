package com.x8bit.bitwarden.data.auth.datasource.network.util

import android.content.Intent
import android.net.Uri
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Test

class LoginResultExtensionsTest {

    @Test
    fun `generateIntentForCaptcha should return valid Intent`() {
        val captchaRequired = LoginResult.CaptchaRequired("testCaptchaId")
        val intent = captchaRequired.generateIntentForCaptcha()
        val expectedUrl = "https://vault.bitwarden.com/captcha-mobile-connector.html" +
            "?data=eyJzaXRlS2V5IjoidGVzdENhcHRjaGkxZGQiLCJsb2NhbGUiOiJlbl9VUyJ9" +
            "&parent=bitwarden%3A%2F%2Fcaptcha-callback&v=1"
        val expectedIntent = Intent(Intent.ACTION_VIEW, Uri.parse(expectedUrl))
        assertEquals(expectedIntent.action, intent.action)
        assertEquals(expectedIntent.data, intent.data)
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
