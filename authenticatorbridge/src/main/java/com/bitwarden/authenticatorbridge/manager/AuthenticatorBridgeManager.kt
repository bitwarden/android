package com.bitwarden.authenticatorbridge.manager

import com.bitwarden.authenticatorbridge.IAuthenticatorBridgeService
import com.bitwarden.authenticatorbridge.manager.model.AccountSyncState
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides an API to make it simpler for consuming applications to
 * query [IAuthenticatorBridgeService].
 */
interface AuthenticatorBridgeManager {

    /**
     * State flow representing the current [AccountSyncState].
     */
    val accountSyncStateFlow: StateFlow<AccountSyncState>
}
