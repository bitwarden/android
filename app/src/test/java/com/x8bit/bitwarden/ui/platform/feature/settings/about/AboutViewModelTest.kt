package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.time.ZoneId

class AboutViewModelTest : BaseViewModelTest() {

    private val environmentRepository = FakeEnvironmentRepository()
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val logsManager: LogsManager = mockk {
        every { isEnabled } returns false
        every { isEnabled = any() } just runs
    }
    private val featureFlagManager = mockk<FeatureFlagManager> {
        every { getFeatureFlag(FlagKey.FlightRecorder) } returns true
        every { getFeatureFlagFlow(FlagKey.FlightRecorder) } returns flowOf(true)
    }

    @Test
    fun `on BackClick should emit NavigateBack`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.BackClick)
            assertEquals(AboutEvent.NavigateBack, awaitItem())
        }
    }

    @Test
    fun `on FlightRecorderTooltipClick should emit NavigateToFlightRecorderHelp`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.FlightRecorderTooltipClick)
            assertEquals(AboutEvent.NavigateToFlightRecorderHelp, awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on FlightRecorderCheckedChange with isEnabled true should emit NavigateToFlightRecorder`() =
        runTest {
            val viewModel = createViewModel()
            viewModel.eventFlow.test {
                viewModel.trySendAction(AboutAction.FlightRecorderCheckedChange(isEnabled = true))
                assertEquals(AboutEvent.NavigateToFlightRecorder, awaitItem())
            }
        }

    @Test
    fun `on FlightRecorderCheckedChange with isEnabled false should do nothing`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.FlightRecorderCheckedChange(isEnabled = false))
            expectNoEvents()
        }
    }

    @Test
    fun `on ViewRecordedLogsClick should emit NavigateToRecordedLogs`() = runTest {
        val viewModel = createViewModel()
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.ViewRecordedLogsClick)
            assertEquals(AboutEvent.NavigateToRecordedLogs, awaitItem())
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
    fun `on PrivacyPolicyClick should emit NavigateToPrivacyPolicy`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.PrivacyPolicyClick)
            assertEquals(AboutEvent.NavigateToPrivacyPolicy, awaitItem())
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
    fun `on SubmitCrashLogsClick should update isSubmitCrashLogsEnabled to true`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        assertFalse(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
        viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(true))
        assertTrue(viewModel.stateFlow.value.isSubmitCrashLogsEnabled)
    }

    @Test
    fun `on SubmitCrashLogsClick should update LogsManager isEnabled`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)

        viewModel.trySendAction(AboutAction.SubmitCrashLogsClick(true))

        coVerify(exactly = 1) { logsManager.isEnabled = true }
    }

    @Test
    fun `on VersionClick should call setText on the ClipboardManager with specific Text`() {
        val state = DEFAULT_ABOUT_STATE
        val expectedText = state.copyrightInfo
            .concat("\n\n".asText())
            .concat(state.version)
            .concat("\n".asText())
            .concat(state.deviceData)
            .concat(state.ciData)

        every { clipboardManager.setText(expectedText, true, null) } just runs

        val viewModel = createViewModel(state)
        viewModel.trySendAction(AboutAction.VersionClick)

        verify(exactly = 1) {
            clipboardManager.setText(expectedText, ofType(Boolean::class), isNull<String>())
        }
    }

    @Test
    fun `on WebVaultClick should emit NavigateToWebVault`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        viewModel.eventFlow.test {
            viewModel.trySendAction(AboutAction.WebVaultClick)
            assertEquals(
                AboutEvent.NavigateToWebVault(
                    vaultUrl = environmentRepository
                        .environment
                        .environmentUrlData
                        .baseWebVaultUrlOrDefault,
                ),
                awaitItem(),
            )
        }
    }

    private fun createViewModel(
        state: AboutState? = null,
    ): AboutViewModel = AboutViewModel(
        savedStateHandle = SavedStateHandle().apply { set("state", state) },
        clipboardManager = clipboardManager,
        clock = fixedClock,
        environmentRepository = environmentRepository,
        logsManager = logsManager,
        featureFlagManager = featureFlagManager,
    )
}

private val fixedClock = Clock.fixed(
    Instant.parse("2024-01-25T10:15:30.00Z"),
    ZoneId.systemDefault(),
)
private val DEFAULT_ABOUT_STATE: AboutState = AboutState(
    version = "Version: <version_name> (<version_code>)".asText(),
    deviceData = "<device_data>".asText(),
    ciData = "\n<ci_info>".asText(),
    copyrightInfo = "© Bitwarden Inc. 2015-${Year.now(fixedClock).value}".asText(),
    isSubmitCrashLogsEnabled = false,
    shouldShowCrashLogsButton = true,
    isFlightRecorderEnabled = false,
    shouldShowFlightRecorder = true,
)
