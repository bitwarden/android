package com.x8bit.bitwarden.ui.platform.feature.settings.about

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.data.datasource.disk.model.FlightRecorderDataSet
import com.bitwarden.data.repository.ServerConfigRepository
import com.bitwarden.data.repository.util.baseWebVaultUrlOrDefault
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.manager.util.deviceData
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.bitwarden.ui.util.concat
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.ui.platform.feature.settings.about.util.getStopsLoggingStringForActiveLog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.Year
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the about screen.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class AboutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val clipboardManager: BitwardenClipboardManager,
    private val clock: Clock,
    private val logsManager: LogsManager,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    buildInfoManager: BuildInfoManager,
    serverConfigRepository: ServerConfigRepository,
) : BaseViewModel<AboutState, AboutEvent, AboutAction>(
    initialState = savedStateHandle[KEY_STATE] ?: run {
        val serverData = serverConfigRepository.serverConfigStateFlow.value?.serverData
        AboutState(
            version = "Version: ${buildInfoManager.versionData}".asText(),
            sdkVersion = "\uD83E\uDD80 SDK: ${buildInfoManager.sdkData}".asText(),
            serverData = StringBuilder()
                .append("\uD83C\uDF29 Server:")
                .apply {
                    serverData?.server?.name?.let { append(" $it") }
                    serverData?.version?.let { append(" $it") }
                    serverData?.environment?.cloudRegion?.let { append(" @ $it") }
                }
                .toString()
                .asText(),
            deviceData = buildInfoManager.deviceData.asText(),
            ciData = buildInfoManager.ciBuildInfo?.let { "\n$it" }.orEmpty().asText(),
            isSubmitCrashLogsEnabled = logsManager.isEnabled,
            shouldShowCrashLogsButton = !buildInfoManager.isFdroid,
            isFlightRecorderEnabled = settingsRepository
                .flightRecorderData
                .hasActiveFlightRecorderData,
            flightRecorderSubtext = settingsRepository
                .flightRecorderData
                .getStopsLoggingStringForActiveLog(clock = clock),
            copyrightInfo = "© Bitwarden Inc. 2015-${Year.now(clock).value}".asText(),
        )
    },
) {
    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        settingsRepository
            .flightRecorderDataFlow
            .map { AboutAction.Internal.FlightRecorderDataReceive(data = it) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AboutAction): Unit = when (action) {
        AboutAction.BackClick -> handleBackClick()
        AboutAction.HelpCenterClick -> handleHelpCenterClick()
        AboutAction.PrivacyPolicyClick -> handlePrivacyPolicyClick()
        AboutAction.LearnAboutOrganizationsClick -> handleLearnAboutOrganizationsClick()
        is AboutAction.SubmitCrashLogsClick -> handleSubmitCrashLogsClick(action)
        AboutAction.VersionClick -> handleVersionClick()
        AboutAction.WebVaultClick -> handleWebVaultClick()
        is AboutAction.FlightRecorderCheckedChange -> handleFlightRecorderCheckedChange(action)
        AboutAction.FlightRecorderTooltipClick -> handleFlightRecorderTooltipClick()
        AboutAction.ViewRecordedLogsClick -> handleViewRecordedLogsClick()
        is AboutAction.Internal -> handleInternalAction(action)
    }

    private fun handleInternalAction(action: AboutAction.Internal) {
        when (action) {
            is AboutAction.Internal.FlightRecorderDataReceive -> {
                handleFlightRecorderDataReceive(action)
            }
        }
    }

    private fun handleFlightRecorderDataReceive(
        action: AboutAction.Internal.FlightRecorderDataReceive,
    ) {
        mutableStateFlow.update {
            it.copy(
                flightRecorderSubtext = action
                    .data
                    .getStopsLoggingStringForActiveLog(clock = clock),
                isFlightRecorderEnabled = action.data.hasActiveFlightRecorderData,
            )
        }
    }

    private fun handleBackClick() {
        sendEvent(AboutEvent.NavigateBack)
    }

    private fun handleHelpCenterClick() {
        sendEvent(AboutEvent.NavigateToHelpCenter)
    }

    private fun handlePrivacyPolicyClick() {
        sendEvent(AboutEvent.NavigateToPrivacyPolicy)
    }

    private fun handleLearnAboutOrganizationsClick() {
        sendEvent(AboutEvent.NavigateToLearnAboutOrganizations)
    }

    private fun handleSubmitCrashLogsClick(action: AboutAction.SubmitCrashLogsClick) {
        logsManager.isEnabled = action.enabled
        mutableStateFlow.update { currentState ->
            currentState.copy(isSubmitCrashLogsEnabled = action.enabled)
        }
    }

    private fun handleVersionClick() {
        clipboardManager.setText(
            text = state.copyrightInfo
                .concat("\n\n".asText())
                .concat(state.version)
                .concat("\n".asText())
                .concat(state.deviceData)
                .concat(state.ciData)
                .concat("\n".asText())
                .concat(state.sdkVersion)
                .concat("\n".asText())
                .concat(state.serverData),
        )
    }

    private fun handleWebVaultClick() {
        sendEvent(
            AboutEvent.NavigateToWebVault(
                environmentRepository.environment.environmentUrlData.baseWebVaultUrlOrDefault,
            ),
        )
    }

    private fun handleFlightRecorderCheckedChange(action: AboutAction.FlightRecorderCheckedChange) {
        if (action.isEnabled) {
            sendEvent(AboutEvent.NavigateToFlightRecorder)
        } else {
            settingsRepository.endFlightRecorder()
        }
    }

    private fun handleFlightRecorderTooltipClick() {
        sendEvent(AboutEvent.NavigateToFlightRecorderHelp)
    }

    private fun handleViewRecordedLogsClick() {
        sendEvent(AboutEvent.NavigateToRecordedLogs)
    }
}

