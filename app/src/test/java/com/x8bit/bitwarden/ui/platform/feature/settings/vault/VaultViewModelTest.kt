package com.x8bit.bitwarden.ui.platform.feature.settings.vault

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = VaultViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VaultAction.BackClick)
            assertEquals(VaultEvent.NavigateBack, awaitItem())
        }
    }
}
