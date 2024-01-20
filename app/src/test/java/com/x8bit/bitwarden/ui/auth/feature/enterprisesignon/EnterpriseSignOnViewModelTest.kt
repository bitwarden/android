package com.x8bit.bitwarden.ui.auth.feature.enterprisesignon

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnterpriseSignOnViewModelTest : BaseViewModelTest() {

    private val savedStateHandle = SavedStateHandle()

    @Test
    fun `initial state should be correct when not pulling from handle`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(DEFAULT_STATE, awaitItem())
        }
    }

    @Test
    fun `initial state should pull from handle when present`() = runTest {
        val expectedState = DEFAULT_STATE.copy(
            orgIdentifierInput = "test",
        )
        val viewModel = createViewModel(expectedState)
        viewModel.stateFlow.test {
            assertEquals(expectedState, awaitItem())
        }
    }

    @Test
    fun `CloseButtonClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnterpriseSignOnAction.CloseButtonClick)
            assertEquals(
                EnterpriseSignOnEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `LogInClick with valid organization should emit ShowToast`() = runTest {
        val state = DEFAULT_STATE.copy(orgIdentifierInput = "Test")
        val viewModel = createViewModel(state)
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnterpriseSignOnAction.LogInClick)
            assertEquals(state, viewModel.stateFlow.value)
            assertEquals(
                EnterpriseSignOnEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `LogInClick with invalid organization should emit ShowToast and show error dialog`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnterpriseSignOnAction.LogInClick)
            assertEquals(
                DEFAULT_STATE.copy(
                    dialogState = EnterpriseSignOnState.DialogState.Error(
                        R.string.validation_field_required.asText(
                            R.string.org_identifier.asText(),
                        ),
                    ),
                ),
                viewModel.stateFlow.value,
            )
            assertEquals(
                EnterpriseSignOnEvent.ShowToast("Not yet implemented."),
                awaitItem(),
            )
        }
    }

    @Test
    fun `OrgIdentifierInputChange should update organization identifier`() = runTest {
        val input = "input"
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.actionChannel.trySend(EnterpriseSignOnAction.OrgIdentifierInputChange(input))
            assertEquals(
                DEFAULT_STATE.copy(orgIdentifierInput = input),
                viewModel.stateFlow.value,
            )
        }
    }

    @Test
    fun `DialogDismiss should clear the active dialog when DialogState is Error`() {
        val initialState = DEFAULT_STATE.copy(
            dialogState = EnterpriseSignOnState.DialogState.Error(
                message = "Error".asText(),
            ),
        )
        val viewModel = createViewModel(initialState)
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss)

        assertEquals(
            initialState.copy(dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DialogDismiss should clear the active dialog when DialogState is Loading`() {
        val initialState = DEFAULT_STATE.copy(
            dialogState = EnterpriseSignOnState.DialogState.Loading(
                message = "Loading".asText(),
            ),
        )
        val viewModel = createViewModel(initialState)
        assertEquals(
            initialState,
            viewModel.stateFlow.value,
        )

        viewModel.trySendAction(EnterpriseSignOnAction.DialogDismiss)

        assertEquals(
            initialState.copy(dialogState = null),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(
        initialState: EnterpriseSignOnState? = null,
        savedStateHandle: SavedStateHandle = SavedStateHandle(
            initialState = mapOf("state" to initialState),
        ),
    ): EnterpriseSignOnViewModel = EnterpriseSignOnViewModel(
        savedStateHandle = savedStateHandle,
    )

    companion object {
        private val DEFAULT_STATE = EnterpriseSignOnState(
            dialogState = null,
            orgIdentifierInput = "",
        )
    }
}
