package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.manager.model.FlagKey
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Implementation of [FirstTimeActionManager]
 */
class FirstTimeActionManagerImpl @Inject constructor(
    dispatcherManager: DispatcherManager,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val featureFlagManager: FeatureFlagManager,
) : FirstTimeActionManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    override val allSettingsBadgeCountFlow: StateFlow<Int>
        get() = combine(
            listOf(
                allSecuritySettingsBadgeCountFlow,
                allAutofillSettingsBadgeCountFlow,
                allVaultSettingsBadgeCountFlow,
            ),
        ) {
            it.sum()
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = 0,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allSecuritySettingsBadgeCountFlow: StateFlow<Int>
        get() = authDiskSource
            .activeUserIdChangesFlow
            .filterNotNull()
            .flatMapLatest {
                // can be expanded to support multiple security settings
                settingsDiskSource.getShowUnlockSettingBadgeFlow(userId = it)
                    .map { showUnlockBadge ->
                        listOfNotNull(showUnlockBadge)
                    }
                    .map { list ->
                        list.count { badgeOnValue -> badgeOnValue }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = 0,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allAutofillSettingsBadgeCountFlow: StateFlow<Int>
        get() = authDiskSource
            .activeUserIdChangesFlow
            .filterNotNull()
            .flatMapLatest {
                // Can be expanded to support multiple autofill settings
                settingsDiskSource.getShowAutoFillSettingBadgeFlow(userId = it)
                    .map { showAutofillBadge ->
                        listOfNotNull(showAutofillBadge)
                    }
                    .map { list ->
                        list.count { showBadge -> showBadge }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = 0,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val allVaultSettingsBadgeCountFlow: StateFlow<Int>
        get() = authDiskSource
            .activeUserIdChangesFlow
            .filterNotNull()
            .flatMapLatest {
                combine(
                    getShowImportLoginsFlowInternal(userId = it),
                    featureFlagManager.getFeatureFlagFlow(FlagKey.ImportLoginsFlow),
                ) { showImportLogins, importLoginsEnabled ->
                    val shouldShowImportLogins = showImportLogins && importLoginsEnabled
                    listOf(shouldShowImportLogins)
                }
                    .map { list ->
                        list.count { showImportLogins -> showImportLogins }
                    }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Lazily,
                initialValue = 0,
            )

    /**
     * Returns a [Flow] that emits every time the active user's first time state is changed.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    override val firstTimeStateFlow: Flow<FirstTimeState>
        get() = authDiskSource
            .activeUserIdChangesFlow
            .filterNotNull()
            .flatMapLatest { activeUserId ->
                combine(
                    listOf(
                        getShowImportLoginsFlowInternal(userId = activeUserId),
                        settingsDiskSource.getShowUnlockSettingBadgeFlow(userId = activeUserId),
                        settingsDiskSource.getShowAutoFillSettingBadgeFlow(userId = activeUserId),
                    ),
                ) {
                    FirstTimeState(
                        showImportLoginsCard = it[0],
                        showSetupUnlockCard = it[1],
                        showSetupAutofillCard = it[2],
                    )
                }
            }
            .onStart {
                emit(
                    FirstTimeState(
                        showImportLoginsCard = null,
                        showSetupUnlockCard = null,
                        showSetupAutofillCard = null,
                    ),
                )
            }
            .distinctUntilChanged()

    /**
     * Internal implementation to get a flow of the showImportLogins value which takes
     * into account if the vault is empty.
     */
    private fun getShowImportLoginsFlowInternal(userId: String): Flow<Boolean> {
        return authDiskSource.getShowImportLoginsFlow(userId)
            .combine(
                vaultDiskSource.getCiphers(userId),
            ) { showImportLogins, ciphers ->
                showImportLogins ?: true && ciphers.isEmpty()
            }
    }

    /**
     * Get the current [FirstTimeState] of the active user if available, otherwise return
     * a default configuration.
     */
    override val currentOrDefaultUserFirstTimeState: FirstTimeState
        get() =
            authDiskSource
                .userState
                ?.activeUserId
                ?.let {
                    FirstTimeState(
                        showImportLoginsCard = authDiskSource.getShowImportLogins(it),
                        showSetupUnlockCard = settingsDiskSource.getShowUnlockSettingBadge(it),
                        showSetupAutofillCard = settingsDiskSource.getShowAutoFillSettingBadge(it),
                    )
                }
                ?: FirstTimeState(
                    showImportLoginsCard = null,
                    showSetupUnlockCard = null,
                    showSetupAutofillCard = null,
                )

    override fun storeShowUnlockSettingBadge(showBadge: Boolean) {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storeShowUnlockSettingBadge(
            userId = activeUserId,
            showBadge = showBadge,
        )
    }

    override fun storeShowAutoFillSettingBadge(showBadge: Boolean) {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storeShowAutoFillSettingBadge(
            userId = activeUserId,
            showBadge = showBadge,
        )
    }

    override fun storeShowImportLogins(showImportLogins: Boolean) {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        authDiskSource.storeShowImportLogins(
            userId = activeUserId,
            showImportLogins = showImportLogins,
        )
    }
}
