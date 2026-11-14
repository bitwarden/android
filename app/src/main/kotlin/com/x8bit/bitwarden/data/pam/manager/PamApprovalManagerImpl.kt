package com.x8bit.bitwarden.data.pam.manager

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.vault.manager.VaultSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

/**
 * Default implementation of [PamApprovalManager].
 *
 * The counts are refreshed when the app comes to the foreground, when the server pushes a refresh
 * signal (types 28 and 29), when the user becomes an approver or gains PAM access and when a
 * decision or a change to one of the user's own requests is recorded in the app.
 */
@Suppress("LongParameterList")
class PamApprovalManagerImpl(
    authRepository: AuthRepository,
    vaultSyncManager: VaultSyncManager,
    featureFlagManager: FeatureFlagManager,
    private val pamRepository: PamRepository,
    pushManager: PushManager,
    dispatcherManager: DispatcherManager,
    processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : PamApprovalManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)
    private var refreshJob: Job? = null
    private var myRequestRefreshJob: Job? = null

    private val mutablePendingCountFlow = MutableStateFlow(0)
    private val mutableMyRequestCountFlow = MutableStateFlow(0)
    private val activeUserIdFlow = authRepository
        .userStateFlow
        .map { it?.activeUserId }
        .distinctUntilChanged()

    override val canApproveFlow: StateFlow<Boolean> = combine(
        featureFlagManager.getFeatureFlagFlow(key = FlagKey.PamMobilePrivilegedControls),
        authRepository.userStateFlow.map { it?.activeAccount?.organizations.orEmpty() },
        vaultSyncManager.collectionsStateFlow.map { it.data.orEmpty() },
    ) { isEnabled, organizations, collections ->
        isEnabled && hasApprovalPrivileges(organizations = organizations, collections = collections)
    }
        .distinctUntilChanged()
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    override val pendingCountFlow: StateFlow<Int> = mutablePendingCountFlow.asStateFlow()

    override val hasPamAccessFlow: StateFlow<Boolean> = combine(
        featureFlagManager.getFeatureFlagFlow(key = FlagKey.PamMobilePrivilegedControls),
        authRepository.userStateFlow.map { it?.activeAccount?.organizations.orEmpty() },
    ) { isEnabled, organizations ->
        isEnabled && organizations.any { it.usePam && !it.isProviderUser }
    }
        .distinctUntilChanged()
        .stateIn(
            scope = unconfinedScope,
            started = SharingStarted.Eagerly,
            initialValue = false,
        )

    override val myRequestCountFlow: StateFlow<Int> = mutableMyRequestCountFlow.asStateFlow()

    init {
        combine(canApproveFlow, activeUserIdFlow) { canApprove, userId -> canApprove to userId }
            .distinctUntilChanged()
            .onEach { (canApprove, userId) ->
                refreshJob?.cancel()
                mutablePendingCountFlow.value = 0
                if (canApprove && userId != null) refreshPendingCount()
            }
            .launchIn(unconfinedScope)
        combine(hasPamAccessFlow, activeUserIdFlow) { hasAccess, userId -> hasAccess to userId }
            .distinctUntilChanged()
            .onEach { (hasAccess, userId) ->
                myRequestRefreshJob?.cancel()
                mutableMyRequestCountFlow.value = 0
                if (hasAccess && userId != null) refreshMyRequestCount()
            }
            .launchIn(unconfinedScope)
        pushManager
            .pamRefreshFlow
            .onEach { refreshAll() }
            .launchIn(unconfinedScope)
        pamRepository
            .accessChangedFlow
            .onEach { refreshAll() }
            .launchIn(unconfinedScope)
        processLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = refreshAll()
            },
        )
    }

    private fun refreshAll() {
        refreshPendingCount()
        refreshMyRequestCount()
    }

    override fun refreshPendingCount() {
        if (!canApproveFlow.value) return
        refreshJob?.cancel()
        refreshJob = ioScope.launch {
            val result = pamRepository.getApprovalInbox()
            ensureActive()
            result.onSuccess { inbox -> mutablePendingCountFlow.value = inbox.size }
        }
    }

    override fun refreshMyRequestCount() {
        if (!hasPamAccessFlow.value) return
        myRequestRefreshJob?.cancel()
        myRequestRefreshJob = ioScope.launch {
            val result = pamRepository.getCurrentAccessRequestCount()
            ensureActive()
            result.onSuccess { count -> mutableMyRequestCountFlow.value = count }
        }
    }
}

/**
 * Whether the user can act on other members' access requests: Manage on at least one collection
 * in a PAM-enabled organization, or the ability to manage every collection of such an
 * organization without being assigned to it. Provider users are excluded because the server does
 * not give them PAM access regardless of their apparent role.
 *
 * Mirrors `hasApprovalPrivileges` of the web client.
 */
internal fun hasApprovalPrivileges(
    organizations: List<Organization>,
    collections: List<CollectionView>,
): Boolean = organizations.any { organization ->
    organization.usePam &&
        !organization.isProviderUser &&
        (
            organization.canEditAllCiphers ||
                collections.any { it.manage && it.organizationId == organization.id }
            )
}
