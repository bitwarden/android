package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AboutViewModelTest : BaseViewModelTest() {

    private val initialState = createAboutState()
    private val initialSavedStateHandle = createSavedStateHandleWithState(initialState)

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = AboutViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.BackClick)
            assertEquals(AboutEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on HelpCenterClick should emit NavigateToHelpCenter`() = runTest {
        val viewModel = AboutViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.HelpCenterClick)
            assertEquals(AboutEvent.NavigateToHelpCenter, awaitItem())
        }
    }

    @Test
    fun `on LearnAboutOrganizationsClick should emit NavigateToLearnAboutOrganizations`() =
        runTest {
            val viewModel = AboutViewModel(initialSavedStateHandle)
            viewModel.eventFlow.test {
                viewModel.trySendAction(AboutAction.LearnAboutOrganizationsClick)
                assertEquals(AboutEvent.NavigateToLearnAboutOrganizations, awaitItem())
            }
        }

    @Test
    fun `on RateAppClick should emit ShowToast`() = runTest {
        val viewModel = AboutViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.RateAppClick)
            assertEquals(AboutEvent.ShowToast("Navigate to rate the app.".asText()), awaitItem())
        }
    }

    @Test
    fun `on SubmitCrashLogsClick should update isSubmitCrashLogsEnabled to true`() = runTest {
        val viewModel = AboutViewModel(initialSavedStateHandle)
        assertFalse(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
        viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(true))
        assertTrue(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
    }

    @Test
    fun `on VersionClick should emit CopyToClipboard`() = runTest {
        val viewModel = AboutViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.VersionClick)
            assertEquals(AboutEvent.CopyToClipboard("0".asText()), awaitItem())
        }
    }

    @Test
    fun `on WebVaultClick should emit NavigateToWebVault`() = runTest {
        val viewModel = AboutViewModel(initialSavedStateHandle)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.WebVaultClick)
            assertEquals(AboutEvent.NavigateToWebVault, awaitItem())
        }
    }

    private fun createAboutState(): AboutState =
        AboutState(
            version = "0".asText(),
            isSubmitCrashLogsEnabled = false,
        )

    private fun createSavedStateHandleWithState(state: AboutState) =
        SavedStateHandle().apply {
            set("state", state)
        }
}
