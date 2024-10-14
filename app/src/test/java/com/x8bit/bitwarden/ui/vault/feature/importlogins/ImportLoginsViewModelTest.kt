package com.x8bit.bitwarden.ui.vault.feature.importlogins

import app.cash.turbine.test
import app.cash.turbine.turbineScope
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ImportLoginsViewModelTest : BaseViewModelTest() {

    @Test
    fun `initial state is correct`() {
        val viewModel = createViewModel()
        assertEquals(
            DEFAULT_STATE,
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `GetStartedClick sets dialog state to GetStarted`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.GetStartedClick)
        assertEquals(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.GetStarted,
                viewState = ImportLoginsState.ViewState.InitialContent,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `ImportLaterClick sets dialog state to ImportLater`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.ImportLaterClick)
        assertEquals(
            ImportLoginsState(
                dialogState = ImportLoginsState.DialogState.ImportLater,
                viewState = ImportLoginsState.ViewState.InitialContent,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `DismissDialog sets dialog state to null`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            assertEquals(
                DEFAULT_STATE,
                awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.GetStartedClick)
            assertEquals(
                ImportLoginsState(
                    dialogState = ImportLoginsState.DialogState.GetStarted,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                ),
                awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.DismissDialog)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmImportLater sets dialog state to null and sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        turbineScope {
            val stateFlow = viewModel.stateFlow.testIn(backgroundScope)
            val eventFlow = viewModel.eventFlow.testIn(backgroundScope)
            // Initial state
            assertEquals(DEFAULT_STATE, stateFlow.awaitItem())

            // Set the dialog state to ImportLater
            viewModel.trySendAction(ImportLoginsAction.ImportLaterClick)
            assertEquals(
                ImportLoginsState(
                    dialogState = ImportLoginsState.DialogState.ImportLater,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                ),
                stateFlow.awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.ConfirmImportLater)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                ),
                stateFlow.awaitItem(),
            )
            assertEquals(
                ImportLoginsEvent.NavigateBack,
                eventFlow.awaitItem(),
            )
        }
    }

    @Test
    fun `ConfirmGetStarted sets dialog state to null and view state to step one`() = runTest {
        val viewModel = createViewModel()
        viewModel.stateFlow.test {
            // Initial state
            assertEquals(DEFAULT_STATE, awaitItem())

            // Set the dialog state to GetStarted
            viewModel.trySendAction(ImportLoginsAction.GetStartedClick)
            assertEquals(
                ImportLoginsState(
                    dialogState = ImportLoginsState.DialogState.GetStarted,
                    viewState = ImportLoginsState.ViewState.InitialContent,
                ),
                awaitItem(),
            )
            viewModel.trySendAction(ImportLoginsAction.ConfirmGetStarted)
            assertEquals(
                ImportLoginsState(
                    dialogState = null,
                    viewState = ImportLoginsState.ViewState.ImportStepOne,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `CloseClick sends NavigateBack event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportLoginsAction.CloseClick)
            assertEquals(
                ImportLoginsEvent.NavigateBack,
                awaitItem(),
            )
        }
    }

    @Test
    fun `HelpClick sends OpenHelpLink event`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(ImportLoginsAction.HelpClick)
            assertEquals(
                ImportLoginsEvent.OpenHelpLink,
                awaitItem(),
            )
        }
    }

    @Test
    fun `MoveToStepOne sets view state to ImportStepOne`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToStepOne)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.ImportStepOne,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToStepTwo sets view state to ImportStepTwo`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToStepTwo)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.ImportStepTwo,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToStepThree sets view state to ImportStepThree`() {
        val viewModel = createViewModel()
        viewModel.trySendAction(ImportLoginsAction.MoveToStepThree)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.ImportStepThree,
            ),
            viewModel.stateFlow.value,
        )
    }

    @Test
    fun `MoveToInitialContent sets view state to InitialContent`() {
        val viewModel = createViewModel()
        // first set to step one
        viewModel.trySendAction(ImportLoginsAction.MoveToStepOne)
        assertTrue(viewModel.stateFlow.value.viewState is ImportLoginsState.ViewState.ImportStepOne)
        // now move back to intial
        viewModel.trySendAction(ImportLoginsAction.MoveToInitialContent)
        assertEquals(
            ImportLoginsState(
                dialogState = null,
                viewState = ImportLoginsState.ViewState.InitialContent,
            ),
            viewModel.stateFlow.value,
        )
    }

    private fun createViewModel(): ImportLoginsViewModel = ImportLoginsViewModel()
}

private val DEFAULT_STATE = ImportLoginsState(
    dialogState = null,
    viewState = ImportLoginsState.ViewState.InitialContent,
)
