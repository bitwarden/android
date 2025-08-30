package com.x8bit.bitwarden.ui.platform.feature.debugmenu

import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.LogsManager
import com.x8bit.bitwarden.data.platform.repository.DebugMenuRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.collections.immutable.toImmutableMap
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the [DebugMenuScreen]
 */
@HiltViewModel
class DebugMenuViewModel @Inject constructor(
    featureFlagManager: FeatureFlagManager,
    private val debugMenuRepository: DebugMenuRepository,
    private val authRepository: AuthRepository,
    private val logsManager: LogsManager,
) : BaseViewModel<DebugMenuState, DebugMenuEvent, DebugMenuAction>(
    initialState = DebugMenuState(featureFlags = persistentMapOf()),
) {

    private var featureFlagResetJob: Job? = null

    init {
        combine(
            flows = FlagKey.activePasswordManagerFlags.map { flagKey ->
                featureFlagManager.getFeatureFlagFlow(flagKey).map { flagKey to it }
            },
        ) { DebugMenuAction.Internal.UpdateFeatureFlagMap(it.toMap()) }
            .onEach(::sendAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: DebugMenuAction) {
        when (action) {
            is DebugMenuAction.UpdateFeatureFlag<*> -> handleUpdateFeatureFlag(action)
            is DebugMenuAction.Internal.UpdateFeatureFlagMap -> handleUpdateFeatureFlagMap(action)
            DebugMenuAction.NavigateBack -> handleNavigateBack()
            DebugMenuAction.ResetFeatureFlagValues -> handleResetFeatureFlagValues()
            DebugMenuAction.RestartOnboarding -> handleResetOnboardingStatus()
            DebugMenuAction.RestartOnboardingCarousel -> handleResetOnboardingCarousel()
            DebugMenuAction.ResetCoachMarkTourStatuses -> handleResetCoachMarkTourStatuses()
            DebugMenuAction.GenerateCrashClick -> handleCrashClick()
            DebugMenuAction.GenerateErrorReportClick -> handleErrorReportClick()
        }
    }

    private fun handleResetCoachMarkTourStatuses() {
        debugMenuRepository.resetCoachMarkTourStatuses()
    }

    private fun handleCrashClick(): Nothing {
        throw IllegalStateException("User has clicked the generate crash button")
    }

    private fun handleErrorReportClick() {
        logsManager.trackNonFatalException(
            throwable = IllegalStateException("User has clicked the generate error report button"),
        )
    }

    private fun handleResetOnboardingCarousel() {
        debugMenuRepository.modifyStateToShowOnboardingCarousel(
            userStateUpdateTrigger = {
                authRepository.hasPendingAccountAddition = true
            },
        )
    }

    private fun handleResetOnboardingStatus() {
        debugMenuRepository.resetOnboardingStatusForCurrentUser()
    }

    private fun handleResetFeatureFlagValues() {
        featureFlagResetJob?.cancel()
        featureFlagResetJob = viewModelScope.launch {
            debugMenuRepository.resetFeatureFlagOverrides()
        }
    }

    private fun handleNavigateBack() {
        sendEvent(DebugMenuEvent.NavigateBack)
    }

    private fun handleUpdateFeatureFlagMap(action: DebugMenuAction.Internal.UpdateFeatureFlagMap) {
        mutableStateFlow.update {
            it.copy(featureFlags = action.newMap.toImmutableMap())
        }
    }

    private fun handleUpdateFeatureFlag(action: DebugMenuAction.UpdateFeatureFlag<*>) {
        debugMenuRepository.updateFeatureFlag(action.flagKey, action.newValue)
    }
}

/**
 * State for the [DebugMenuViewModel]
 */
data class DebugMenuState(
    val featureFlags: ImmutableMap<FlagKey<Any>, Any>,
)

/**
 * Models event for the [DebugMenuViewModel] to send to the UI.
 */
sealed class DebugMenuEvent {
    /**
     * Navigates back to previous screen.
     */
    data object NavigateBack : DebugMenuEvent()
}

/**
 * Models action for the [DebugMenuViewModel] to handle.
 */
sealed class DebugMenuAction {

    /**
     * Updates a feature flag for the given [FlagKey] to the given [newValue].
     */
    data class UpdateFeatureFlag<T : Any>(
        val flagKey: FlagKey<T>,
        val newValue: T,
    ) : DebugMenuAction()

    /**
     * The user has clicked "back" button.
     */
    data object NavigateBack : DebugMenuAction()

    /**
     * The user has clicked "reset" button for the feature flag section.
     */
    data object ResetFeatureFlagValues : DebugMenuAction()

    /**
     * The user has clicked the restart onboarding button for the onboarding section.
     */
    data object RestartOnboarding : DebugMenuAction()

    /**
     * The user has clicked the restart onboarding button for the onboarding section.
     */
    data object RestartOnboardingCarousel : DebugMenuAction()

    /**
     * User has clicked to reset coach mark values.
     */
    data object ResetCoachMarkTourStatuses : DebugMenuAction()

    /**
     * The user has clicked generate crash button.
     */
    data object GenerateCrashClick : DebugMenuAction()

    /**
     * The user has clicked generate error report button.
     */
    data object GenerateErrorReportClick : DebugMenuAction()

    /**
     * Internal actions not triggered from the UI.
     */
    sealed class Internal : DebugMenuAction() {
        /**
         * Update the feature flag map with the new value.
         */
        data class UpdateFeatureFlagMap(val newMap: Map<FlagKey<Any>, Any>) : Internal()
    }
}
