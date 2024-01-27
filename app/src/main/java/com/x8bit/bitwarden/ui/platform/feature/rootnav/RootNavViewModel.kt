package com.x8bit.bitwarden.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash,
) {
    init {
        combine(
            authRepository
                .userStateFlow,
            specialCircumstanceManager
                .specialCircumstanceStateFlow,
        ) { userState, specialCircumstance ->
            RootNavAction.Internal.UserStateUpdateReceive(
                userState = userState,
                specialCircumstance = specialCircumstance,
            )
        }
            .onEach(::handleAction)
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            is RootNavAction.BackStackUpdate -> handleBackStackUpdate()
            is RootNavAction.Internal.UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
        }
    }

    private fun handleBackStackUpdate() {
        authRepository.updateLastActiveTime()
    }

    private fun handleUserStateUpdateReceive(
        action: RootNavAction.Internal.UserStateUpdateReceive,
    ) {
        val userState = action.userState
        val specialCircumstance = action.specialCircumstance
        val updatedRootNavState = when {
            userState == null ||
                !userState.activeAccount.isLoggedIn ||
                userState.hasPendingAccountAddition -> RootNavState.Auth

            userState.activeAccount.isVaultUnlocked -> {
                when (specialCircumstance) {
                    is SpecialCircumstance.AutofillSelection -> {
                        RootNavState.VaultUnlockedForAutofillSelection(
                            type = specialCircumstance.autofillSelectionData.type,
                        )
                    }

                    is SpecialCircumstance.ShareNewSend -> RootNavState.VaultUnlockedForNewSend

                    null -> {
                        RootNavState.VaultUnlocked(
                            activeUserId = userState.activeAccount.userId,
                        )
                    }
                }
            }

            else -> RootNavState.VaultLocked
        }
        mutableStateFlow.update { updatedRootNavState }
    }
}

/**
 * Models root level destinations for the app.
 */
sealed class RootNavState : Parcelable {
    /**
     * App should show auth nav graph.
     */
    @Parcelize
    data object Auth : RootNavState()

    /**
     * App should show splash nav graph.
     */
    @Parcelize
    data object Splash : RootNavState()

    /**
     * App should show vault locked nav graph.
     */
    @Parcelize
    data object VaultLocked : RootNavState()

    /**
     * App should show vault unlocked nav graph for the given [activeUserId].
     */
    @Parcelize
    data class VaultUnlocked(
        val activeUserId: String,
    ) : RootNavState()

    /**
     * App should show a selection screen for autofill for an unlocked user.
     */
    @Parcelize
    data class VaultUnlockedForAutofillSelection(
        val type: AutofillSelectionData.Type,
    ) : RootNavState()

    /**
     * App should show the new send screen for an unlocked user.
     */
    @Parcelize
    data object VaultUnlockedForNewSend : RootNavState()
}

/**
 * Models root level navigation actions.
 */
sealed class RootNavAction {

    /**
     * Indicates the backstack has changed.
     */
    data object BackStackUpdate : RootNavAction()

    /**
     * Internal ViewModel actions.
     */
    sealed class Internal {

        /**
         * User state in the repository layer changed.
         */
        data class UserStateUpdateReceive(
            val userState: UserState?,
            val specialCircumstance: SpecialCircumstance?,
        ) : RootNavAction()
    }
}
