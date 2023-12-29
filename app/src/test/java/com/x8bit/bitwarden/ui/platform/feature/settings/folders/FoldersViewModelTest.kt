package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FoldersViewModelTest : BaseViewModelTest() {

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FoldersAction.CloseButtonClick)
            assertEquals(FoldersEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `AddFolderButtonClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FoldersAction.AddFolderButtonClick)
            assertEquals(
                FoldersEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): FoldersViewModel = FoldersViewModel()
}
