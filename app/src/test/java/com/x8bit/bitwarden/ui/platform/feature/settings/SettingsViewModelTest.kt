package com.x8bit.bitwarden.ui.platform.feature.settings

import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsViewModelTest : BaseViewModelTest() {

    private val mutableAutofillBadgeCountFlow = MutableStateFlow(0)
    private val mutableSecurityBadgeCountFlow = MutableStateFlow(0)
    private val settingsRepository = mockk<SettingsRepository> {
        every { allSecuritySettingsBadgeCountFlow } returns mutableSecurityBadgeCountFlow
        every { allAutofillSettingsBadgeCountFlow } returns mutableAutofillBadgeCountFlow
    }

    @Test
    fun `on SettingsClick with ABOUT should emit NavigateAbout`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ABOUT))
            assertEquals(SettingsEvent.NavigateAbout, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with ACCOUNT_SECURITY should emit NavigateAccountSecurity`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY))
            assertEquals(SettingsEvent.NavigateAccountSecurity, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with APPEARANCE should emit NavigateAppearance`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE))
            assertEquals(SettingsEvent.NavigateAppearance, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with AUTO_FILL should emit NavigateAutoFill`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL))
            assertEquals(SettingsEvent.NavigateAutoFill, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with OTHER should emit NavigateOther`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.OTHER))
            assertEquals(SettingsEvent.NavigateOther, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with VAULT should emit NavigateVault`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT))
            assertEquals(SettingsEvent.NavigateVault, awaitItem())
        }
    }

    @Test
    fun `initial state reflects the current state of the repository`() {
        mutableAutofillBadgeCountFlow.update { 1 }
        mutableSecurityBadgeCountFlow.update { 2 }
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        assertEquals(
            SettingsState(
                autoFillCount = 1,
                securityCount = 2,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `State updates when repository emits new values for badge counts`() = runTest {
        val viewModel = SettingsViewModel(settingsRepository = settingsRepository)
        viewModel.stateFlow.test {
            assertEquals(
                SettingsState(
                    autoFillCount = 0,
                    securityCount = 0,
                ),
                awaitItem(),
            )

            mutableSecurityBadgeCountFlow.update { 2 }
            assertEquals(
                SettingsState(
                    autoFillCount = 0,
                    securityCount = 2,
                ),
                awaitItem(),
            )

            mutableAutofillBadgeCountFlow.update { 1 }
            assertEquals(
                SettingsState(
                    autoFillCount = 1,
                    securityCount = 2,
                ),
                awaitItem(),
            )
        }
    }
}
