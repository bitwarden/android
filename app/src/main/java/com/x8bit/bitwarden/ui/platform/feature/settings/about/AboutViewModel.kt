package com.x8bit.bitwarden.ui.platform.feature.settings.about

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.manager.clipboard.BitwardenClipboardManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.util.baseWebVaultUrlOrDefault
import com.x8bit.bitwarden.data.platform.util.ciBuildInfo
import com.x8bit.bitwarden.data.platform.util.deviceData
import com.x8bit.bitwarden.data.platform.util.isFdroid
import com.x8bit.bitwarden.data.platform.util.versionData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.concat
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
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
@HiltViewModel
class AboutViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val clipboardManager: BitwardenClipboardManager,
    clock: Clock,
    private val logsManager: LogsManager,
    private val environmentRepository: EnvironmentRepository,
) : BaseViewModel<AboutState, AboutEvent, AboutAction>(
    initialState = savedStateHandle[KEY_STATE]
        ?: AboutState(
            version = R.string.version.asText().concat(": $versionData".asText()),
            deviceData = deviceData.asText(),
            ciData = ciBuildInfo?.let { "\n$it" }.orEmpty().asText(),
            isSubmitCrashLogsEnabled = logsManager.isEnabled,
            shouldShowCrashLogsButton = !isFdroid,
            copyrightInfo = "© Bitwarden Inc. 2015-${Year.now(clock).value}".asText(),
        ),
) {
    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: AboutAction): Unit = when (action) {
        AboutAction.BackClick -> handleBackClick()
        AboutAction.GiveFeedbackClick -> handleGiveFeedbackClick()
        AboutAction.HelpCenterClick -> handleHelpCenterClick()
        AboutAction.PrivacyPolicyClick -> handlePrivacyPolicyClick()
        AboutAction.LearnAboutOrganizationsClick -> handleLearnAboutOrganizationsClick()
        AboutAction.RateAppClick -> handleRateAppClick()
        is AboutAction.SubmitCrashLogsClick -> handleSubmitCrashLogsClick(action)
        AboutAction.VersionClick -> handleVersionClick()
        AboutAction.WebVaultClick -> handleWebVaultClick()
    }

    private fun handleBackClick() {
        sendEvent(AboutEvent.NavigateBack)
    }

    private fun handleHelpCenterClick() {
        sendEvent(AboutEvent.NavigateToHelpCenter)
    }

    private fun handleGiveFeedbackClick() {
        sendEvent(AboutEvent.NavigateToFeedbackForm)
    }

    private fun handlePrivacyPolicyClick() {
        sendEvent(AboutEvent.NavigateToPrivacyPolicy)
    }

    private fun handleLearnAboutOrganizationsClick() {
        sendEvent(AboutEvent.NavigateToLearnAboutOrganizations)
    }

    private fun handleRateAppClick() {
        sendEvent(AboutEvent.NavigateToRateApp)
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
                .concat(state.ciData),
        )
    }

    private fun handleWebVaultClick() {
        sendEvent(
            AboutEvent.NavigateToWebVault(
                environmentRepository.environment.environmentUrlData.baseWebVaultUrlOrDefault,
            ),
        )
    }
}

/**
 * Represents the state of the about screen.
 */
@Parcelize
data class AboutState(
    val version: Text,
    val deviceData: Text,
    val ciData: Text,
    val isSubmitCrashLogsEnabled: Boolean,
    val shouldShowCrashLogsButton: Boolean,
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
     * Navigate to the feedback form.
     */
    data object NavigateToFeedbackForm : AboutEvent()

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

    /**
     * Navigates to rate the app.
     */
    data object NavigateToRateApp : AboutEvent()
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
     * User clicked the give feedback row.
     */
    data object GiveFeedbackClick : AboutAction()

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
     * User clicked the rate the app row.
     */
    data object RateAppClick : AboutAction()

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
}
