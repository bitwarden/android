package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultSettingsViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = VaultSettingsViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultSettingsAction.BackClick)
            assertEquals(VaultSettingsEvent.NavigateBack, awaitItem())
        }
    }
}
