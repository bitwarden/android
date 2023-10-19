package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach

class MainViewModelTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(LOGIN_RESULT_PATH)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(LOGIN_RESULT_PATH)
    }

    @Test
    fun `ReceiveNewIntent with captcha host should call setCaptchaCallbackToken`() {
        val authRepository = mockk<AuthRepository> {
            every {
                setCaptchaCallbackTokenResult(
                    tokenResult = CaptchaCallbackTokenResult.Success(
                        token = "mockk_token",
                    ),
                )
            } returns Unit
        }
        val mockIntent = mockk<Intent> {
            every { data?.host } returns "captcha-callback"
            every { data?.getQueryParameter("token") } returns "mockk_token"
            every { action } returns Intent.ACTION_VIEW
        }
        val viewModel = MainViewModel(
            authRepository = authRepository,
        )
        viewModel.sendAction(
            MainAction.ReceiveNewIntent(
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

    companion object {
        private const val LOGIN_RESULT_PATH =
            "com.x8bit.bitwarden.data.auth.datasource.network.util.LoginResultExtensionsKt"
    }
}
