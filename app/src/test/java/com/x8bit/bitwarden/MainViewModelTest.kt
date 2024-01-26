package com.x8bit.bitwarden

import android.content.Intent
import app.cash.turbine.test
import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class MainViewModelTest : BaseViewModelTest() {

    private val mutableAppThemeFlow = MutableStateFlow(AppTheme.DEFAULT)
    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val mutableScreenCaptureAllowedFlow = MutableStateFlow(true)
    val authRepository = mockk<AuthRepository> {
        every { userStateFlow } returns mutableUserStateFlow
        every { activeUserId } returns USER_ID
    }
    private val settingsRepository = mockk<SettingsRepository> {
        every { appTheme } returns AppTheme.DEFAULT
        every { appThemeStateFlow } returns mutableAppThemeFlow
        every { isScreenCaptureAllowedStateFlow } returns mutableScreenCaptureAllowedFlow
    }
    private val specialCircumstanceManager = SpecialCircumstanceManagerImpl()
    private val intentManager: IntentManager = mockk {
        every { getShareDataFromIntent(any()) } returns null
    }
    private val savedStateHandle = SavedStateHandle()

    @Suppress("MaxLineLength")
    @Test
    fun `initialization should set a saved SpecialCircumstance to the SpecialCircumstanceManager if present`() {
        assertNull(specialCircumstanceManager.specialCircumstance)

        val specialCircumstance = mockk<SpecialCircumstance>()
        createViewModel(
            initialSpecialCircumstance = specialCircumstance,
        )

        assertEquals(
            specialCircumstance,
            specialCircumstanceManager.specialCircumstance,
        )
    }

    @Test
    fun `SpecialCircumstance updates should update the SavedStateHandle`() {
        createViewModel()

        assertNull(savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY])

        val specialCircumstance = mockk<SpecialCircumstance>()
        specialCircumstanceManager.specialCircumstance = specialCircumstance

        assertEquals(
            specialCircumstance,
            savedStateHandle[SPECIAL_CIRCUMSTANCE_KEY],
        )
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

    @Suppress("MaxLineLength")
    @Test
    fun `changes in the allowed screen capture value should result in emissions of ScreenCaptureSettingChange `() =
        runTest {
            val viewModel = createViewModel()

            viewModel.eventFlow.test {
                assertEquals(
                    MainEvent.ScreenCaptureSettingChange(isAllowed = true),
                    awaitItem(),
                )

                mutableScreenCaptureAllowedFlow.value = false
                assertEquals(
                    MainEvent.ScreenCaptureSettingChange(isAllowed = false),
                    awaitItem(),
                )
            }
        }

    private fun createViewModel(
        initialSpecialCircumstance: SpecialCircumstance? = null,
    ) = MainViewModel(
        specialCircumstanceManager = specialCircumstanceManager,
        settingsRepository = settingsRepository,
        intentManager = intentManager,
        savedStateHandle = savedStateHandle.apply {
            set(SPECIAL_CIRCUMSTANCE_KEY, initialSpecialCircumstance)
        },
    )

    companion object {
        private const val SPECIAL_CIRCUMSTANCE_KEY = "special-circumstance"
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
