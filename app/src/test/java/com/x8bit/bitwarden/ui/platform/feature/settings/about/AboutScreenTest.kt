package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
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
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.time.ZoneOffset

class AboutScreenTest : BaseComposeTest() {
    private var haveCalledNavigateBack = false

    private val mutableStateFlow = MutableStateFlow(
        AboutState(
            version = "Version: 1.0.0 (1)".asText(),
            isSubmitCrashLogsEnabled = false,
            copyrightInfo = "".asText(),
            shouldShowCrashLogsButton = true,
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
    fun `on give feedback click should display confirmation dialog and confirm click should emit GiveFeedbackClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Give Feedback").performScrollTo().performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify {
            viewModel.trySendAction(AboutAction.GiveFeedbackClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on bitwarden help center click should display confirmation dialog and confirm click should emit HelpCenterClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Bitwarden help center").performClick()
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
    fun `on privacy policy click should display confirmation dialog and confirm click should emit PrivacyPolicyClick`() {
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        composeTestRule.onNodeWithText("Privacy Policy").performClick()
        composeTestRule.onNode(isDialog()).assertExists()
        composeTestRule
            .onAllNodesWithText("Continue")
            .filterToOne(hasAnyAncestor(isDialog()))
            .performClick()
        composeTestRule.onNode(isDialog()).assertDoesNotExist()
        verify {
            viewModel.trySendAction(AboutAction.PrivacyPolicyClick)
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
    fun `on NavigateToPrivacyPolicy should call launchUri on IntentManager`() {
        mutableEventFlow.tryEmit(AboutEvent.NavigateToPrivacyPolicy)
        verify {
            intentManager.launchUri("https://bitwarden.com/privacy".toUri())
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
        val testUrl = "www.testUrl.com"
        mutableEventFlow.tryEmit(AboutEvent.NavigateToWebVault(testUrl))
        verify {
            intentManager.launchUri(testUrl.toUri())
        }
    }

    @Test
    fun `on NavigateToAboutSend should call launchUri on intentManager`() {
        mutableEventFlow.tryEmit(AboutEvent.NavigateToRateApp)
        verify {
            intentManager.launchUri(
                "https://play.google.com/store/apps/details?id=com.x8bit.bitwarden".toUri(),
            )
        }
    }

    @Test
    fun `submit crash logs switch should be displayed according to state`() {
        mutableStateFlow.update { it.copy(shouldShowCrashLogsButton = true) }

        composeTestRule
            .onNodeWithText("Submit crash logs")
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(shouldShowCrashLogsButton = false) }

        composeTestRule
            .onNodeWithText("Submit crash logs")
            .assertIsNotDisplayed()
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
        composeTestRule.onNodeWithText("Version: 1.0.0 (1)")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(AboutAction.VersionClick)
        }
    }

    @Test
    fun `version should update according to the state`() = runTest {
        composeTestRule.onNodeWithText("Version: 1.0.0 (1)")
            .performScrollTo()
            .assertIsDisplayed()

        mutableStateFlow.update { it.copy(version = "Version: 1.1.0 (2)".asText()) }

        composeTestRule.onNodeWithText("Version: 1.1.0 (2)").assertIsDisplayed()
    }

    @Test
    fun `copyright info should update according to the state`() = runTest {
        val fixedClock = Clock.fixed(Instant.parse("2024-01-25T00:00:00Z"), ZoneOffset.UTC)
        val currentYear = Year.now(fixedClock).value

        mutableStateFlow.update {
            it.copy(copyrightInfo = "© Bitwarden Inc. 2015-$currentYear".asText())
        }

        composeTestRule.onNodeWithText("© Bitwarden Inc. 2015-$currentYear")
            .performScrollTo()
            .assertIsDisplayed()
    }
}
