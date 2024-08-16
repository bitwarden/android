package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.DuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.WebAuthResult
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getDuoCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getSsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.getWebAuthResultOrNull
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

class AuthCallbackViewModelTest : BaseViewModelTest() {
    private val authRepository = mockk<AuthRepository> {
        every { setCaptchaCallbackTokenResult(any()) } just runs
        every { setSsoCallbackResult(any()) } just runs
        every { setDuoCallbackTokenResult(any()) } just runs
        every { setYubiKeyResult(any()) } just runs
        every { setWebAuthResult(any()) } just runs
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(
            Intent::getYubiKeyResultOrNull,
            Intent::getWebAuthResultOrNull,
            Intent::getCaptchaCallbackTokenResult,
            Intent::getDuoCallbackTokenResult,
            Intent::getSsoCallbackResult,
        )
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            Intent::getYubiKeyResultOrNull,
            Intent::getWebAuthResultOrNull,
            Intent::getCaptchaCallbackTokenResult,
            Intent::getDuoCallbackTokenResult,
            Intent::getSsoCallbackResult,
        )
    }

    @Test
    fun `on IntentReceive with captcha host should call setCaptchaCallbackToken`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val captchaCallbackTokenResult = CaptchaCallbackTokenResult.Success(token = "mockk_token")
        every { mockIntent.getCaptchaCallbackTokenResult() } returns captchaCallbackTokenResult
        every { mockIntent.getDuoCallbackTokenResult() } returns null
        every { mockIntent.getYubiKeyResultOrNull() } returns null
        every { mockIntent.getWebAuthResultOrNull() } returns null
        every { mockIntent.getSsoCallbackResult() } returns null

        viewModel.trySendAction(AuthCallbackAction.IntentReceive(intent = mockIntent))
        verify(exactly = 1) {
            authRepository.setCaptchaCallbackTokenResult(tokenResult = captchaCallbackTokenResult)
        }
    }

    @Test
    fun `on IntentReceive with duo host should call setDuoCallbackToken`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val duoCallbackTokenResult = DuoCallbackTokenResult.Success(token = "mockk_token")
        every { mockIntent.getCaptchaCallbackTokenResult() } returns null
        every { mockIntent.getDuoCallbackTokenResult() } returns duoCallbackTokenResult
        every { mockIntent.getYubiKeyResultOrNull() } returns null
        every { mockIntent.getWebAuthResultOrNull() } returns null
        every { mockIntent.getSsoCallbackResult() } returns null

        viewModel.trySendAction(AuthCallbackAction.IntentReceive(intent = mockIntent))
        verify(exactly = 1) {
            authRepository.setDuoCallbackTokenResult(tokenResult = duoCallbackTokenResult)
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
        every { mockIntent.getWebAuthResultOrNull() } returns null
        every { mockIntent.getCaptchaCallbackTokenResult() } returns null
        every { mockIntent.getDuoCallbackTokenResult() } returns null

        viewModel.trySendAction(AuthCallbackAction.IntentReceive(intent = mockIntent))
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
        every { mockIntent.getWebAuthResultOrNull() } returns null
        every { mockIntent.getCaptchaCallbackTokenResult() } returns null
        every { mockIntent.getDuoCallbackTokenResult() } returns null
        every { mockIntent.getSsoCallbackResult() } returns null

        viewModel.trySendAction(AuthCallbackAction.IntentReceive(intent = mockIntent))
        verify(exactly = 1) {
            authRepository.setYubiKeyResult(yubiKeyResult)
        }
    }

    @Test
    fun `on ReceiveNewIntent with Web Auth Result should call setWebAuthResult`() {
        val viewModel = createViewModel()
        val webAuthResult = mockk<WebAuthResult>()
        val mockIntent = mockk<Intent> {
            every { getWebAuthResultOrNull() } returns webAuthResult
            every { getYubiKeyResultOrNull() } returns null
            every { getCaptchaCallbackTokenResult() } returns null
            every { getDuoCallbackTokenResult() } returns null
            every { getSsoCallbackResult() } returns null
        }

        viewModel.trySendAction(AuthCallbackAction.IntentReceive(intent = mockIntent))
        verify(exactly = 1) {
            authRepository.setWebAuthResult(webAuthResult)
        }
    }

    private fun createViewModel() = AuthCallbackViewModel(
        authRepository = authRepository,
    )
}
