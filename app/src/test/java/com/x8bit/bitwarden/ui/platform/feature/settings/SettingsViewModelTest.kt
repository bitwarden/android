package com.x8bit.bitwarden.ui.platform.feature.settings

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SettingsViewModelTest : BaseViewModelTest() {

    @Test
    fun `on AccountSecurityClick should emit NavigateAccountSecurity`() = runTest {
        val viewModel = SettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(SettingsAction.AccountSecurityClick)
            assertEquals(SettingsEvent.NavigateAccountSecurity, awaitItem())
        }
    }
}