/**
 * Represents the state of the about screen.
 */
@Parcelize
data class AboutState(
    val version: Text,
    val sdkVersion: Text,
    val serverData: Text,
    val deviceData: Text,
    val ciData: Text,
    val isSubmitCrashLogsEnabled: Boolean,
    val shouldShowCrashLogsButton: Boolean,
    val isFlightRecorderEnabled: Boolean,
    val flightRecorderSubtext: Text?,
    val copyrightInfo: Text,
) : Parcelable

/**
 * Models events for the about screen.
 */
sealed class AboutEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AboutEvent()

    /**
     * Navigates to the flight recorder configuration.
     */
    data object NavigateToFlightRecorder : AboutEvent()

    /**
     * Navigates to the flight recorder help info.
     */
    data object NavigateToFlightRecorderHelp : AboutEvent()

    /**
     * Navigates to the flight recorder log history.
     */
    data object NavigateToRecordedLogs : AboutEvent()

    /**
     * Navigates to the help center.
     */
    data object NavigateToHelpCenter : AboutEvent()

    /**
     * Navigates to the private policy.
     */
    data object NavigateToPrivacyPolicy : AboutEvent()

    /**
     * Navigates to learn about organizations.
     */
    data object NavigateToLearnAboutOrganizations : AboutEvent()

    /**
     * Navigates to the web vault.
     */
    data class NavigateToWebVault(val vaultUrl: String) : AboutEvent()
}

/**
 * Models actions for the about screen.
 */
sealed class AboutAction {
    /**
     * User clicked back button.
     */
    data object BackClick : AboutAction()

    /**
     *  User clicked the helper center row.
     */
    data object HelpCenterClick : AboutAction()

    /**
     * User clicked the privacy policy row.
     */
    data object PrivacyPolicyClick : AboutAction()

    /**
     * User clicked the learn about organizations row.
     */
    data object LearnAboutOrganizationsClick : AboutAction()

    /**
     * User clicked the submit crash logs toggle.
     */
    data class SubmitCrashLogsClick(
        val enabled: Boolean,
    ) : AboutAction()

    /**
     * User clicked the version row.
     */
    data object VersionClick : AboutAction()

    /**
     * User clicked the web vault row.
     */
    data object WebVaultClick : AboutAction()

    /**
     * User clicked the flight recorder check box.
     */
    data class FlightRecorderCheckedChange(
        val isEnabled: Boolean,
    ) : AboutAction()

    /**
     * User clicked the flight recorder tooltip.
     */
    data object FlightRecorderTooltipClick : AboutAction()

    /**
     * User clicked the view recorded logs row.
     */
    data object ViewRecordedLogsClick : AboutAction()

    /**
     * Actions for internal use by the ViewModel.
     */
    sealed class Internal : AboutAction() {
        /**
         * Indicates that the flight recorder data has changed.
         */
        data class FlightRecorderDataReceive(val data: FlightRecorderDataSet) : Internal()
    }
}
