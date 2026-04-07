package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.collections.CollectionType
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.network.model.BulkShareCiphersJsonRequest
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.model.toCipherWithIdJsonRequest
import com.bitwarden.network.service.CiphersService
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.network.NetworkConnectionManager
import com.x8bit.bitwarden.data.platform.repository.util.observeWhenSubscribedAndUnlocked
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.manager.model.VaultMigrationData
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.MigratePersonalVaultResult
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher
import com.x8bit.bitwarden.data.vault.repository.util.updateFromMiniResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import timber.log.Timber

/**
 * Default implementation of [VaultMigrationManager].
 *
 * Reactively observes vault cipher data and automatically updates migration state when:
 * - Vault is unlocked
 * - Sync has occurred at least once
 * - Cipher data changes
 * - Network connectivity changes
 */
@Suppress("LongParameterList")
class VaultMigrationManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultRepository: VaultRepository,
    private val vaultSdkSource: VaultSdkSource,
    private val ciphersService: CiphersService,
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
     * Observes cipher data, sync state, and network connectivity for the given user and updates
     * migration state when changes occur. Only emits updates after the user has synced at least
     * once to ensure data freshness.
     *
     * Uses optimized [VaultDiskSource.hasPersonalCiphersFlow] query that checks only the
     * indexed organizationId column without loading full cipher JSON data.
     *
     * Combines cipher data with [SettingsDiskSource.getLastSyncTimeFlow] to handle multi-account
     * scenarios where lastSyncTime may be cleared without clearing cipher data. This ensures
     * migration state updates when sync completes, not just when cipher data changes.
     *
     * Also combines with [NetworkConnectionManager.isNetworkConnectedFlow] to ensure migration
     * state updates reactively when network connectivity changes.
     */
    private fun observeCipherDataAndUpdateMigrationState(userId: String) =
        combine(
            vaultDiskSource.hasPersonalCiphersFlow(userId = userId),
            settingsDiskSource.getLastSyncTimeFlow(userId = userId),
            connectionManager.isNetworkConnectedFlow,
        ) { hasPersonalCiphers, lastSyncTime, isNetworkConnected ->
            // Only process after sync has occurred at least once
            lastSyncTime ?: return@combine null
            hasPersonalCiphers to isNetworkConnected
        }
            .filterNotNull()
            .onEach { (hasPersonalCiphers, isNetworkConnected) ->
                verifyAndUpdateMigrationState(
                    userId = userId,
                    hasPersonalCiphers = hasPersonalCiphers,
                    isNetworkConnected = isNetworkConnected,
                )
            }

    /**
     * Verifies if the user should migrate their personal vault to organization collections
     * based on active policies, feature flags, network connectivity, and whether they have
     * personal ciphers.
     *
     * @param userId The ID of the user to check for migration.
     * @param hasPersonalCiphers Boolean indicating if the user has any personal ciphers.
     * @param isNetworkConnected Boolean indicating if the device has network connectivity.
     */
    private fun verifyAndUpdateMigrationState(
        userId: String,
        hasPersonalCiphers: Boolean,
        isNetworkConnected: Boolean,
    ) {
        mutableVaultMigrationDataStateFlow.update {
            if (!shouldMigrateVault(
                    hasPersonalCiphers = hasPersonalCiphers,
                    isNetworkConnected = isNetworkConnected,
                )
            ) {
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
     * @param isNetworkConnected Boolean indicating if the device has network connectivity.
     * @return true if migration conditions are met, false otherwise.
     */
    private fun shouldMigrateVault(
        hasPersonalCiphers: Boolean,
        isNetworkConnected: Boolean,
    ): Boolean =
        policyManager
            .getActivePolicies(PolicyTypeJson.PERSONAL_OWNERSHIP)
            .any() &&
            featureFlagManager.getFeatureFlag(FlagKey.MigrateMyVaultToMyItems) &&
            isNetworkConnected &&
            hasPersonalCiphers

    override suspend fun migratePersonalVault(
        userId: String,
        organizationId: String,
    ): MigratePersonalVaultResult {
        val vaultData = vaultRepository.vaultDataStateFlow.value.data
            ?: return MigratePersonalVaultResult.Failure(
                IllegalStateException("Vault data not available"),
            )

        val defaultUserCollection = getDefaultUserCollection(vaultData, organizationId)
            .getOrElse { return MigratePersonalVaultResult.Failure(it) }

        val personalCiphers = getPersonalCipherViews(vaultData)
            .getOrElse { return MigratePersonalVaultResult.Failure(it) }

        if (personalCiphers.isEmpty()) {
            clearMigrationState()
            return MigratePersonalVaultResult.Success
        }

        val cipherIds = personalCiphers.mapNotNull { it.id }
        val encryptedCiphers = vaultDiskSource.getSelectedCiphers(
            userId = userId,
            cipherIds = cipherIds,
        )
        val encryptedCiphersMap = encryptedCiphers.associateBy { it.id }

        val processedCipherViews = migrateAttachments(userId, personalCiphers)
            .getOrElse { return MigratePersonalVaultResult.Failure(it) }

        encryptAndShareCiphers(
            userId = userId,
            organizationId = organizationId,
            processedCipherViews = processedCipherViews,
            encryptedCiphersMap = encryptedCiphersMap,
            collectionIds = listOfNotNull(defaultUserCollection.id),
        ).getOrElse { return MigratePersonalVaultResult.Failure(it) }

        clearMigrationState()
        return MigratePersonalVaultResult.Success
    }

    override fun clearMigrationState() {
        mutableVaultMigrationDataStateFlow.update { VaultMigrationData.NoMigrationRequired }
    }

    private fun getDefaultUserCollection(
        vaultData: VaultData,
        organizationId: String,
    ): Result<CollectionView> {
        val collection = vaultData.collectionViewList.find {
            it.type == CollectionType.DEFAULT_USER_COLLECTION && it.organizationId == organizationId
        }
        return collection?.asSuccess()
            ?: IllegalStateException("Default user collection not found for organization")
                .asFailure()
    }

    private suspend fun getPersonalCipherViews(
        vaultData: VaultData,
    ): Result<List<CipherView>> = runCatching {
        vaultData.decryptCipherListResult.successes
            .filter { it.organizationId == null }
            .mapNotNull { cipherListView ->
                cipherListView.id?.let { cipherId ->
                    vaultRepository
                        .getCipher(cipherId = cipherId)
                        .toCipherViewOrFailure()
                        ?.getOrElse { error ->
                            return error.asFailure()
                        }
                }
            }
    }

    private suspend fun migrateAttachments(
        userId: String,
        personalCiphers: List<CipherView>,
    ): Result<List<CipherView>> = runCatching {
        personalCiphers.map { cipherView ->
            vaultRepository
                .migrateAttachments(userId = userId, cipherView = cipherView)
                .getOrElse { error ->
                    return error.asFailure()
                }
        }
    }

    private suspend fun encryptAndShareCiphers(
        userId: String,
        organizationId: String,
        processedCipherViews: List<CipherView>,
        encryptedCiphersMap: Map<String, SyncResponseJson.Cipher>,
        collectionIds: List<String>,
    ): Result<Unit> {
        return vaultSdkSource
            .bulkMoveToOrganization(
                userId = userId,
                organizationId = organizationId,
                cipherViews = processedCipherViews,
                collectionIds = collectionIds,
            )
            .map { encryptionContexts ->
                encryptionContexts.mapNotNull { context ->
                    context.cipher.id?.let { cipherId ->
                        context
                            .toEncryptedNetworkCipher()
                            .toCipherWithIdJsonRequest(id = cipherId)
                    }
                }
            }
            .flatMap { cipherRequests ->
                ciphersService.bulkShareCiphers(
                    body = BulkShareCiphersJsonRequest(
                        ciphers = cipherRequests,
                        collectionIds = collectionIds,
                    ),
                )
            }
            .map { bulkShareResponse ->
                bulkShareResponse.cipherMiniResponse.forEach { miniResponse ->
                    encryptedCiphersMap[miniResponse.id]?.let {
                        vaultDiskSource.saveCipher(
                            userId = userId,
                            cipher = it.updateFromMiniResponse(
                                miniResponse = miniResponse,
                                collectionIds = collectionIds,
                            ),
                        )
                    }
                }
            }
    }

    private fun GetCipherResult.toCipherViewOrFailure(): Result<CipherView>? =
        when (this) {
            GetCipherResult.CipherNotFound -> {
                Timber.e("Cipher not found for vault migration.")
                null
            }

            is GetCipherResult.Failure -> {
                Timber.e(this.error, "Failed to decrypt cipher for vault migration.")
                this.error.asFailure()
            }

            is GetCipherResult.Success -> this.cipherView.asSuccess()
        }
}
