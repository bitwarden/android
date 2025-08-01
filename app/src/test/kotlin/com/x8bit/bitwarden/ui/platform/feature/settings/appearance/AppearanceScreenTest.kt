package com.x8bit.bitwarden.ui.platform.feature.settings.appearance

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.core.util.isBuildVersionAtLeast
import com.bitwarden.ui.platform.feature.settings.appearance.model.AppTheme
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.util.assertNoDialogExists
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import com.x8bit.bitwarden.ui.platform.feature.settings.appearance.model.AppLanguage
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppearanceScreenTest : BitwardenComposeTest() {

    private var haveCalledNavigateBack = false
    private val mutableEventFlow = bufferedMutableSharedFlow<AppearanceEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<AppearanceViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager: IntentManager = mockk {
        every { launchUri(uri = any()) } just runs
    }

    @Before
    fun setup() {
        mockkStatic(::isBuildVersionAtLeast)
        every { isBuildVersionAtLeast(any()) } returns true
        setContent(
            intentManager = intentManager,
        ) {
            AppearanceScreen(
                onNavigateBack = { haveCalledNavigateBack = true },
                viewModel = viewModel,
            )
        }
    }

    @After
    fun tearDown() {
        unmockkStatic(::isBuildVersionAtLeast)
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AppearanceAction.BackClick) }
    }

    @Test
    fun `on language row click should display language selection dialog`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Default (System). Language")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("Language")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on language selection dialog item click should send LanguageChange`() {
        // Clicking the Language row shows the language selection dialog
        composeTestRule
            .onNodeWithContentDescription(label = "Default (System). Language")
            .performScrollTo()
            .performClick()
        // Selecting a language dismisses this dialog and displays the confirmation
        composeTestRule
            .onAllNodesWithText("Afrikaans")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule
            .onAllNodesWithText("Afrikaans")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsNotDisplayed()

        verify {
            viewModel.trySendAction(
                AppearanceAction.LanguageChange(
                    language = AppLanguage.AFRIKAANS,
                ),
            )
        }
    }

    @Test
    fun `on language selection dialog cancel click should dismiss dialog`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Default (System). Language")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on theme row click should display theme selection dialog`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Default (System). Theme")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("Theme")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertIsDisplayed()
    }

    @Test
    fun `on theme selection dialog item click should send ThemeChange`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Default (System). Theme")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("Dark")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()

        verify {
            viewModel.trySendAction(
                AppearanceAction.ThemeChange(
                    theme = AppTheme.DARK,
                ),
            )
        }
    }

    @Test
    fun `on theme selection dialog cancel click should dismiss dialog`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Default (System). Theme")
            .performScrollTo()
            .performClick()
        composeTestRule
            .onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.assertNoDialogExists()
    }

    @Test
    fun `on show website icons row click should send ShowWebsiteIconsToggled`() {
        composeTestRule.onNodeWithText("Show website icons")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsToggle(true)) }
    }

    @Test
    fun `on show website icons tooltip click should send ShowWebsiteIconsToggled`() {
        composeTestRule
            .onNodeWithContentDescription(label = "Show website icons help")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(AppearanceAction.ShowWebsiteIconsTooltipClick)
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AppearanceEvent.NavigateBack)
        assertTrue(haveCalledNavigateBack)
    }

    @Test
    fun `on NavigateToWebsiteIconsHelp should call launchUri`() {
        mutableEventFlow.tryEmit(AppearanceEvent.NavigateToWebsiteIconsHelp)
        verify {
            intentManager.launchUri("https://bitwarden.com/help/website-icons/".toUri())
        }
    }

    @Test
    fun `use dynamic colors should be displayed based on state`() {
        composeTestRule.onNodeWithText("Use dynamic colors")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update {
            it.copy(isDynamicColorsSupported = false)
        }
        composeTestRule.onNodeWithText("Use dynamic colors")
            .assertIsNotDisplayed()
    }

    @Test
    fun `on DynamicColorsToggle should send DynamicColorsToggle`() {
        composeTestRule.onNodeWithText("Use dynamic colors")
            .performScrollTo()
            .performClick()
        verify { viewModel.trySendAction(AppearanceAction.DynamicColorsToggle(true)) }
    }

    @Test
    fun `on ConfirmEnableDynamicColorsClick should send ConfirmEnableDynamicColorsClick`() {
        mutableStateFlow.update {
            it.copy(dialogState = AppearanceState.DialogState.EnableDynamicColors)
        }
        composeTestRule.onAllNodesWithText(text = "Okay")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(AppearanceAction.ConfirmEnableDynamicColorsClick) }
    }

    @Test
    fun `on DismissDialog should send DismissDialog`() {
        mutableStateFlow.update {
            it.copy(dialogState = AppearanceState.DialogState.EnableDynamicColors)
        }
        composeTestRule.onAllNodesWithText("Cancel")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        verify { viewModel.trySendAction(AppearanceAction.DismissDialog) }
    }
}

private val DEFAULT_STATE = AppearanceState(
    language = AppLanguage.DEFAULT,
    showWebsiteIcons = false,
    theme = AppTheme.DEFAULT,
    isDynamicColorsEnabled = false,
    isDynamicColorsSupported = true,
    dialogState = null,
)
