package com.x8bit.bitwarden.ui.auth.feature.masterpasswordhint

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MasterPasswordHintViewModelTest : BaseViewModelTest() {

    @Suppress("MaxLineLength")
    @Test
    fun `initial state should be correct`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `on CloseClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(MasterPasswordHintAction.CloseClick)
            assertEquals(MasterPasswordHintEvent.NavigateBack, awaitItem())
        }
    }

    private fun createViewModel(
        state: MasterPasswordHintState? = DEFAULT_STATE,
    ): MasterPasswordHintViewModel = MasterPasswordHintViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: MasterPasswordHintState = MasterPasswordHintState(
    emailInput = "email",
)
