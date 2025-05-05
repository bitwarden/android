package com.bitwarden.authenticator.ui.platform.feature.debugmenu

import androidx.lifecycle.viewModelScope
import com.bitwarden.authenticator.data.platform.manager.FeatureFlagManager
import com.bitwarden.authenticator.data.platform.manager.model.FlagKey
import com.bitwarden.authenticator.data.platform.repository.DebugMenuRepository
import com.bitwarden.authenticator.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
) : BaseViewModel<DebugMenuState, DebugMenuEvent, DebugMenuAction>(
    initialState = DebugMenuState(featureFlags = emptyMap()),
) {

    private var featureFlagResetJob: Job? = null

    init {
        combine(
            flows = FlagKey.activeFlags.map { flagKey ->
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
        }
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
            it.copy(featureFlags = action.newMap)
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
    val featureFlags: Map<FlagKey<Any>, Any>,
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
    data class UpdateFeatureFlag<T : Any>(val flagKey: FlagKey<T>, val newValue: T) :
        DebugMenuAction()

    /**
     * The user has clicked "back" button.
     */
    data object NavigateBack : DebugMenuAction()

    /**
     * The user has clicked "reset" button for the feature flag section.
     */
    data object ResetFeatureFlagValues : DebugMenuAction()

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
