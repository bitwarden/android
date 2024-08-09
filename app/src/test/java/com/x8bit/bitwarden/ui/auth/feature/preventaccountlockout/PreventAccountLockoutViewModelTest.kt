package com.x8bit.bitwarden.ui.auth.feature.preventaccountlockout

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PreventAccountLockoutViewModelTest : BaseViewModelTest() {

    @Test
    fun `When handling CloseClickAction a NavigateBack event is emitted`() = runTest {
        val viewModel = PreventAccountLockoutViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(PreventAccountLockoutAction.CloseClickAction)
            assertEquals(PreventAccountLockoutEvent.NavigateBack, awaitItem())
        }
    }
}
