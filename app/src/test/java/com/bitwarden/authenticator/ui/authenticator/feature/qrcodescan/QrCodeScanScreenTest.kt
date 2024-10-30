package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.ui.platform.base.BaseComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertTrue

class QrCodeScanScreenTest : BaseComposeTest() {

    private var onNavigateBackCalled = false
    private var onNavigateToManualCodeEntryScreenCalled = false

    private val qrCodeAnalyzer = FakeQrCodeAnalyzer()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<QrCodeScanEvent>()

    val viewModel: QrCodeScanViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            QrCodeScanScreen(
                viewModel = viewModel,
                qrCodeAnalyzer = qrCodeAnalyzer,
                onNavigateBack = { onNavigateBackCalled = true },
                onNavigateToManualCodeEntryScreen = {
                    onNavigateToManualCodeEntryScreenCalled = true
                },
            )
        }
    }

    @Test
    fun `on NavigateBack event receive should call navigate back`() {
        mutableEventFlow.tryEmit(QrCodeScanEvent.NavigateBack)
        assertTrue(onNavigateBackCalled)
    }

    @Test
    fun `on Save here click should send SaveLocallyClick action`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog = QrCodeScanState.DialogState.ChooseSaveLocation,
            )
        }
        composeTestRule
            .onNodeWithText("Save here")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(QrCodeScanAction.SaveLocallyClick(false)) }

        // Click again but with "Save as default" checked:
        composeTestRule
            .onNodeWithText("Save option as default")
            .performClick()
        composeTestRule
            .onNodeWithText("Save here")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(QrCodeScanAction.SaveLocallyClick(true)) }
    }

    @Test
    fun `on Save to Bitwarden click should send SaveToBitwardenClick action`() {
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog = QrCodeScanState.DialogState.ChooseSaveLocation,
            )
        }
        composeTestRule
            .onNodeWithText("Take me to Bitwarden")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(false)) }

        // Click again but with "Save as default" checked:
        composeTestRule
            .onNodeWithText("Save option as default")
            .performClick()
        composeTestRule
            .onNodeWithText("Take me to Bitwarden")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenClick(true)) }
    }

    @Test
    fun `dismissing error dialog should send SaveToBitwardenErrorDismiss`() {
        // Make sure dialog isn't showing:
        composeTestRule
            .onNodeWithText("Something went wrong")
            .assertDoesNotExist()

        // Display dialog and click OK
        mutableStateFlow.update {
            DEFAULT_STATE.copy(
                dialog = QrCodeScanState.DialogState.SaveToBitwardenError,
            )
        }
        composeTestRule
            .onNodeWithText("Something went wrong")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
        composeTestRule
            .onNodeWithText("OK")
            .assertIsDisplayed()
            .assert(hasAnyAncestor(isDialog()))
            .performClick()

        verify { viewModel.trySendAction(QrCodeScanAction.SaveToBitwardenErrorDismiss) }
    }
}

private val DEFAULT_STATE = QrCodeScanState(
    dialog = null,
)
