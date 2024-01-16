package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.ui.platform.base.BaseComposeTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.intent.IntentManager
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AboutScreenTest : BaseComposeTest() {
    private var haveCalledNavigateBack = false

    private val mutableStateFlow = MutableStateFlow(
        AboutState(
            version = "Version: 1.0.0 (1)".asText(),
            isSubmitCrashLogsEnabled = false,
        ),
    )
    private val mutableEventFlow = bufferedMutableSharedFlow<AboutEvent>()
    val viewModel: AboutViewModel = mockk {
        every { stateFlow } returns mutableStateFlow
        every { eventFlow } returns mutableEventFlow
        every { trySendAction(any()) } just runs
    }

    private val intentManager: IntentManager = mockk {
        every { launchUri(any()) } just runs
    }

    @Before
    fun setup() {
        composeTestRule.setContent {
            AboutScreen(
                viewModel = viewModel,
                intentManager = intentManager,
                onNavigateBack = { haveCalledNavigateBack = true },
            )
        }
    }

    @Test
    fun `on back click should send BackClick`() {
        composeTestRule.onNodeWithContentDescription("Back").performClick()
        verify { viewModel.trySendAction(AboutAction.BackClick) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on bitwarden help center click should display confirmation dialog and confirm click should emit HelpCenterClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Bitwarden Help Center").performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify {
            viewModel.trySendAction(AboutAction.HelpCenterClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on bitwarden web vault click should display confirmation dialog and confirm click should emit WebVaultClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Bitwarden web vault").performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify {
            viewModel.trySendAction(AboutAction.WebVaultClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on learn about organizations click should display confirmation dialog and confirm click should emit LearnAboutOrganizationsClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Learn about organizations").performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify {
            viewModel.trySendAction(AboutAction.LearnAboutOrganizationsClick)
        }
    }

    @Test
    fun `on NavigateBack should call onNavigateBack`() {
        mutableEventFlow.tryEmit(AboutEvent.NavigateBack)
        assertTrue(haveCalledNavigateBack)
    }

    @Test
    fun `on NavigateToHelpCenter should call launchUri on IntentManager`() {
        mutableEventFlow.tryEmit(AboutEvent.NavigateToHelpCenter)
        verify {
            intentManager.launchUri("https://bitwarden.com/help".toUri())
        }
    }

    @Test
    fun `on NavigateToLearnAboutOrganizations should call launchUri on IntentManager`() {
        mutableEventFlow.tryEmit(AboutEvent.NavigateToLearnAboutOrganizations)
        verify {
            intentManager.launchUri("https://bitwarden.com/help/about-organizations".toUri())
        }
    }

    @Test
    fun `on NavigateToWebVault should call launchUri on IntentManager`() {
        mutableEventFlow.tryEmit(AboutEvent.NavigateToWebVault)
        verify {
            intentManager.launchUri("https://vault.bitwarden.com".toUri())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on rate the app click should display confirmation dialog and confirm click should emit RateAppClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Rate the app").performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify {
            viewModel.trySendAction(AboutAction.RateAppClick)
        }
    }

    @Test
    fun `on submit crash logs toggle should send SubmitCrashLogsClick`() {
        val enabled = true
        composeTestRule.onNodeWithText("Submit crash logs").performClick()
        verify {
            viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(enabled))
        }
    }

    fun `on submit crash logs should be toggled on or off according to the state`() {
        composeTestRule.onNodeWithText("Submit crash logs").assertIsOff()
        mutableStateFlow.update { it.copy(isSubmitCrashLogsEnabled = true) }
        composeTestRule.onNodeWithText("Submit crash logs").assertIsOn()
    }

    @Test
    fun `on version info click should send VersionClick`() {
        composeTestRule.onNodeWithText("Version: 1.0.0 (1)").performClick()
        verify {
            viewModel.trySendAction(AboutAction.VersionClick)
        }
    }

    @Test
    fun `version should update according to the state`() = runTest {
        composeTestRule.onNodeWithText("Version: 1.0.0 (1)").assertIsDisplayed()

        mutableStateFlow.update { it.copy(version = "Version: 1.1.0 (2)".asText()) }

        composeTestRule.onNodeWithText("Version: 1.1.0 (2)").assertIsDisplayed()
    }
}
