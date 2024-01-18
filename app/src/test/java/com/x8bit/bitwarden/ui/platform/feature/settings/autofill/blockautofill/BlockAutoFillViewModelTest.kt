package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.blockautofill

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.jupiter.api.Test

class BlockAutoFillViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(BlockAutoFillAction.BackClick)
            assertEquals(BlockAutoFillEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        state: BlockAutoFillState? = DEFAULT_STATE,
    ): BlockAutoFillViewModel = BlockAutoFillViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: BlockAutoFillState = BlockAutoFillState(
    BlockAutoFillState.ViewState.Empty,
)
