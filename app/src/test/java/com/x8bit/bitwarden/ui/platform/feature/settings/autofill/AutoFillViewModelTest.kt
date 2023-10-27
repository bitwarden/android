package com.x8bit.bitwarden.ui.platform.feature.settings.autofill

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutoFillViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = AutoFillViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AutoFillAction.BackClick)
            assertEquals(AutoFillEvent.NavigateBack, awaitItem())
        }
    }
}
