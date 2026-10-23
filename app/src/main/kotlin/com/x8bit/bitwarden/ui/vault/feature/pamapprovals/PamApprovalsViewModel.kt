package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.base.BaseViewModel
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.pam.manager.PamApprovalManager
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.pam.util.shouldShowPamNotificationPrompt
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Instant
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the screen listing the access requests waiting for the user's decision, and the
 * ones that were already decided.
 */
@Suppress("LongParameterList")
@HiltViewModel
class PamApprovalsViewModel @Inject constructor(
    private val pamRepository: PamRepository,
    private val pamApprovalManager: PamApprovalManager,
    private val vaultRepository: VaultRepository,
    private val authRepository: AuthRepository,
    private val featureFlagManager: FeatureFlagManager,
    private val settingsRepository: SettingsRepository,
    private val environmentRepository: EnvironmentRepository,
    buildInfoManager: BuildInfoManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PamApprovalsState, PamApprovalsEvent, PamApprovalsAction>(
    initialState = savedStateHandle[KEY_STATE] ?: PamApprovalsState(
        viewState = PamApprovalsState.ViewState.Loading,
        selectedTab = PamApprovalsState.Tab.INBOX,
        isRefreshing = false,
        showNotificationPrompt = shouldShowPamNotificationPrompt(
            buildInfoManager = buildInfoManager,
            settingsRepository = settingsRepository,
        ),
    ),
) {
    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        loadRequests()
    }

    override fun handleAction(action: PamApprovalsAction) {
        when (action) {
            PamApprovalsAction.BackClick -> sendEvent(PamApprovalsEvent.NavigateBack)
            PamApprovalsAction.LifecycleResume -> loadRequests()
            PamApprovalsAction.RefreshPull -> handleRefreshPull()
            is PamApprovalsAction.TabClick -> {
                mutableStateFlow.update { it.copy(selectedTab = action.tab) }
            }

            PamApprovalsAction.NotificationPromptDismiss -> {
                settingsRepository.hasShownPamNotificationPrompt = true
                mutableStateFlow.update { it.copy(showNotificationPrompt = false) }
            }

            is PamApprovalsAction.RequestClick -> {
                sendEvent(PamApprovalsEvent.NavigateToDetail(requestId = action.requestId))
            }

            is PamApprovalsAction.Internal.RequestsReceive -> handleRequestsReceive(action)
        }
    }

    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        loadRequests()
    }

    private fun loadRequests() {
        viewModelScope.launch {
            if (!featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)) {
                sendEvent(PamApprovalsEvent.NavigateBack)
                return@launch
            }
            val inbox = pamRepository.getApprovalInbox()
            val history = pamRepository.getApprovalHistory().getOrNull().orEmpty()
            sendAction(
                PamApprovalsAction.Internal.RequestsReceive(
                    result = inbox.map { it to history },
                ),
            )
        }
    }

    private fun handleRequestsReceive(action: PamApprovalsAction.Internal.RequestsReceive) {
        val names = resolvePamNames(
            vaultRepository = vaultRepository,
            authRepository = authRepository,
        )
        val iconOf = { request: PamApprovalRequest ->
            resolvePamItemIcon(
                vaultRepository = vaultRepository,
                cipherId = request.cipherId,
                baseIconUrl = environmentRepository.environment.baseIconUrl,
                isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            )
        }
        action.result.fold(
            onSuccess = { (inbox, history) ->
                pamApprovalManager.refreshPendingCount()
                mutableStateFlow.update {
                    it.copy(
                        viewState = PamApprovalsState.ViewState.Content(
                            inbox = inbox.map { request -> request.toItem(names, iconOf) },
                            history = history.map { request -> request.toItem(names, iconOf) },
                        ),
                        isRefreshing = false,
                    )
                }
            },
            onFailure = {
                mutableStateFlow.update { current ->
                    current.copy(
                        viewState = current
                            .viewState
                            .takeIf { it is PamApprovalsState.ViewState.Content }
                            ?: PamApprovalsState.ViewState.Error,
                        isRefreshing = false,
                    )
                }
            },
        )
    }
}

