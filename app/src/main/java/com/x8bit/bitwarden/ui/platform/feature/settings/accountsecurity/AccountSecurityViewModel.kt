package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model for the account security screen.
 */
@HiltViewModel
class AccountSecurityViewModel @Inject constructor(
    private val authRepository: AuthRepository,
) : BaseViewModel<Unit, AccountSecurityEvent, AccountSecurityAction>(
    initialState = Unit,
) {
    override fun handleAction(action: AccountSecurityAction): Unit = when (action) {
        AccountSecurityAction.LogoutClick -> authRepository.logout()
        AccountSecurityAction.BackClick -> sendEvent(AccountSecurityEvent.NavigateBack)
    }
}

/**
 * Models events for the account security screen.
 */
sealed class AccountSecurityEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : AccountSecurityEvent()
}

/**
 * Models actions for the account security screen.
 */
sealed class AccountSecurityAction {
    /**
     * User clicked back button.
     */
    data object BackClick : AccountSecurityAction()

    /**
     * User clicked log out.
     */
    data object LogoutClick : AccountSecurityAction()
}
