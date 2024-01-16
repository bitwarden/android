package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.CaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MainViewModelTest : BaseViewModelTest() {

    private val mutableAppThemeFlow = MutableStateFlow(AppTheme.DEFAULT)
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
        every { activeUserId } returns USER_ID
        every {
            setCaptchaCallbackTokenResult(
                tokenResult = CaptchaCallbackTokenResult.Success(
                    token = "mockk_token",
                ),
            )
        } just runs
    }
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
    }

    @Test
    fun `on AppThemeChanged should update state`() {
        val viewModel = createViewModel()

        assertEquals(
            MainState(
                theme = AppTheme.DEFAULT,
            ),
            viewModel.stateFlow.value,
        )
        viewModel.trySendAction(
            MainAction.Internal.ThemeUpdate(
                theme = AppTheme.DARK,
            ),
        )
        assertEquals(
            MainState(
                theme = AppTheme.DARK,
            ),
            viewModel.stateFlow.value,
        )

        verify {
            settingsRepository.appTheme
            settingsRepository.appThemeStateFlow
        }
    }

    @Test
    fun `on ReceiveNewIntent with captcha host should call setCaptchaCallbackToken`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent> {
            every { data?.host } returns "captcha-callback"
            every { data?.getQueryParameter("token") } returns "mockk_token"
            every { action } returns Intent.ACTION_VIEW
        }
        viewModel.trySendAction(
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

    private fun createViewModel() = MainViewModel(
        authRepository = authRepository,
        settingsRepository = settingsRepository,
    )

    companion object {
        private const val USER_ID = "userID"
        private val DEFAULT_USER_STATE = UserState(
            activeUserId = USER_ID,
            accounts = listOf(
                UserState.Account(
                    userId = USER_ID,
                    name = "Active User",
                    email = "active@bitwarden.com",
                    environment = Environment.Us,
                    avatarColorHex = "#aa00aa",
                    isPremium = true,
                    isLoggedIn = true,
                    isVaultUnlocked = true,
                    organizations = emptyList(),
                ),
            ),
        )
    }
}
