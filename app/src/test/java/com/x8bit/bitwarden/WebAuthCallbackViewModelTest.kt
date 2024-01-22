package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
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
    }

    @BeforeEach
    fun setUp() {
        mockkStatic(Intent::getCaptchaCallbackTokenResult)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(Intent::getCaptchaCallbackTokenResult)
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

    private fun createViewModel() = WebAuthCallbackViewModel(
        authRepository = authRepository,
    )
}
