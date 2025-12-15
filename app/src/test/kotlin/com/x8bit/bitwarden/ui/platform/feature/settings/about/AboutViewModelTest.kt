package com.x8bit.bitwarden.ui.platform.feature.settings.about

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.datasource.disk.model.ServerConfig
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.ui.platform.manager.util.deviceData
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.about.util.getStopsLoggingStringForActiveLog
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.time.ZoneOffset

class AboutViewModelTest : BaseViewModelTest() {

    private val environmentRepository = FakeEnvironmentRepository()
    private val clipboardManager: BitwardenClipboardManager = mockk()
    private val logsManager: LogsManager = mockk {
        every { isEnabled } returns false
        every { isEnabled = any() } just runs
    }
    private val mutableFlightRecorderFlow = MutableStateFlow(FlightRecorderDataSet(emptySet()))
    private val settingsRepository = mockk<SettingsRepository> {
        every { flightRecorderDataFlow } returns mutableFlightRecorderFlow
        every { flightRecorderData } returns FlightRecorderDataSet(emptySet())
    }
    private val mutableServerConfigStateFlow = MutableStateFlow<ServerConfig?>(null)
    private val serverConfigRepository: ServerConfigRepository = mockk {
        every { serverConfigStateFlow } returns mutableServerConfigStateFlow
    }
    private val buildInfoManager: BuildInfoManager = mockk {
        every { buildTypeName } returns "mockBuildType"
        every { versionData } returns "mockVersionData"
        every { sdkData } returns "mockSdkData"
        every { deviceData } returns "mockDeviceData"
        every { ciBuildInfo } returns "mockCiBuildInfo"
        every { isFdroid } returns false
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(FlightRecorderDataSet::getStopsLoggingStringForActiveLog)
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
    fun `on FlightRecorderCheckedChange with isEnabled false should end the logging`() {
        every { settingsRepository.endFlightRecorder() } just runs
        val viewModel = createViewModel()
        viewModel.trySendAction(AboutAction.FlightRecorderCheckedChange(isEnabled = false))
        verify(exactly = 1) {
            settingsRepository.endFlightRecorder()
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
    fun `on FlightRecorderDataReceive should update state appropriately`() = runTest {
        val viewModel = createViewModel(DEFAULT_ABOUT_STATE)
        mockkStatic(FlightRecorderDataSet::getStopsLoggingStringForActiveLog)
        val activeData = mockk<FlightRecorderDataSet> {
            every {
                getStopsLoggingStringForActiveLog(FIXED_CLOCK)
            } returns "Stops at 10 PM".asText()
            every { hasActiveFlightRecorderData } returns true
        }
        val inactiveData = mockk<FlightRecorderDataSet> {
            every { getStopsLoggingStringForActiveLog(FIXED_CLOCK) } returns null
            every { hasActiveFlightRecorderData } returns false
        }

        viewModel.stateFlow.test {
            assertEquals(DEFAULT_ABOUT_STATE, awaitItem())

            mutableFlightRecorderFlow.value = activeData
            assertEquals(
                DEFAULT_ABOUT_STATE.copy(
                    isFlightRecorderEnabled = true,
                    flightRecorderSubtext = "Stops at 10 PM".asText(),
                ),
                awaitItem(),
            )

            mutableFlightRecorderFlow.value = inactiveData
            assertEquals(DEFAULT_ABOUT_STATE, awaitItem())
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
            .concat("\n".asText())
            .concat(state.sdkVersion)
            .concat("\n".asText())
            .concat(state.serverData)

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
        clock = FIXED_CLOCK,
        environmentRepository = environmentRepository,
        logsManager = logsManager,
        settingsRepository = settingsRepository,
        serverConfigRepository = serverConfigRepository,
        buildInfoManager = buildInfoManager,
    )
}

private val FIXED_CLOCK = Clock.fixed(
    Instant.parse("2024-01-25T10:15:30.00Z"),
    ZoneOffset.UTC,
)
private val DEFAULT_ABOUT_STATE: AboutState = AboutState(
    version = "Version: <version_name> (<version_code>)".asText(),
    sdkVersion = "\uD83E\uDD80 SDK: 1.0.0-20250708.105256-238".asText(),
    serverData = "\uD83C\uDF29 Server: 2025.7.1".asText(),
    deviceData = "<device_data>".asText(),
    ciData = "<ci_info>".asText(),
    copyrightInfo = "© Bitwarden Inc. 2015-${Year.now(FIXED_CLOCK).value}".asText(),
    isSubmitCrashLogsEnabled = false,
    shouldShowCrashLogsButton = true,
    isFlightRecorderEnabled = false,
    flightRecorderSubtext = null,
)
