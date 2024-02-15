package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getSsoCallbackResult
import com.x8bit.bitwarden.data.auth.util.YubiKeyResult
import com.x8bit.bitwarden.data.auth.util.getYubiKeyResultOrNull
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class WebAuthCallbackViewModelTest : BaseViewModelTest() {
    private val authRepository = mockk<AuthRepository> {
        every { setCaptchaCallbackTokenResult(any()) } just runs
        every { setSsoCallbackResult(any()) } just runs
        every { setYubiKeyResult(any()) } just runs
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(
            Intent::getYubiKeyResultOrNull,
            Intent::getCaptchaCallbackTokenResult,
            Intent::getSsoCallbackResult,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            Intent::getYubiKeyResultOrNull,
            Intent::getCaptchaCallbackTokenResult,
            Intent::getSsoCallbackResult,
        )
    }

    @Test
    fun `on IntentReceive with captcha host should call setCaptchaCallbackToken`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val captchaCallbackTokenResult = CaptchaCallbackTokenResult.Success(token = "mockk_token")
        every { mockIntent.getCaptchaCallbackTokenResult() } returns captchaCallbackTokenResult
        every { mockIntent.getYubiKeyResultOrNull() } returns null
        every { mockIntent.getSsoCallbackResult() } returns null

        viewModel.trySendAction(WebAuthCallbackAction.IntentReceive(intent = mockIntent))
        verify(exactly = 1) {
            authRepository.setCaptchaCallbackTokenResult(tokenResult = captchaCallbackTokenResult)
        }
    }

    @Test
    fun `on IntentReceive with sso host should call setSsoCallbackResult`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val sseCallbackResult = SsoCallbackResult.Success(
            state = "mockk_state",
            code = "mockk_code",
        )
        every { mockIntent.getSsoCallbackResult() } returns sseCallbackResult
        every { mockIntent.getYubiKeyResultOrNull() } returns null
        every { mockIntent.getCaptchaCallbackTokenResult() } returns null

        viewModel.trySendAction(WebAuthCallbackAction.IntentReceive(intent = mockIntent))
        verify(exactly = 1) {
            authRepository.setSsoCallbackResult(result = sseCallbackResult)
        }
    }

    @Test
    fun `on ReceiveNewIntent with Yubi Key Result should call setYubiKeyResult`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val yubiKeyResult = mockk<YubiKeyResult>()
        every { mockIntent.getYubiKeyResultOrNull() } returns yubiKeyResult
        every { mockIntent.getCaptchaCallbackTokenResult() } returns null
        every { mockIntent.getSsoCallbackResult() } returns null

        viewModel.trySendAction(WebAuthCallbackAction.IntentReceive(intent = mockIntent))
        verify(exactly = 1) {
            authRepository.setYubiKeyResult(yubiKeyResult)
        }
    }

    private fun createViewModel() = WebAuthCallbackViewModel(
        authRepository = authRepository,
    )
}
