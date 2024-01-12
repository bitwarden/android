package com.x8bit.bitwarden.ui.platform.feature.rootnav

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val KEY_NAV_DESTINATION = "nav_state"

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    authRepository: AuthRepository,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = RootNavState.Splash,
) {
    init {
        authRepository
            .userStateFlow
            .onEach { sendAction(RootNavAction.Internal.UserStateUpdateReceive(it)) }
            .launchIn(viewModelScope)
    }

    override fun handleAction(action: RootNavAction) {
        when (action) {
            is RootNavAction.Internal.UserStateUpdateReceive -> handleUserStateUpdateReceive(action)
        }
    }

    private fun handleUserStateUpdateReceive(
        action: RootNavAction.Internal.UserStateUpdateReceive,
    ) {
        val userState = action.userState
        val updatedRootNavState = when {
            userState == null ||
                !userState.activeAccount.isLoggedIn ||
                userState.hasPendingAccountAddition -> RootNavState.Auth

            userState.activeAccount.isVaultUnlocked -> {
                RootNavState.VaultUnlocked(
                    activeUserId = userState.activeAccount.userId,
                )
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
}

/**
 * Models root level navigation actions.
 */
sealed class RootNavAction {

    /**
     * Internal ViewModel actions.
     */
    sealed class Internal {

        /**
         * User state in the repository layer changed.
         */
        data class UserStateUpdateReceive(val userState: UserState?) : RootNavAction()
    }
}
