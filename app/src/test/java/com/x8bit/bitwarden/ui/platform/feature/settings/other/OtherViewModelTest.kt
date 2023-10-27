package com.x8bit.bitwarden.ui.platform.feature.settings.other

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class OtherViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = OtherViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(OtherAction.BackClick)
            assertEquals(OtherEvent.NavigateBack, awaitItem())
        }
    }
}
