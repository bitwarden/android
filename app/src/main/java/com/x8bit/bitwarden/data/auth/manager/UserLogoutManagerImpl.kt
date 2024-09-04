package com.x8bit.bitwarden.data.auth.manager

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.model.LogoutEvent
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.util.bufferedMutableSharedFlow
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.GeneratorDiskSource
import com.x8bit.bitwarden.data.tools.generator.datasource.disk.PasswordHistoryDiskSource
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * Primary implementation of [UserLogoutManager].
 */
@Suppress("LongParameterList")
class UserLogoutManagerImpl(
    private val context: Context,
    private val authDiskSource: AuthDiskSource,
    private val generatorDiskSource: GeneratorDiskSource,
    private val passwordHistoryDiskSource: PasswordHistoryDiskSource,
    private val pushDiskSource: PushDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    dispatcherManager: DispatcherManager,
    private val vaultSdkSource: VaultSdkSource,
) : UserLogoutManager {
    private val scope = CoroutineScope(dispatcherManager.unconfined)
    private val mainScope = CoroutineScope(dispatcherManager.main)

    private val mutableLogoutEventFlow: MutableSharedFlow<LogoutEvent> =
        bufferedMutableSharedFlow()
    override val logoutEventFlow: SharedFlow<LogoutEvent> = mutableLogoutEventFlow.asSharedFlow()

    override fun logout(userId: String, isExpired: Boolean) {
        authDiskSource.userState ?: return

        if (isExpired) {
            showToast(message = R.string.login_expired)
        }

        val ableToSwitchToNewAccount = switchUserIfAvailable(
            currentUserId = userId,
            isExpired = isExpired,
            removeCurrentUserFromAccounts = true,
        )

        if (!ableToSwitchToNewAccount) {
            // Update the user information and log out
            authDiskSource.userState = null
        }

        clearData(userId = userId)
        mutableLogoutEventFlow.tryEmit(LogoutEvent(loggedOutUserId = userId))
    }

    override fun softLogout(userId: String, isExpired: Boolean) {
        if (isExpired) {
            showToast(message = R.string.login_expired)
        }
        authDiskSource.storeAccountTokens(
            userId = userId,
            accountTokens = null,
        )

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
        scope.launch {
            passwordHistoryDiskSource.clearPasswordHistories(userId = userId)
            vaultDiskSource.deleteVaultData(userId = userId)
        }
    }

    private fun showToast(@StringRes message: Int) {
        mainScope.launch { Toast.makeText(context, message, Toast.LENGTH_SHORT).show() }
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
                showToast(message = R.string.account_switched_automatically)
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