private fun PamApprovalRequest.toItem(
    names: PamNames,
    iconOf: (PamApprovalRequest) -> IconData,
): PamApprovalsState.RequestItem =
    PamApprovalsState.RequestItem(
        id = id,
        itemName = names.itemName(request = this),
        itemIcon = iconOf(this),
        collectionName = names.collectionName(request = this),
        requesterName = requesterName?.takeIf { it.isNotBlank() } ?: requesterEmail,
        status = status,
        windowStart = windowStart,
        windowEnd = windowEnd,
        submittedAt = submittedAt,
        isOwnRequest = isOwnRequest,
    )

/**
 * Models state for the access approvals screen.
 */
@Parcelize
data class PamApprovalsState(
    val viewState: ViewState,
    val selectedTab: Tab,
    val isRefreshing: Boolean,
    val showNotificationPrompt: Boolean,
) : Parcelable {

    /**
     * The requests shown for the [selectedTab].
     */
    val visibleRequests: List<RequestItem>
        get() = (viewState as? ViewState.Content)
            ?.let { if (selectedTab == Tab.INBOX) it.inbox else it.history }
            .orEmpty()

    /**
     * The tabs of the screen.
     */
    enum class Tab {
        INBOX,
        HISTORY,
    }

    /**
     * Represents the specific view states of the screen.
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * The requests are loading.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * The requests could not be loaded.
         */
        @Parcelize
        data object Error : ViewState()

        /**
         * The loaded requests.
         *
         * @property inbox The requests waiting for a decision, longest waiting first.
         * @property history The requests that were already decided, newest first.
         */
        @Parcelize
        data class Content(
            val inbox: List<RequestItem>,
            val history: List<RequestItem>,
        ) : ViewState()
    }

    /**
     * A single request row.
     *
     * @property id The ID of the request.
     * @property itemName The name of the item, if it is in the local vault.
     * @property itemIcon The icon of the item: its favicon or, when that is not available, the icon
     * for its type.
     * @property collectionName The name of the collection, if it is in the local vault.
     * @property requesterName The name (or email) of the requester, if known.
     * @property status The status of the request.
     * @property windowStart The start of the requested window.
     * @property windowEnd The end of the requested window.
     * @property submittedAt When the request was submitted.
     * @property isOwnRequest Whether the user made the request themselves.
     */
    @Parcelize
    data class RequestItem(
        val id: String,
        val itemName: String?,
        val itemIcon: IconData,
        val collectionName: String?,
        val requesterName: String?,
        val status: PamRequestStatus,
        val windowStart: Instant,
        val windowEnd: Instant,
        val submittedAt: Instant,
        val isOwnRequest: Boolean,
    ) : Parcelable
}

/**
 * Models events for the access approvals screen.
 */
sealed class PamApprovalsEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : PamApprovalsEvent()

    /**
     * Navigate to the detail of the request with the given [requestId].
     */
    data class NavigateToDetail(val requestId: String) : PamApprovalsEvent()
}

/**
 * Models actions for the access approvals screen.
 */
sealed class PamApprovalsAction {
    /**
     * The user clicked the back button.
     */
    data object BackClick : PamApprovalsAction()

    /**
     * The screen resumed.
     */
    data object LifecycleResume : PamApprovalsAction()

    /**
     * The user pulled to refresh.
     */
    data object RefreshPull : PamApprovalsAction()

    /**
     * The user selected the given [tab].
     */
    data class TabClick(val tab: PamApprovalsState.Tab) : PamApprovalsAction()

    /**
     * The notification permission prompt was answered or skipped.
     */
    data object NotificationPromptDismiss : PamApprovalsAction()

    /**
     * The user tapped the request with the given [requestId].
     */
    data class RequestClick(val requestId: String) : PamApprovalsAction()

    /**
     * Internal actions not performed by the user.
     */
    sealed class Internal : PamApprovalsAction() {
        /**
         * The inbox and history were loaded.
         */
        data class RequestsReceive(
            val result: Result<Pair<List<PamApprovalRequest>, List<PamApprovalRequest>>>,
        ) : Internal()
    }
}
