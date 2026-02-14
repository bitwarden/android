package com.x8bit.bitwarden.ui.platform.feature.cookieacquisition

import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.core.net.toUri
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.base.BitwardenComposeTest
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.junit.Before
import org.junit.Test

class CookieAcquisitionScreenTest : BitwardenComposeTest() {

    private val mutableEventFlow =
        bufferedMutableSharedFlow<CookieAcquisitionEvent>()
    private val mutableStateFlow = MutableStateFlow(DEFAULT_STATE)
    private val viewModel = mockk<CookieAcquisitionViewModel>(relaxed = true) {
        every { eventFlow } returns mutableEventFlow
        every { stateFlow } returns mutableStateFlow
    }
    private val intentManager = mockk<IntentManager> {
        every { startCustomTabsActivity(any()) } just runs
        every { launchUri(any()) } just runs
    }

    @Before
    fun setUp() {
        setContent(
            intentManager = intentManager,
        ) {
            CookieAcquisitionScreen(
                viewModel = viewModel,
            )
        }
    }

    @Test
    fun `LaunchBrowser event should call startCustomTabsActivity`() {
        val uri = "https://example.com"
        mutableEventFlow.tryEmit(CookieAcquisitionEvent.LaunchBrowser(uri = uri))
        verify {
            intentManager.startCustomTabsActivity(uri.toUri())
        }
    }

    @Test
    fun `NavigateToHelp event should call launchUri`() {
        val uri = "https://bitwarden.com/help"
        mutableEventFlow.tryEmit(CookieAcquisitionEvent.NavigateToHelp(uri = uri))
        verify {
            intentManager.launchUri(uri.toUri())
        }
    }

    @Test
    fun `title should be displayed`() {
        composeTestRule
            .onNodeWithText("Sync with browser")
            .assertExists()
    }

    @Test
    fun `body text should display environment URL`() {
        composeTestRule
            .onNodeWithText(
                text = DEFAULT_ENVIRONMENT_URL,
                substring = true,
            )
            .assertExists()
    }

    @Test
    fun `launch browser button click should send LaunchBrowserClick action`() {
        composeTestRule
            .onNodeWithText("Launch browser")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(CookieAcquisitionAction.LaunchBrowserClick)
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `continue without syncing button click should send ContinueWithoutSyncingClick action`() {
        composeTestRule
            .onNodeWithText("Continue without syncing")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(
                CookieAcquisitionAction.ContinueWithoutSyncingClick,
            )
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `why am I seeing this button click should send WhyAmISeeingThisClick action`() {
        composeTestRule
            .onNodeWithText("Why am I seeing this?")
            .performScrollTo()
            .performClick()
        verify {
            viewModel.trySendAction(
                CookieAcquisitionAction.WhyAmISeeingThisClick,
            )
        }
    }

    @Test
    fun `error dialog should not be displayed by default`() {
        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertDoesNotExist()
    }

    @Test
    fun `error dialog should be displayed when dialogState is Error`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = CookieAcquisitionDialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("An error has occurred")
            .filterToOne(hasAnyAncestor(isDialog()))
            .assertExists()
    }

    @Test
    fun `error dialog dismiss should send DismissDialogClick action`() {
        mutableStateFlow.update {
            it.copy(
                dialogState = CookieAcquisitionDialogState.Error(
                    title = BitwardenString.an_error_has_occurred.asText(),
                    message = BitwardenString.generic_error_message.asText(),
                ),
            )
        }

        composeTestRule
            .onAllNodesWithText("Okay")
            .filterToOne(
                hasAnyAncestor(isDialog()) and hasClickAction(),
            )
            .performClick()

        verify {
            viewModel.trySendAction(CookieAcquisitionAction.DismissDialogClick)
        }
    }
}

private const val DEFAULT_ENVIRONMENT_URL = "https://vault.bitwarden.com"

private val DEFAULT_STATE = CookieAcquisitionState(
    environmentUrl = DEFAULT_ENVIRONMENT_URL,
    dialogState = null,
)
