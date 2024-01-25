package com.x8bit.bitwarden.ui.platform.feature.settings.folders

import app.cash.turbine.test
import com.bitwarden.core.FolderView
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FoldersViewModelTest : BaseViewModelTest() {

    private val mutableFoldersStateFlow = MutableStateFlow(DataState.Loaded(listOf<FolderView>()))

    private val vaultRepository: VaultRepository = mockk {
        every { foldersStateFlow } returns mutableFoldersStateFlow
    }

    @Test
    fun `BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FoldersAction.CloseButtonClick)
            assertEquals(FoldersEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `AddFolderButtonClick should emit NavigateToAddFolderScreen`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(FoldersAction.AddFolderButtonClick)
            assertEquals(
                FoldersEvent.NavigateToAddFolderScreen,
                awaitItem(),
            )
        }
    }

    private fun createViewModel(): FoldersViewModel = FoldersViewModel(
        vaultRepository = vaultRepository,
    )
}
