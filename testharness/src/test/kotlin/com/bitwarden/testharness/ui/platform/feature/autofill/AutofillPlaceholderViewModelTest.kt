package com.bitwarden.testharness.ui.platform.feature.autofill

import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AutofillPlaceholderViewModelTest : BaseViewModelTest() {

    @Test
    fun `BackClick action emits NavigateBack event`() = runTest {
        val viewModel = AutofillPlaceholderViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(AutofillPlaceholderAction.BackClick)

            assertEquals(
                AutofillPlaceholderEvent.NavigateBack,
                awaitItem(),
            )
        }
    }
}
