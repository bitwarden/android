package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.IntentHandler
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.toAnnotatedString
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

class AboutScreenTest : BaseComposeTest() {
    private val mutableStateFlow = MutableStateFlow(
        AboutState(
            version = "Version: 1.0.0 (1)".asText(),
            isSubmitCrashLogsEnabled = false,
        ),
    )

    @Test
    fun `on back click should send BackClick`() {
        val viewModel: AboutViewModel = mockk {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.BackClick) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AboutAction.BackClick) }
    }

    @Test
    fun `on bitwarden help center click should send HelpCenterClick`() {
        val viewModel: AboutViewModel = mockk {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.HelpCenterClick) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Bitwarden Help Center").performClick()
        verify {
            viewModel.trySendAction(AboutAction.HelpCenterClick)
        }
    }

    @Test
    fun `on bitwarden web vault click should send WebVaultClick`() {
        val viewModel: AboutViewModel = mockk {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.WebVaultClick) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Bitwarden web vault").performClick()
        verify {
            viewModel.trySendAction(AboutAction.WebVaultClick)
        }
    }

    @Test
    fun `on CopyToClipboard should call setText on ClipboardManager`() {
        val text = "copy text"
        val clipboardManager = mockk<ClipboardManager> {
            every { setText(any()) } just Runs
        }
        val viewModel = mockk<AboutViewModel> {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns flowOf(AboutEvent.CopyToClipboard(text.asText()))
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
                clipboardManager = clipboardManager,
            )
        }
        verify {
            clipboardManager.setText(text.toAnnotatedString())
        }
    }

    @Test
    fun `on learn about organizations click should send LearnAboutOrganizationsClick`() {
        val viewModel: AboutViewModel = mockk {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.LearnAboutOrganizationsClick) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Learn about organizations").performClick()
        verify {
            viewModel.trySendAction(AboutAction.LearnAboutOrganizationsClick)
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        var haveCalledNavigateBack = false
        val viewModel = mockk<AboutViewModel> {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns flowOf(AboutEvent.NavigateBack)
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
        assertTrue(haveCalledNavigateBack)
    }

    @Test
    fun `on NavigateToHelpCenter should call launchUri on IntentHandler`() {
        val intentHandler = mockk<IntentHandler> {
            every { launchUri(any()) } just Runs
        }
        val viewModel = mockk<AboutViewModel> {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns flowOf(AboutEvent.NavigateToHelpCenter)
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
                intentHandler = intentHandler,
            )
        }
        verify {
            intentHandler.launchUri("https://bitwarden.com/help".toUri())
        }
    }

    @Test
    fun `on NavigateToLearnAboutOrganizations should call launchUri on IntentHandler`() {
        val intentHandler = mockk<IntentHandler> {
            every { launchUri(any()) } just Runs
        }
        val viewModel = mockk<AboutViewModel> {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns flowOf(AboutEvent.NavigateToLearnAboutOrganizations)
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
                intentHandler = intentHandler,
            )
        }
        verify {
            intentHandler.launchUri("https://bitwarden.com/help/about-organizations".toUri())
        }
    }

    @Test
    fun `on NavigateToWebVault should call launchUri on IntentHandler`() {
        val intentHandler = mockk<IntentHandler> {
            every { launchUri(any()) } just Runs
        }
        val viewModel = mockk<AboutViewModel> {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns flowOf(AboutEvent.NavigateToWebVault)
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
                intentHandler = intentHandler,
            )
        }
        verify {
            intentHandler.launchUri("https://vault.bitwarden.com".toUri())
        }
    }

    @Test
    fun `on rate the app click should send RateAppClick`() {
        val viewModel: AboutViewModel = mockk {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.RateAppClick) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Rate the app").performClick()
        verify {
            viewModel.trySendAction(AboutAction.RateAppClick)
        }
    }

    @Test
    fun `on submit crash logs toggle should send SubmitCrashLogsClick`() {
        val enabled = true
        val viewModel: AboutViewModel = mockk {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.SubmitCrashLogsClick(enabled)) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Submit crash logs").performClick()
        verify {
            viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(enabled))
        }
    }

    fun `on submit crash logs should be toggled on or off according to the state`() {
        val viewModel = mockk<AboutViewModel>(relaxed = true) {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns mutableStateFlow
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Submit crash logs").assertIsOff()
        mutableStateFlow.update { it.copy(isSubmitCrashLogsEnabled = true) }
        composeTestRule.onNodeWithText("Submit crash logs").assertIsOn()
    }

    @Test
    fun `on version info click should send VersionClick`() {
        val viewModel: AboutViewModel = mockk {
            every { stateFlow } returns mutableStateFlow
            every { eventFlow } returns emptyFlow()
            every { trySendAction(AboutAction.VersionClick) } returns Unit
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Version: 1.0.0 (1)").performClick()
        verify {
            viewModel.trySendAction(AboutAction.VersionClick)
        }
    }

    @Test
    fun `version should update according to the state`() = runTest {
        val viewModel = mockk<AboutViewModel> {
            every { eventFlow } returns emptyFlow()
            every { stateFlow } returns mutableStateFlow
        }
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                onNavigateBack = { },
            )
        }
        composeTestRule.onNodeWithText("Version: 1.0.0 (1)").assertIsDisplayed()

        mutableStateFlow.update { it.copy(version = "Version: 1.1.0 (2)".asText()) }

        composeTestRule.onNodeWithText("Version: 1.1.0 (2)").assertIsDisplayed()
    }
}
