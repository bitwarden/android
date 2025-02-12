package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.bitwarden.authenticator.data.platform.repository.util.bufferedMutableSharedFlow
import com.bitwarden.authenticator.ui.platform.base.BaseComposeTest
import com.bitwarden.authenticator.ui.platform.manager.intent.IntentManager
import com.bitwarden.authenticator.ui.platform.manager.permissions.FakePermissionManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class ManualCodeEntryScreenTest : BaseComposeTest() {

    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val mutableEventFlow = bufferedMutableSharedFlow<ManualCodeEntryEvent>()

    private val viewModel: ManualCodeEntryViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    private val intentManager: IntentManager = mockk()
    private val permissionsManager = FakePermissionManager()

    @Before
    fun setup() {
        composeTestRule.setContent {
            ManualCodeEntryScreen(
                onNavigateBack = {},
                onNavigateToQrCodeScreen = {},
                viewModel = viewModel,
                intentManager = intentManager,
                permissionsManager = permissionsManager,
            )
        }
    }

    @Test
    fun `on Add code click should emit SaveLocallyClick`() {
        composeTestRule
            .onNodeWithText("Add code")
            .performClick()

        // Make sure save to bitwaren isn't showing:
        composeTestRule
            .onNodeWithText("Add code to Bitwarden")
            .assertDoesNotExist()

        verify { viewModel.trySendAction(ManualCodeEntryAction.SaveLocallyClick) }
    }

    @Test
    fun `on Add code to Bitwarden click should emit SaveToBitwardenClick`() {
        mutableStateFlow.update {
            it.copy(buttonState = ManualCodeEntryState.ButtonState.SaveToBitwardenPrimary)
        }
        composeTestRule
            .onNodeWithText("Save to Bitwarden")
            .performClick()

        // Make sure locally only save isn't showing:
        composeTestRule
            .onNodeWithText("Add code")
            .assertDoesNotExist()

        // Make sure locally option is showing:
        composeTestRule
            .onNodeWithText("Save here")
            .assertIsDisplayed()

        verify { viewModel.trySendAction(ManualCodeEntryAction.SaveToBitwardenClick) }
    }

    @Test
    fun `on Add code locally click should emit SaveLocallyClick`() {
        mutableStateFlow.update {
            it.copy(buttonState = ManualCodeEntryState.ButtonState.SaveLocallyPrimary)
        }
        composeTestRule
            .onNodeWithText("Save here")
            .performClick()

        // Make sure locally only save isn't showing:
        composeTestRule
            .onNodeWithText("Add code")
            .assertDoesNotExist()

        // Make sure save to bitwarden option is showing:
        composeTestRule
            .onNodeWithText("Save to Bitwarden")
            .assertIsDisplayed()

        verify { viewModel.trySendAction(ManualCodeEntryAction.SaveLocallyClick) }
    }
}

private val DEFAULT_STATE = ManualCodeEntryState(
    code = "",
    issuer = "",
    dialog = null,
    buttonState = ManualCodeEntryState.ButtonState.LocalOnly,
)
