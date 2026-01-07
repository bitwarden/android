package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.network.model.PolicyTypeJson
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndUnlocked
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update

/**
 * Default implementation of [VaultMigrationManager].
 *
 * Reactively observes vault cipher data and automatically updates migration state when:
 * - Vault is unlocked
 * - Sync has occurred at least once
 * - Cipher data changes
 */
@Suppress("LongParameterList")
class VaultMigrationManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val policyManager: PolicyManager,
    private val featureFlagManager: FeatureFlagManager,
    private val connectionManager: NetworkConnectionManager,
    vaultLockManager: VaultLockManager,
    dispatcherManager: DispatcherManager,
) : VaultMigrationManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    private val mutableVaultMigrationDataStateFlow =
        MutableStateFlow<VaultMigrationData>(value = VaultMigrationData.NoMigrationRequired)

    override val vaultMigrationDataStateFlow: StateFlow<VaultMigrationData>
        get() = mutableVaultMigrationDataStateFlow.asStateFlow()

    init {
        // Observe cipher data changes and automatically verify migration state
        mutableVaultMigrationDataStateFlow
            .observeWhenSubscribedAndUnlocked(
                userStateFlow = authDiskSource.userStateFlow,
                vaultUnlockFlow = vaultLockManager.vaultUnlockDataStateFlow,
            ) { activeUserId ->
                observeCipherDataAndUpdateMigrationState(userId = activeUserId)
            }
            .launchIn(unconfinedScope)
    }

    /**
     * Observes cipher data for the given user and updates migration state when changes occur.
     * Only emits updates after the user has synced at least once to ensure data freshness.
     *
     * Uses optimized [VaultDiskSource.hasPersonalCiphersFlow] query that checks only the
     * indexed organizationId column without loading full cipher JSON data.
     */
    private fun observeCipherDataAndUpdateMigrationState(userId: String) =
        vaultDiskSource
            .hasPersonalCiphersFlow(userId = userId)
            .filter {
                // Only process cipher data after sync has occurred at least once
                settingsDiskSource.getLastSyncTime(userId = userId) != null
            }
            .onEach { hasPersonalCiphers ->
                verifyAndUpdateMigrationState(
                    userId = userId,
                    hasPersonalCiphers = hasPersonalCiphers,
                )
            }

    /**
     * Verifies if the user should migrate their personal vault to organization collections
     * based on active policies, feature flags, and whether they have personal ciphers.
     *
     * @param userId The ID of the user to check for migration.
     * @param hasPersonalCiphers Boolean indicating if the user has any personal ciphers.
     */
    private fun verifyAndUpdateMigrationState(
        userId: String,
        hasPersonalCiphers: Boolean,
    ) {
        mutableVaultMigrationDataStateFlow.update {
            if (!shouldMigrateVault(hasPersonalCiphers)) {
                return@update VaultMigrationData.NoMigrationRequired
            }

            val orgId = policyManager.getPersonalOwnershipPolicyOrganizationId()
                ?: return@update VaultMigrationData.NoMigrationRequired

            val orgName = authDiskSource
                .getOrganizations(userId = userId)
                ?.firstOrNull { it.id == orgId }
                ?.name
                ?: return@update VaultMigrationData.NoMigrationRequired

            VaultMigrationData.MigrationRequired(
                organizationId = orgId,
                organizationName = orgName,
            )
        }
    }

    /**
     * Checks if the user should migrate their vault based on policies, feature flags,
     * network connectivity, and whether they have personal items.
     *
     * @param hasPersonalCiphers Boolean indicating if the user has any personal ciphers.
     * @return true if migration conditions are met, false otherwise.
     */
    private fun shouldMigrateVault(hasPersonalCiphers: Boolean): Boolean =
        policyManager
            .getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            .any() &&
            featureFlagManager.getFeatureFlag(FlagKey.MigrateMyVaultToMyItems) &&
            connectionManager.isNetworkConnected &&
            hasPersonalCiphers
}
