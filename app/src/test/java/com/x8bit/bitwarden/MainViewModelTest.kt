package com.x8bit.bitwarden

import android.content.Intent
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.getCaptchaCallbackTokenResult
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManagerImpl
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.mockk
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
    }
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
    }
    private val specialCircumstanceManager = SpecialCircumstanceManagerImpl()
    private val intentManager: IntentManager = mockk {
        every { getShareDataFromIntent(any()) } returns null
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

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveFirstIntent with share data should set the special circumstance to ShareNewSend`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val shareData = mockk<IntentManager.ShareData>()
        every { mockIntent.getCaptchaCallbackTokenResult() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns shareData

        viewModel.trySendAction(
            MainAction.ReceiveFirstIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.ShareNewSend(
                data = shareData,
                shouldFinishWhenComplete = true,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on ReceiveNewIntent with share data should set the special circumstance to ShareNewSend`() {
        val viewModel = createViewModel()
        val mockIntent = mockk<Intent>()
        val shareData = mockk<IntentManager.ShareData>()
        every { mockIntent.getCaptchaCallbackTokenResult() } returns null
        every { intentManager.getShareDataFromIntent(mockIntent) } returns shareData

        viewModel.trySendAction(
            MainAction.ReceiveNewIntent(
                intent = mockIntent,
            ),
        )
        assertEquals(
            SpecialCircumstance.ShareNewSend(
                data = shareData,
                shouldFinishWhenComplete = false,
            ),
            specialCircumstanceManager.specialCircumstance,
        )
    }

    private fun createViewModel() = MainViewModel(
        specialCircumstanceManager = specialCircumstanceManager,
        settingsRepository = settingsRepository,
        intentManager = intentManager,
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
