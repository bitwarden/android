package com.x8bit.bitwarden.ui.auth.feature.masterpasswordguidance

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MasterPasswordGuidanceViewModelTest : BaseViewModelTest() {

    @Test
    fun `CloseAction should cause NavigateBack event to emit`() = runTest {
        val viewModel = MasterPasswordGuidanceViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MasterPasswordGuidanceAction.CloseAction)
            assertEquals(MasterPasswordGuidanceEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `TryPasswordGeneratorAction should cause NavigateToPasswordGenerator to emit`() = runTest {
        val viewModel = MasterPasswordGuidanceViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MasterPasswordGuidanceAction.TryPasswordGeneratorAction)
            assertEquals(MasterPasswordGuidanceEvent.NavigateToPasswordGenerator, awaitItem())
        }
    }
}
