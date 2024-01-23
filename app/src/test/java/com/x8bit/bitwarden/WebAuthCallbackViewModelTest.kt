package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.SsoCallbackResult
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getSsoCallbackResult
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
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(Intent::getCaptchaCallbackTokenResult)
        mockkStatic(Intent::getSsoCallbackResult)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Intent::getCaptchaCallbackTokenResult)
        unmockkStatic(Intent::getSsoCallbackResult)
    }

    @Test
    fun `on ReceiveNewIntent with captcha host should call setCaptchaCallbackToken`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        every {
            mockIntent.getCaptchaCallbackTokenResult()
        } returns CaptchaCallbackTokenResult.Success(
            token = "mockk_token",
        )
        every {
            mockIntent.getSsoCallbackResult()
        } returns null
        viewModel.trySendAction(
            WebAuthCallbackAction.IntentReceive(
                intent = mockIntent,
            ),
        )
        verify {
            authRepository.setCaptchaCallbackTokenResult(
                tokenResult = CaptchaCallbackTokenResult.Success(
                    token = "mockk_token",
                ),
            )
        }
    }

    @Test
    fun `on ReceiveNewIntent with sso host should call setSsoCallbackResult`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        every {
            mockIntent.getSsoCallbackResult()
        } returns SsoCallbackResult.Success(
            state = "mockk_state",
            code = "mockk_code",
        )
        every {
            mockIntent.getCaptchaCallbackTokenResult()
        } returns null
        viewModel.trySendAction(
            WebAuthCallbackAction.IntentReceive(
                intent = mockIntent,
            ),
        )
        verify {
            authRepository.setSsoCallbackResult(
                result = SsoCallbackResult.Success(
                    state = "mockk_state",
                    code = "mockk_code",
                ),
            )
        }
    }

    private fun createViewModel() = WebAuthCallbackViewModel(
        authRepository = authRepository,
    )
}
