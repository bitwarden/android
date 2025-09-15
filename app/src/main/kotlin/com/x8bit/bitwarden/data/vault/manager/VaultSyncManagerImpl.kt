package com.x8bit.bitwarden.data.vault.manager

import com.bitwarden.core.InitOrgCryptoRequest
import com.bitwarden.network.model.SyncResponseJson
import com.bitwarden.network.service.SyncService
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.manager.UserLogoutManager
import com.x8bit.bitwarden.data.auth.repository.model.LogoutReason
import com.x8bit.bitwarden.data.auth.repository.util.toUpdatedUserStateJson
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.SecurityStampMismatchException
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.SyncVaultDataResult
import kotlinx.coroutines.flow.firstOrNull
import java.time.Clock

/**
 * Default implementation of [VaultSyncManager].
 */
@Suppress("LongParameterList")
class VaultSyncManagerImpl(
    private val syncService: SyncService,
    private val settingsDiskSource: SettingsDiskSource,
    private val authDiskSource: AuthDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val userLogoutManager: UserLogoutManager,
    private val clock: Clock,
) : VaultSyncManager {

    @Suppress("LongMethod")
    override suspend fun sync(
        userId: String,
        forced: Boolean,
    ): SyncVaultDataResult {
        if (!forced) {
            // Skip this check if we are forcing the request.
            val lastSyncInstant = settingsDiskSource
                .getLastSyncTime(userId = userId)
                ?.toEpochMilli()
            lastSyncInstant?.let { lastSyncTimeMs ->
                // If the lasSyncState is null we just sync, no checks required.
                syncService
                    .getAccountRevisionDateMillis()
                    .fold(
                        onSuccess = { serverRevisionDate ->
                            if (serverRevisionDate < lastSyncTimeMs) {
                                // We can skip the actual sync call if there is no new data or
                                // database scheme changes since the last sync.
                                settingsDiskSource.storeLastSyncTime(
                                    userId = userId,
                                    lastSyncTime = clock.instant(),
                                )
                                vaultDiskSource.resyncVaultData(userId = userId)
                                val itemsAvailable = vaultDiskSource
                                    .getCiphersFlow(userId)
                                    .firstOrNull()
                                    ?.isNotEmpty() == true
                                return SyncVaultDataResult.Success(itemsAvailable = itemsAvailable)
                            }
                        },
                        onFailure = {
                            return SyncVaultDataResult.Error(throwable = it)
                        },
                    )
            }
        }

        return syncService
            .sync()
            .fold(
                onSuccess = { syncResponse ->
                    val localSecurityStamp = authDiskSource.userState?.activeAccount?.profile?.stamp
                    val serverSecurityStamp = syncResponse.profile.securityStamp

                    // Log the user out if the stamps do not match
                    localSecurityStamp?.let {
                        if (serverSecurityStamp != localSecurityStamp) {
                            userLogoutManager.softLogout(
                                // Ensure UserLogoutManager is available
                                userId = userId,
                                reason = LogoutReason.SecurityStamp,
                            )
                            return SyncVaultDataResult.Error(
                                throwable = SecurityStampMismatchException(),
                            )
                        }
                    }

                    // Update user information with additional information from sync response
                    authDiskSource.userState = authDiskSource.userState?.toUpdatedUserStateJson(
                        syncResponse = syncResponse,
                    )

                    unlockVaultForOrganizationsIfNecessary(syncResponse = syncResponse)
                    storeProfileData(syncResponse = syncResponse)

                    // Treat absent network policies as known empty data to
                    // distinguish between unknown null data.
                    authDiskSource.storePolicies(
                        userId = userId,
                        policies = syncResponse.policies.orEmpty(),
                    )
                    settingsDiskSource.storeLastSyncTime(
                        userId = userId,
                        lastSyncTime = clock.instant(),
                    )
                    vaultDiskSource.replaceVaultData(userId = userId, vault = syncResponse)
                    val itemsAvailable = syncResponse.ciphers?.isNotEmpty() == true
                    SyncVaultDataResult.Success(itemsAvailable = itemsAvailable)
                },
                onFailure = { throwable ->
                    SyncVaultDataResult.Error(throwable = throwable)
                },
            )
    }

    private suspend fun unlockVaultForOrganizationsIfNecessary(
        syncResponse: SyncResponseJson,
    ) {
        val profile = syncResponse.profile
        val organizationKeys = profile.organizations
            .orEmpty()
            .filter { it.key != null }
            .associate { it.id to requireNotNull(it.key) }
            .takeUnless { it.isEmpty() }
            ?: return

        // There shouldn't be issues when unlocking directly from the syncResponse so we can ignore
        // the return type here.
        vaultSdkSource
            .initializeOrganizationCrypto(
                userId = syncResponse.profile.id,
                request = InitOrgCryptoRequest(
                    organizationKeys = organizationKeys,
                ),
            )
    }

    private fun storeProfileData(
        syncResponse: SyncResponseJson,
    ) {
        val profile = syncResponse.profile
        val userId = profile.id
        authDiskSource.apply {
            storeUserKey(
                userId = userId,
                userKey = profile.key,
            )
            storePrivateKey(
                userId = userId,
                privateKey = profile.privateKey,
            )
            storeAccountKeys(
                userId = userId,
                accountKeys = profile.accountKeys,
            )
            storeOrganizationKeys(
                userId = userId,
                organizationKeys = profile.organizations
                    .orEmpty()
                    .filter { it.key != null }
                    .associate { it.id to requireNotNull(it.key) },
            )
            storeShouldUseKeyConnector(
                userId = userId,
                shouldUseKeyConnector = profile.shouldUseKeyConnector,
            )
            storeOrganizations(
                userId = userId,
                organizations = profile.organizations,
            )
        }
    }
}
