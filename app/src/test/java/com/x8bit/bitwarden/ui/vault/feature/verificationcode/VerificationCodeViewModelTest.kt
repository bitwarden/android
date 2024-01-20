package com.x8bit.bitwarden.ui.vault.feature.verificationcode

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VerificationCodeViewModelTest : BaseViewModelTest() {

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(VerificationCodeAction.BackClick)
            assertEquals(VerificationCodeEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on ItemClick should emit ItemClick`() = runTest {
        val viewModel = createViewModel()
        val testId = "testId"

        viewModel.eventFlow.test {
            viewModel.trySendAction(VerificationCodeAction.ItemClick(testId))
            assertEquals(VerificationCodeEvent.NavigateToVaultItem(testId), awaitItem())
        }
    }

    private fun createViewModel(
        state: VerificationCodeState? = DEFAULT_STATE,
    ): VerificationCodeViewModel = VerificationCodeViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
    )
}

private val DEFAULT_STATE: VerificationCodeState = VerificationCodeState(
    VerificationCodeState.ViewState.Empty,
)
