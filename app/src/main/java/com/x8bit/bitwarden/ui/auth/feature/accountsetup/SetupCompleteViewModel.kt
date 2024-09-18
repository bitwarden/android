package com.x8bit.bitwarden.ui.auth.feature.accountsetup

import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for the [SetupCompleteScreen]
 */
@HiltViewModel
class SetupCompleteViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel<SetupCompleteState, Unit, SetupCompleteAction>(
    initialState = run {
        val userId = requireNotNull(authRepository.userStateFlow.value).activeUserId
        SetupCompleteState(userId = userId)
    },
) {
    override fun handleAction(action: SetupCompleteAction) {
        when (action) {
            is SetupCompleteAction.CompleteSetup -> handleCompleteSetup()
        }
    }

    private fun handleCompleteSetup() {
        authRepository.setOnboardingStatus(
            userId = state.userId,
            status = OnboardingStatus.COMPLETE,
        )
    }
}

/**
 * State for the [SetupCompleteScreen]
 */
data class SetupCompleteState(
    val userId: String,
)

/**
 * Model user actions for the [SetupCompleteScreen]
 */
sealed class SetupCompleteAction {

    /**
     * The user has performed an action to confirm that they are done setting up their account.
     */
    data object CompleteSetup : SetupCompleteAction()
}
