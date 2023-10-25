package com.x8bit.bitwarden.ui.platform.feature.settings

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsViewModelTest : BaseViewModelTest() {

    @Test
    fun `on SettingsClick with ABOUT should emit nothing`() = runTest {
        val viewModel = SettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ABOUT))
            expectNoEvents()
        }
    }

    @Test
    fun `on SettingsClick with ACCOUNT_SECURITY should emit NavigateAccountSecurity`() = runTest {
        val viewModel = SettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.ACCOUNT_SECURITY))
            assertEquals(SettingsEvent.NavigateAccountSecurity, awaitItem())
        }
    }

    @Test
    fun `on SettingsClick with APPEARANCE should emit nothing`() = runTest {
        val viewModel = SettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.APPEARANCE))
            expectNoEvents()
        }
    }

    @Test
    fun `on SettingsClick with AUTO_FILL should emit nothing`() = runTest {
        val viewModel = SettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.AUTO_FILL))
            expectNoEvents()
        }
    }

    @Test
    fun `on SettingsClick with OTHER should emit nothing`() = runTest {
        val viewModel = SettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.OTHER))
            expectNoEvents()
        }
    }

    @Test
    fun `on SettingsClick with VAULT should emit nothing`() = runTest {
        val viewModel = SettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.SettingsClick(Settings.VAULT))
            expectNoEvents()
        }
    }
}
