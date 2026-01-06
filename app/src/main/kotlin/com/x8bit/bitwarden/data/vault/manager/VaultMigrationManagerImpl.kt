package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Default implementation of [VaultMigrationManager].
 */
class VaultMigrationManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val policyManager: PolicyManager,
    private val featureFlagManager: FeatureFlagManager,
    private val connectionManager: NetworkConnectionManager,
) : VaultMigrationManager {

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    private val mutableVaultMigrationDataStateFlow =
        MutableStateFlow<VaultMigrationData>(value = VaultMigrationData.NoMigrationRequired)

    override val vaultMigrationDataStateFlow: StateFlow<VaultMigrationData>
        get() = mutableVaultMigrationDataStateFlow.asStateFlow()

    override fun verifyAndUpdateMigrationState(cipherList: List<SyncResponseJson.Cipher>) {
        val userId = activeUserId ?: return

        mutableVaultMigrationDataStateFlow.update {
            if (shouldMigrateVault { cipherList.any { it.organizationId == null } }) {
                val orgId = policyManager.getPersonalOwnershipPolicyOrganizationId()
                val orgName = authDiskSource
                    .getOrganizations(userId = userId)
                    ?.firstOrNull { it.id == orgId }
                    ?.name

                if (orgId != null && orgName != null) {
                    VaultMigrationData.MigrationRequired(
                        organizationId = orgId,
                        organizationName = orgName,
                    )
                } else {
                    VaultMigrationData.NoMigrationRequired
                }
            } else {
                VaultMigrationData.NoMigrationRequired
            }
        }
    }

    override fun shouldMigrateVault(hasPersonalItems: () -> Boolean): Boolean {
        return policyManager
            .getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            .any() &&
            featureFlagManager.getFeatureFlag(FlagKey.MigrateMyVaultToMyItems) &&
            connectionManager.isNetworkConnected &&
            hasPersonalItems()
    }
}
