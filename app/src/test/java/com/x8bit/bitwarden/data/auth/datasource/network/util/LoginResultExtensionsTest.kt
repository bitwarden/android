package com.x8bit.bitwarden.data.auth.datasource.network.util

import android.content.Intent
import android.net.Uri
import com.x8bit.bitwarden.data.auth.datasource.network.model.LoginResult
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
}
