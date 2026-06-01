package com.x8bit.bitwarden.data.platform.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.autofill.manager.AutofillEnabledManager
import com.x8bit.bitwarden.data.autofill.manager.browser.BrowserThirdPartyAutofillEnabledManager
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.CoachMarkTourType
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
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
@Suppress("TooManyFunctions")
class FirstTimeActionManagerImpl @Inject constructor(
    dispatcherManager: DispatcherManager,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val autofillEnabledManager: AutofillEnabledManager,
    private val thirdPartyAutofillEnabledManager: BrowserThirdPartyAutofillEnabledManager,
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
                combine(
                    getShowAutofillSettingBadgeFlowInternal(userId = it),
                    getShowBrowserAutofillSettingBadgeFlowInternal(userId = it),
                ) { showAutofillBadge, showBrowserAutofillBadge ->
                    listOf(showAutofillBadge, showBrowserAutofillBadge)
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
                getShowImportLoginsSettingBadgeFlowInternal(userId = it)
                    .map { showImportLogins -> listOf(showImportLogins) }
                    .map { list -> list.count { showImportLogins -> showImportLogins } }
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
                        getShowAutofillSettingBadgeFlowInternal(userId = activeUserId),
                        getShowImportLoginsSettingBadgeFlowInternal(userId = activeUserId),
                        getShowBrowserAutofillSettingBadgeFlowInternal(userId = activeUserId),
                    ),
                ) {
                    FirstTimeState(
                        showImportLoginsCard = it[0],
                        showSetupUnlockCard = it[1],
                        showSetupAutofillCard = it[2],
                        showImportLoginsCardInSettings = it[3],
                        showSetupBrowserAutofillCard = it[4],
                    )
                }
            }
            .onStart { emit(currentOrDefaultUserFirstTimeState) }
            .distinctUntilChanged()

    override val shouldShowAddLoginCoachMarkFlow: Flow<Boolean>
        get() = settingsDiskSource
            .getShouldShowAddLoginCoachMarkFlow()
            .map { it != false }
            .mapFalseIfAnyLoginCiphersAvailable()
            .distinctUntilChanged()

    override val shouldShowGeneratorCoachMarkFlow: Flow<Boolean>
        get() = settingsDiskSource
            .getShouldShowGeneratorCoachMarkFlow()
            .map { it != false }
            .mapFalseIfAnyLoginCiphersAvailable()
            .distinctUntilChanged()

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
                        showSetupAutofillCard = getShowAutofillSettingBadgeInternal(it),
                        showImportLoginsCardInSettings = settingsDiskSource
                            .getShowImportLoginsSettingBadge(it),
                        showSetupBrowserAutofillCard = settingsDiskSource
                            .getShowBrowserAutofillSettingBadge(it),
                    )
                }
                ?: FirstTimeState()

    override fun storeShowUnlockSettingBadge(showBadge: Boolean) {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storeShowUnlockSettingBadge(
            userId = activeUserId,
            showBadge = showBadge,
        )
    }

    override fun storeShowBrowserAutofillSettingBadge(showBadge: Boolean) {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storeShowBrowserAutofillSettingBadge(
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

    override fun storeShowImportLoginsSettingsBadge(showBadge: Boolean) {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storeShowImportLoginsSettingBadge(
            userId = activeUserId,
            showBadge = showBadge,
        )
    }

    override fun markCoachMarkTourCompleted(tourCompleted: CoachMarkTourType) {
        when (tourCompleted) {
            CoachMarkTourType.ADD_LOGIN -> {
                settingsDiskSource.storeShouldShowAddLoginCoachMark(shouldShow = false)
            }

            CoachMarkTourType.GENERATOR -> {
                settingsDiskSource.storeShouldShowGeneratorCoachMark(shouldShow = false)
            }
        }
    }

    /**
     * Internal implementation to get a flow of the showImportLogins value which takes
     * into account if the vault is empty.
     */
    private fun getShowImportLoginsFlowInternal(userId: String): Flow<Boolean> {
        return authDiskSource
            .getShowImportLoginsFlow(userId)
            .combine(
                vaultDiskSource.getCiphersFlow(userId),
            ) { showImportLogins, ciphers ->
                showImportLogins ?: true && ciphers.isEmpty()
            }
    }

    /**
     * Internal implementation to get a flow of the showImportLogins value which takes
     * into account if the vault is empty.
     */
    private fun getShowImportLoginsSettingBadgeFlowInternal(userId: String): Flow<Boolean> {
        return settingsDiskSource
            .getShowImportLoginsSettingBadgeFlow(userId)
            .combine(
                vaultDiskSource.getCiphersFlow(userId),
            ) { showImportLogins, ciphers ->
                showImportLogins ?: false && ciphers.isEmpty()
            }
    }

    /**
     * Internal implementation to get a flow of the showBrowserAutofill value which takes
     * into account if autofill and if browser autofill is already enabled.
     */
    private fun getShowBrowserAutofillSettingBadgeFlowInternal(userId: String): Flow<Boolean> =
        combine(
            settingsDiskSource.getShowBrowserAutofillSettingBadgeFlow(userId = userId),
            autofillEnabledManager.isAutofillEnabledStateFlow,
            thirdPartyAutofillEnabledManager.browserThirdPartyAutofillStatusFlow,
        ) { showBadge, autofillEnabled, status ->
            showBadge ?: false && autofillEnabled && status.isAnyIsAvailableAndDisabled
        }

    /**
     * Internal implementation to get a flow of the showAutofill value which takes
     * into account if autofill is already enabled globally.
     */
    private fun getShowAutofillSettingBadgeFlowInternal(userId: String): Flow<Boolean> {
        return settingsDiskSource
            .getShowAutoFillSettingBadgeFlow(userId)
            .combine(
                autofillEnabledManager.isAutofillEnabledStateFlow,
            ) { showAutofill, autofillEnabled ->
                showAutofill ?: false && !autofillEnabled
            }
    }

    private fun getShowAutofillSettingBadgeInternal(userId: String): Boolean {
        return settingsDiskSource.getShowAutoFillSettingBadge(userId) ?: false &&
            !autofillEnabledManager.isAutofillEnabled
    }

    /**
     *  If there are any existing "Login" type ciphers then we'll map the current value
     *  of the receiver Flow to `false`.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun Flow<Boolean>.mapFalseIfAnyLoginCiphersAvailable(): Flow<Boolean> =
        authDiskSource
            .activeUserIdChangesFlow
            .filterNotNull()
            .flatMapLatest { activeUserId ->
                combine(
                    flow = this,
                    flow2 = vaultDiskSource.getCiphersFlow(activeUserId),
                ) { receiverCurrentValue, ciphers ->
                    receiverCurrentValue && ciphers.none {
                        it.login != null && it.organizationId == null
                    }
                }
            }
            .distinctUntilChanged()
}
