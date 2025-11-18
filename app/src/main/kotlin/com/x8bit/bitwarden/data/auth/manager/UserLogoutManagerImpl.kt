package com.x8bit.bitwarden.data.auth.manager

import androidx.annotation.StringRes
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.toast.ToastManager
import com.bitwarden.core.data.repository.util.bufferedMutableSharedFlow
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.model.LogoutEvent
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.CredentialExchangeRegistryManager
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

/**
 * Primary implementation of [UserLogoutManager].
 */
@Suppress("LongParameterList")
class UserLogoutManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val generatorDiskSource: GeneratorDiskSource,
    private val passwordHistoryDiskSource: PasswordHistoryDiskSource,
    private val pushDiskSource: PushDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val toastManager: ToastManager,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val credentialExchangeRegistryManager: CredentialExchangeRegistryManager,
    dispatcherManager: DispatcherManager,
) : UserLogoutManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val mainScope = CoroutineScope(dispatcherManager.main)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private val mutableLogoutEventFlow: MutableSharedFlow<LogoutEvent> =
        bufferedMutableSharedFlow()
    override val logoutEventFlow: SharedFlow<LogoutEvent> = mutableLogoutEventFlow.asSharedFlow()

    override fun logout(userId: String, reason: LogoutReason) {
        authDiskSource.userState ?: return
        Timber.d("logout reason=$reason")
        val isExpired = reason == LogoutReason.SecurityStamp
        if (isExpired) {
            showToast(message = BitwardenString.login_expired)
        }

        val ableToSwitchToNewAccount = switchUserIfAvailable(
            currentUserId = userId,
            isExpired = isExpired,
            removeCurrentUserFromAccounts = true,
        )

        if (!ableToSwitchToNewAccount) {
            // Update the user information and log out.
            authDiskSource.userState = null
            // Unregister the application from CXP Export since there are no other accounts.
            ioScope.launch { credentialExchangeRegistryManager.unregister() }
        }

        clearData(userId = userId)
        mutableLogoutEventFlow.tryEmit(LogoutEvent(loggedOutUserId = userId))
    }

    override fun softLogout(userId: String, reason: LogoutReason) {
        Timber.d("softLogout reason=$reason")
        val isExpired = reason == LogoutReason.SecurityStamp
        if (isExpired) {
            showToast(message = BitwardenString.login_expired)
        }

        // Save any data that will still need to be retained after otherwise clearing all dat
        val vaultTimeoutInMinutes = settingsDiskSource.getVaultTimeoutInMinutes(userId = userId)
        val vaultTimeoutAction = settingsDiskSource.getVaultTimeoutAction(userId = userId)

        switchUserIfAvailable(
            currentUserId = userId,
            removeCurrentUserFromAccounts = false,
            isExpired = isExpired,
        )

        clearData(userId = userId)
        mutableLogoutEventFlow.tryEmit(LogoutEvent(loggedOutUserId = userId))

        // Restore data that is still required
        settingsDiskSource.apply {
            storeVaultTimeoutInMinutes(
                userId = userId,
                vaultTimeoutInMinutes = vaultTimeoutInMinutes,
            )
            storeVaultTimeoutAction(
                userId = userId,
                vaultTimeoutAction = vaultTimeoutAction,
            )
        }
    }

    private fun clearData(userId: String) {
        vaultSdkSource.clearCrypto(userId = userId)
        authDiskSource.clearData(userId = userId)
        generatorDiskSource.clearData(userId = userId)
        pushDiskSource.clearData(userId = userId)
        settingsDiskSource.clearData(userId = userId)
        unconfinedScope.launch {
            passwordHistoryDiskSource.clearPasswordHistories(userId = userId)
            vaultDiskSource.deleteVaultData(userId = userId)
        }
    }

    private fun showToast(@StringRes message: Int) {
        mainScope.launch { toastManager.show(messageId = message) }
    }

    private fun switchUserIfAvailable(
        currentUserId: String,
        removeCurrentUserFromAccounts: Boolean,
        isExpired: Boolean = false,
    ): Boolean {
        val currentUserState = authDiskSource.userState ?: return false

        val currentAccountsMap = currentUserState.accounts

        // Remove the active user from the accounts map
        val updatedAccounts = currentAccountsMap
            .filterKeys { it != currentUserId }

        // Check if there is a new active user
        return if (updatedAccounts.isNotEmpty()) {
            if (currentUserId == currentUserState.activeUserId && !isExpired) {
                showToast(message = BitwardenString.account_switched_automatically)
            }

            // If we logged out a non-active user, we want to leave the active user unchanged.
            // If we logged out the active user, we want to set the active user to the first one
            // in the list.
            val updatedActiveUserId = currentUserState
                .activeUserId
                .takeUnless { it == currentUserId }
                ?: updatedAccounts.entries.first().key

            // Update the user information and emit an updated token
            authDiskSource.userState = currentUserState.copy(
                activeUserId = updatedActiveUserId,
                accounts = if (removeCurrentUserFromAccounts) {
                    updatedAccounts
                } else {
                    currentAccountsMap
                },
            )
            true
        } else {
            false
        }
    }
}
