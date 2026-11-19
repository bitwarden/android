package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the [SetupCompleteScreen]
 */
@HiltViewModel
class SetupCompleteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel<SetupCompleteState, Unit, SetupCompleteAction>(
    initialState = SetupCompleteState,
) {
    override fun handleAction(action: SetupCompleteAction) {
        when (action) {
            is SetupCompleteAction.CompleteSetup -> handleCompleteSetup()
        }
    }

    private fun handleCompleteSetup() {
        authRepository.goToNextStep()
    }
}

/**
 * State for the [SetupCompleteScreen]
 */
data object SetupCompleteState

/**
 * Model user actions for the [SetupCompleteScreen]
 */
sealed class SetupCompleteAction {

    /**
     * The user has performed an action to confirm that they are done setting up their account.
     */
    data object CompleteSetup : SetupCompleteAction()
}
