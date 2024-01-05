package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.x8bit.bitwarden.ui.platform.base.util.asText
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AboutViewModelTest : BaseViewModelTest() {

    private val clipboardManager: BitwardenClipboardManager = mockk()

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.BackClick)
            assertEquals(AboutEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on HelpCenterClick should emit NavigateToHelpCenter`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.HelpCenterClick)
            assertEquals(AboutEvent.NavigateToHelpCenter, awaitItem())
        }
    }

    @Test
    fun `on LearnAboutOrganizationsClick should emit NavigateToLearnAboutOrganizations`() =
        runTest {
            val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
            viewModel.eventFlow.test {
                viewModel.trySendAction(AboutAction.LearnAboutOrganizationsClick)
                assertEquals(AboutEvent.NavigateToLearnAboutOrganizations, awaitItem())
            }
        }

    @Test
    fun `on RateAppClick should emit ShowToast`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.RateAppClick)
            assertEquals(AboutEvent.ShowToast("Navigate to rate the app.".asText()), awaitItem())
        }
    }

    @Test
    fun `on SubmitCrashLogsClick should update isSubmitCrashLogsEnabled to true`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        assertFalse(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
        viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(true))
        assertTrue(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
    }

    @Test
    fun `on VersionClick should call setText on the ClipboardManager`() {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        every { clipboardManager.setText(text = "0".asText()) } just runs

        viewModel.trySendAction(AboutAction.VersionClick)

        verify(exactly = 1) {
            clipboardManager.setText(text = "0".asText())
        }
    }

    @Test
    fun `on WebVaultClick should emit NavigateToWebVault`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.WebVaultClick)
            assertEquals(AboutEvent.NavigateToWebVault, awaitItem())
        }
    }

    private fun createViewModel(
        state: AboutState? = null,
    ): AboutViewModel = AboutViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        clipboardManager = clipboardManager,
    )
}

private val DEFAULT_ABOUT_STATE: AboutState = AboutState(
    version = "0".asText(),
    isSubmitCrashLogsEnabled = false,
)
