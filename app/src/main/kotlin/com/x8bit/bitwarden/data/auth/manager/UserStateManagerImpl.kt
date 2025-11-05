package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.auth.repository.util.currentOnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.util.onboardingStatusChangesFlow
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.auth.repository.util.userAccountTokens
import com.x8bit.bitwarden.data.auth.repository.util.userAccountTokensFlow
import com.x8bit.bitwarden.data.auth.repository.util.userKeyConnectorStateFlow
import com.x8bit.bitwarden.data.auth.repository.util.userKeyConnectorStateList
import com.x8bit.bitwarden.data.auth.repository.util.userOrganizationsList
import com.x8bit.bitwarden.data.auth.repository.util.userOrganizationsListFlow
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * The default implementation of the [UserStateManager].
 */
class UserStateManagerImpl(
    private val authDiskSource: AuthDiskSource,
    firstTimeActionManager: FirstTimeActionManager,
    vaultLockManager: VaultLockManager,
    private val policyManager: PolicyManager,
    dispatcherManager: DispatcherManager,
) : UserStateManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    //region Pending Account Addition
    private val mutableHasPendingAccountAdditionStateFlow = MutableStateFlow(value = false)

    override val hasPendingAccountAdditionStateFlow: StateFlow<Boolean>
        get() = mutableHasPendingAccountAdditionStateFlow

    override var hasPendingAccountAddition: Boolean
        by mutableHasPendingAccountAdditionStateFlow::value
    //endregion Pending Account Addition

    //region Pending Account Deletion
    /**
     * If there is a pending account deletion, continue showing the original UserState until it
     * is confirmed. This is accomplished by blocking the emissions of the [userStateFlow]
     * whenever set to `true`.
     */
    private val mutableHasPendingAccountDeletionStateFlow = MutableStateFlow(value = false)

    override var hasPendingAccountDeletion: Boolean
        by mutableHasPendingAccountDeletionStateFlow::value
    //endregion Pending Account Deletion

    /**
     * Whenever a function needs to update multiple underlying data-points that contribute to the
     * [UserState], we update this [MutableStateFlow] and continue to show the original `UserState`
     * until the transaction is complete. This is accomplished by blocking the emissions of the
     * [userStateFlow] whenever this is set to a value above 0 (a count is used if more than one
     * process is updating data simultaneously).
     */
    private val mutableUserStateTransactionCountStateFlow = MutableStateFlow(0)

    @Suppress("UNCHECKED_CAST", "MagicNumber")
    override val userStateFlow: StateFlow<UserState?> = combine(
        authDiskSource.userStateFlow,
        authDiskSource.userAccountTokensFlow,
        authDiskSource.userOrganizationsListFlow,
        authDiskSource.userKeyConnectorStateFlow,
        authDiskSource.onboardingStatusChangesFlow,
        firstTimeActionManager.firstTimeStateFlow,
        vaultLockManager.vaultUnlockDataStateFlow,
        hasPendingAccountAdditionStateFlow,
        // Ignore the data in the merge, but trigger an update when they emit.
        merge(
            mutableHasPendingAccountDeletionStateFlow,
            mutableUserStateTransactionCountStateFlow,
            vaultLockManager.isActiveUserUnlockingFlow,
        ),
    ) { array ->
        val userStateJson = array[0] as UserStateJson?
        val userAccountTokens = array[1] as List<UserAccountTokens>
        val userOrganizationsList = array[2] as List<UserOrganizations>
        val userIsUsingKeyConnectorList = array[3] as List<UserKeyConnectorState>
        val onboardingStatus = array[4] as OnboardingStatus?
        val firstTimeState = array[5] as FirstTimeState
        val vaultState = array[6] as List<VaultUnlockData>
        val hasPendingAccountAddition = array[7] as Boolean
        userStateJson?.toUserState(
            vaultState = vaultState,
            userAccountTokens = userAccountTokens,
            userOrganizationsList = userOrganizationsList,
            userIsUsingKeyConnectorList = userIsUsingKeyConnectorList,
            hasPendingAccountAddition = hasPendingAccountAddition,
            onboardingStatus = onboardingStatus,
            isBiometricsEnabledProvider = ::isBiometricsEnabled,
            vaultUnlockTypeProvider = ::getVaultUnlockType,
            isDeviceTrustedProvider = ::isDeviceTrusted,
            firstTimeState = firstTimeState,
            getUserPolicies = ::existingPolicies,
        )
    }
        .filterNot {
            mutableHasPendingAccountDeletionStateFlow.value ||
                mutableUserStateTransactionCountStateFlow.value > 0 ||
                vaultLockManager.isActiveUserUnlockingFlow.value
        }
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = authDiskSource
                .userState
                ?.toUserState(
                    vaultState = vaultLockManager.vaultUnlockDataStateFlow.value,
                    userAccountTokens = authDiskSource.userAccountTokens,
                    userOrganizationsList = authDiskSource.userOrganizationsList,
                    userIsUsingKeyConnectorList = authDiskSource.userKeyConnectorStateList,
                    hasPendingAccountAddition = mutableHasPendingAccountAdditionStateFlow.value,
                    onboardingStatus = authDiskSource.currentOnboardingStatus,
                    isBiometricsEnabledProvider = ::isBiometricsEnabled,
                    vaultUnlockTypeProvider = ::getVaultUnlockType,
                    isDeviceTrustedProvider = ::isDeviceTrusted,
                    firstTimeState = firstTimeActionManager.currentOrDefaultUserFirstTimeState,
                    getUserPolicies = ::existingPolicies,
                ),
        )

    override suspend fun <T> userStateTransaction(block: suspend () -> T): T {
        mutableUserStateTransactionCountStateFlow.update { it.inc() }
        return try {
            block()
        } finally {
            mutableUserStateTransactionCountStateFlow.update { it.dec() }
        }
    }

    private fun isBiometricsEnabled(
        userId: String,
    ): Boolean = authDiskSource.getUserBiometricUnlockKey(userId = userId) != null

    private fun isDeviceTrusted(
        userId: String,
    ): Boolean = authDiskSource.getDeviceKey(userId = userId) != null

    private fun getVaultUnlockType(
        userId: String,
    ): VaultUnlockType = authDiskSource
        .getPinProtectedUserKeyEnvelope(userId = userId)
        ?.let { VaultUnlockType.PIN }
        ?: VaultUnlockType.MASTER_PASSWORD

    private fun existingPolicies(
        userId: String,
        policyType: PolicyTypeJson,
    ): List<SyncResponseJson.Policy> = policyManager.getUserPolicies(
        userId = userId,
        type = policyType,
    )
}
