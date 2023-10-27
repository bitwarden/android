package com.x8bit.bitwarden.ui.platform.feature.settings.about

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AboutViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = AboutViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.BackClick)
            assertEquals(AboutEvent.NavigateBack, awaitItem())
        }
    }
}
