package com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.BuildInfoManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.pam.manager.PamApprovalManager
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.PamLeaseStatus
import com.x8bit.bitwarden.data.pam.repository.model.PamMyAccessRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.pam.util.shouldShowPamNotificationPrompt
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemArgs
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.resolvePamItemIcon
import com.x8bit.bitwarden.ui.vault.util.toSdkCipherType
import com.x8bit.bitwarden.ui.vault.util.toVaultItemCipherType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

private const val KEY_STATE = "state"
private const val SECONDS_PER_MINUTE = 60L

/**
 * View model for the screen listing the user's privileged access requests.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class MyAccessViewModel @Inject constructor(
    private val pamRepository: PamRepository,
    private val pamApprovalManager: PamApprovalManager,
    private val vaultRepository: VaultRepository,
    private val featureFlagManager: FeatureFlagManager,
    private val clock: Clock,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    private val settingsRepository: SettingsRepository,
    private val environmentRepository: EnvironmentRepository,
    buildInfoManager: BuildInfoManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<MyAccessState, MyAccessEvent, MyAccessAction>(
    initialState = savedStateHandle[KEY_STATE] ?: MyAccessState(
        viewState = MyAccessState.ViewState.Loading,
        selectedTab = MyAccessState.Tab.CURRENT,
        isRefreshing = false,
        dialogState = null,
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

    override fun onCleared() {
        val circumstance = specialCircumstanceManager.specialCircumstance
        if (circumstance is SpecialCircumstance.PamAlert &&
            circumstance.pamAlertData.type != PamAlertType.ACCESS_REQUESTED
        ) {
            specialCircumstanceManager.specialCircumstance = null
        }
        super.onCleared()
    }

    override fun handleAction(action: MyAccessAction) {
        when (action) {
            MyAccessAction.BackClick -> sendEvent(MyAccessEvent.NavigateBack)
            MyAccessAction.LifecycleResume -> loadRequests()
            MyAccessAction.RefreshPull -> handleRefreshPull()
            MyAccessAction.LeaseCountdownFinished -> loadRequests()
            MyAccessAction.NotificationPromptDismiss -> {
                settingsRepository.hasShownPamNotificationPrompt = true
                mutableStateFlow.update { it.copy(showNotificationPrompt = false) }
            }

            is MyAccessAction.TabClick -> {
                mutableStateFlow.update { it.copy(selectedTab = action.tab) }
            }

            is MyAccessAction.RequestClick -> handleRequestClick(action)
            is MyAccessAction.StartAccessClick -> handleStartAccessClick(action)
            is MyAccessAction.CancelRequestClick -> handleCancelRequestClick(action)
            is MyAccessAction.EndAccessClick -> handleEndAccessClick(action)
            is MyAccessAction.ExtendAccessClick -> handleExtendAccessClick(action)
            is MyAccessAction.ExtendDurationChange -> {
                updateExtendForm { it.copy(durationMinutes = action.durationMinutes) }
            }

            is MyAccessAction.ExtendReasonChange -> {
                updateExtendForm { it.copy(reason = action.reason) }
            }

            MyAccessAction.ConfirmCancelRequestClick -> handleConfirmCancelRequestClick()
            MyAccessAction.ConfirmEndAccessClick -> handleConfirmEndAccessClick()
            MyAccessAction.SubmitExtendClick -> handleSubmitExtendClick()
            MyAccessAction.DismissDialog -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
            }

            is MyAccessAction.Internal -> handleInternalAction(action)
        }
    }

    private fun handleInternalAction(action: MyAccessAction.Internal) {
        when (action) {
            is MyAccessAction.Internal.RequestsReceive -> handleRequestsReceive(action)
            is MyAccessAction.Internal.ActionResultReceive -> handleActionResultReceive(action)
        }
    }

    private fun handleRefreshPull() {
        mutableStateFlow.update { it.copy(isRefreshing = true) }
        loadRequests()
    }

    private fun handleRequestClick(action: MyAccessAction.RequestClick) {
        val cipher = action
            .cipherId
            ?.let { id ->
                vaultRepository
                    .decryptCipherListResultStateFlow
                    .value
                    .data
                    ?.successes
                    ?.firstOrNull { it.id == id }
            }
        val cipherId = cipher?.id
        if (cipher == null || cipherId == null) {
            sendEvent(
                MyAccessEvent.ShowSnackbar(
                    data = BitwardenSnackbarData(
                        message = BitwardenString.pam_item_unavailable.asText(),
                    ),
                ),
            )
            return
        }
        sendEvent(
            MyAccessEvent.NavigateToVaultItem(
                args = VaultItemArgs(
                    vaultItemId = cipherId,
                    cipherType = cipher.type.toSdkCipherType().toVaultItemCipherType(),
                ),
            ),
        )
    }

    private fun handleStartAccessClick(action: MyAccessAction.StartAccessClick) {
        showLoadingAndRun(successMessage = BitwardenString.pam_access_started.asText()) {
            pamRepository.startAccess(requestId = action.requestId)
        }
    }

    private fun handleCancelRequestClick(action: MyAccessAction.CancelRequestClick) {
        mutableStateFlow.update {
            it.copy(dialogState = MyAccessState.DialogState.ConfirmCancel(action.requestId))
        }
    }

    private fun handleEndAccessClick(action: MyAccessAction.EndAccessClick) {
        mutableStateFlow.update {
            it.copy(dialogState = MyAccessState.DialogState.ConfirmEnd(action.leaseId))
        }
    }

    private fun handleExtendAccessClick(action: MyAccessAction.ExtendAccessClick) {
        val item = (state.viewState as? MyAccessState.ViewState.Content)
            ?.requests
            ?.firstOrNull { it.leaseId == action.leaseId && it.canExtend }
            ?: return
        mutableStateFlow.update {
            it.copy(
                dialogState = MyAccessState.DialogState.Extend(
                    leaseId = action.leaseId,
                    durationMinutes = "",
                    reason = "",
                    maxDurationMinutes = item.maxExtensionMinutes,
                ),
            )
        }
    }

    private inline fun updateExtendForm(
        crossinline transform: (
            MyAccessState.DialogState.Extend,
        ) -> MyAccessState.DialogState.Extend,
    ) {
        val dialog = state.dialogState as? MyAccessState.DialogState.Extend ?: return
        mutableStateFlow.update { it.copy(dialogState = transform(dialog)) }
    }

    private fun handleConfirmCancelRequestClick() {
        val dialog = state.dialogState as? MyAccessState.DialogState.ConfirmCancel ?: return
        showLoadingAndRun(successMessage = BitwardenString.pam_request_cancelled.asText()) {
            pamRepository.cancelAccessRequest(requestId = dialog.requestId)
        }
    }

    private fun handleConfirmEndAccessClick() {
        val dialog = state.dialogState as? MyAccessState.DialogState.ConfirmEnd ?: return
        showLoadingAndRun(successMessage = BitwardenString.pam_access_ended.asText()) {
            pamRepository.endAccess(leaseId = dialog.leaseId)
        }
    }

    private fun handleSubmitExtendClick() {
        val dialog = state.dialogState as? MyAccessState.DialogState.Extend ?: return
        val minutes = dialog.durationMinutesOrNull ?: return
        if (!dialog.isSubmittable) return
        showLoadingAndRun(successMessage = BitwardenString.pam_extension_requested.asText()) {
            pamRepository.extendAccess(
                leaseId = dialog.leaseId,
                durationSeconds = minutes * SECONDS_PER_MINUTE,
                reason = dialog.reason.trim(),
            )
        }
    }

    private fun showLoadingAndRun(
        successMessage: Text,
        block: suspend () -> Result<Unit>,
    ) {
        mutableStateFlow.update {
            it.copy(dialogState = MyAccessState.DialogState.Loading)
        }
        viewModelScope.launch {
            sendAction(
                MyAccessAction.Internal.ActionResultReceive(
                    result = block(),
                    successMessage = successMessage,
                ),
            )
        }
    }

    private fun handleActionResultReceive(action: MyAccessAction.Internal.ActionResultReceive) {
        action.result.fold(
            onSuccess = {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    MyAccessEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(message = action.successMessage),
                    ),
                )
            },
            onFailure = {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = MyAccessState.DialogState.Error(
                            message = BitwardenString.pam_action_failed.asText(),
                        ),
                    )
                }
            },
        )
        loadRequests()
    }

    private fun loadRequests() {
        viewModelScope.launch {
            if (!featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)) {
                sendEvent(MyAccessEvent.NavigateBack)
                return@launch
            }
            sendAction(
                MyAccessAction.Internal.RequestsReceive(
                    result = pamRepository.getMyAccessRequests(),
                ),
            )
        }
    }

    private fun handleRequestsReceive(action: MyAccessAction.Internal.RequestsReceive) {
        val now = clock.instant()
        val names = vaultRepository
            .decryptCipherListResultStateFlow
            .value
            .data
            ?.successes
            .orEmpty()
            .mapNotNull { cipher -> cipher.id?.let { it to cipher.name } }
            .toMap()
        val baseIconUrl = environmentRepository.environment.baseIconUrl
        val isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled
        action.result.fold(
            onSuccess = { requests ->
                pamApprovalManager.refreshMyRequestCount()
                mutableStateFlow.update {
                    it.copy(
                        viewState = MyAccessState.ViewState.Content(
                            requests = requests.map { request ->
                                request.toItem(
                                    itemName = names[request.cipherId],
                                    itemIcon = resolvePamItemIcon(
                                        vaultRepository = vaultRepository,
                                        cipherId = request.cipherId,
                                        baseIconUrl = baseIconUrl,
                                        isIconLoadingDisabled = isIconLoadingDisabled,
                                    ),
                                    now = now,
                                )
                            },
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
                            .takeIf { it is MyAccessState.ViewState.Content }
                            ?: MyAccessState.ViewState.Error,
                        isRefreshing = false,
                    )
                }
            },
        )
    }
}

private fun PamMyAccessRequest.toStage(now: Instant): MyAccessState.Stage {
    val lease = lease
    return when {
        lease == null -> toRequestStage()
        lease.isActiveAt(now) -> MyAccessState.Stage.ACTIVE
        else -> when (lease.status) {
            PamLeaseStatus.REVOKED -> MyAccessState.Stage.REVOKED
            PamLeaseStatus.CANCELLED -> MyAccessState.Stage.CANCELLED
            PamLeaseStatus.ACTIVE, PamLeaseStatus.EXPIRED, PamLeaseStatus.UNKNOWN -> {
                MyAccessState.Stage.EXPIRED
            }
        }
    }
}

private fun PamMyAccessRequest.toRequestStage(): MyAccessState.Stage = when (status) {
    PamRequestStatus.PENDING -> MyAccessState.Stage.PENDING
    PamRequestStatus.APPROVED -> {
        if (awaitingActivation && !isExtension) {
            MyAccessState.Stage.READY
        } else {
            MyAccessState.Stage.APPROVED
        }
    }

    PamRequestStatus.DENIED -> MyAccessState.Stage.DENIED
    PamRequestStatus.CANCELLED -> MyAccessState.Stage.CANCELLED
    PamRequestStatus.EXPIRED -> MyAccessState.Stage.EXPIRED
    PamRequestStatus.UNKNOWN -> MyAccessState.Stage.UNKNOWN
}

private fun PamMyAccessRequest.toItem(
    itemName: String?,
    itemIcon: IconData,
    now: Instant,
): MyAccessState.RequestItem {
    val stage = toStage(now = now)
    return MyAccessState.RequestItem(
        id = id,
        cipherId = cipherId,
        itemName = itemName,
        itemIcon = itemIcon,
        stage = stage,
        isExtension = isExtension,
        reason = reason,
        submittedAt = submittedAt,
        leaseId = lease?.id,
        leaseEndsAt = lease?.notAfter,
        canExtend = stage == MyAccessState.Stage.ACTIVE && extensionPolicy != null,
        maxExtensionMinutes = extensionPolicy
            ?.maxDurationSeconds
            ?.let { it / SECONDS_PER_MINUTE },
    )
}

/**
 * Models state for the my access requests screen.
 */
@Parcelize
data class MyAccessState(
    val viewState: ViewState,
    val selectedTab: Tab,
    val isRefreshing: Boolean,
    val dialogState: DialogState?,
    val showNotificationPrompt: Boolean,
) : Parcelable {

    /**
     * The requests shown for the [selectedTab]. Current shows requests that are still in play
     * (pending, ready to start or active); history shows the finished ones.
     */
    val visibleRequests: List<RequestItem>
        get() = (viewState as? ViewState.Content)
            ?.requests
            ?.filter { it.stage.isCurrent == (selectedTab == Tab.CURRENT) }
            .orEmpty()

    /**
     * The tabs of the screen.
     */
    enum class Tab {
        CURRENT,
        HISTORY,
    }

    /**
     * The display stage of a request.
     *
     * @property isCurrent Whether the request is shown in the current list instead of history.
     */
    enum class Stage(val isCurrent: Boolean) {
        PENDING(isCurrent = true),
        READY(isCurrent = true),
        ACTIVE(isCurrent = true),
        APPROVED(isCurrent = false),
        DENIED(isCurrent = false),
        CANCELLED(isCurrent = false),
        EXPIRED(isCurrent = false),
        REVOKED(isCurrent = false),
        UNKNOWN(isCurrent = false),
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
         * The loaded [requests].
         */
        @Parcelize
        data class Content(val requests: List<RequestItem>) : ViewState()
    }

    /**
     * A single request row.
     *
     * @property id The ID of the request.
     * @property cipherId The ID of the item the request is for.
     * @property itemName The name of the item, if it is known.
     * @property itemIcon The icon of the item: its favicon or, when that is not available, the icon
     * for its type.
     * @property stage The display stage.
     * @property isExtension Whether the request extends an existing lease.
     * @property reason The reason given for the request.
     * @property submittedAt When the request was submitted.
     * @property leaseId The ID of the lease the request produced, if any.
     * @property leaseEndsAt When the lease ends, if the request produced one.
     * @property canExtend Whether the server allows extending the active lease.
     * @property maxExtensionMinutes The longest extension allowed, in minutes, if limited.
     */
    @Parcelize
    data class RequestItem(
        val id: String,
        val cipherId: String,
        val itemName: String?,
        val itemIcon: IconData,
        val stage: Stage,
        val isExtension: Boolean,
        val reason: String?,
        val submittedAt: Instant,
        val leaseId: String?,
        val leaseEndsAt: Instant?,
        val canExtend: Boolean,
        val maxExtensionMinutes: Long?,
    ) : Parcelable

    /**
     * Dialogs shown over the screen.
     */
    @Parcelize
    sealed class DialogState : Parcelable {

        /**
         * A blocking progress dialog.
         */
        @Parcelize
        data object Loading : DialogState()

        /**
         * An error dialog.
         */
        @Parcelize
        data class Error(val message: Text) : DialogState()

        /**
         * Asks the user to confirm cancelling the request with the given [requestId].
         */
        @Parcelize
        data class ConfirmCancel(val requestId: String) : DialogState()

        /**
         * Asks the user to confirm ending the lease with the given [leaseId].
         */
        @Parcelize
        data class ConfirmEnd(val leaseId: String) : DialogState()

        /**
         * The form to extend the lease with the given [leaseId].
         *
         * @property durationMinutes The requested extension, in minutes, as typed.
         * @property reason The required reason.
         * @property maxDurationMinutes The longest extension allowed, if limited.
         */
        @Parcelize
        data class Extend(
            val leaseId: String,
            val durationMinutes: String,
            val reason: String,
            val maxDurationMinutes: Long?,
        ) : DialogState() {

            /**
             * The typed duration, or `null` if it is not a positive number.
             */
            val durationMinutesOrNull: Long?
                get() = durationMinutes.trim().toLongOrNull()?.takeIf { it > 0 }

            /**
             * Whether the form has everything needed to be submitted.
             */
            val isSubmittable: Boolean
                get() = durationMinutesOrNull
                    ?.let { maxDurationMinutes == null || it <= maxDurationMinutes } == true &&
                    reason.isNotBlank()
        }
    }
}

/**
 * Models events for the my access requests screen.
 */
sealed class MyAccessEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : MyAccessEvent()

    /**
     * Navigate to the vault item described by [args].
     */
    data class NavigateToVaultItem(val args: VaultItemArgs) : MyAccessEvent()

    /**
     * Show a snackbar with the given [data].
     */
    data class ShowSnackbar(val data: BitwardenSnackbarData) : MyAccessEvent(), BackgroundEvent
}

/**
 * Models actions for the my access requests screen.
 */
sealed class MyAccessAction {
    /**
     * The user clicked the back button.
     */
    data object BackClick : MyAccessAction()

    /**
     * The screen resumed.
     */
    data object LifecycleResume : MyAccessAction()

    /**
     * The user pulled to refresh.
     */
    data object RefreshPull : MyAccessAction()

    /**
     * The countdown of an active lease reached zero.
     */
    data object LeaseCountdownFinished : MyAccessAction()

    /**
     * The notification permission prompt was answered or skipped.
     */
    data object NotificationPromptDismiss : MyAccessAction()

    /**
     * The user selected the given [tab].
     */
    data class TabClick(val tab: MyAccessState.Tab) : MyAccessAction()

    /**
     * The user tapped the request row for the item with the given [cipherId], if it has one.
     */
    data class RequestClick(val cipherId: String?) : MyAccessAction()

    /**
     * The user tapped Start access on the request with the given [requestId].
     */
    data class StartAccessClick(val requestId: String) : MyAccessAction()

    /**
     * The user tapped Cancel on the request with the given [requestId].
     */
    data class CancelRequestClick(val requestId: String) : MyAccessAction()

    /**
     * The user tapped End access on the lease with the given [leaseId].
     */
    data class EndAccessClick(val leaseId: String) : MyAccessAction()

    /**
     * The user tapped Extend on the lease with the given [leaseId].
     */
    data class ExtendAccessClick(val leaseId: String) : MyAccessAction()

    /**
     * The extension duration changed.
     */
    data class ExtendDurationChange(val durationMinutes: String) : MyAccessAction()

    /**
     * The extension reason changed.
     */
    data class ExtendReasonChange(val reason: String) : MyAccessAction()

    /**
     * The user confirmed cancelling the request.
     */
    data object ConfirmCancelRequestClick : MyAccessAction()

    /**
     * The user confirmed ending the access.
     */
    data object ConfirmEndAccessClick : MyAccessAction()

    /**
     * The user submitted the extension form.
     */
    data object SubmitExtendClick : MyAccessAction()

    /**
     * The user dismissed the dialog.
     */
    data object DismissDialog : MyAccessAction()

    /**
     * Internal actions not performed by the user.
     */
    sealed class Internal : MyAccessAction() {
        /**
         * The requests were loaded.
         */
        data class RequestsReceive(
            val result: Result<List<PamMyAccessRequest>>,
        ) : Internal()

        /**
         * A start, cancel, end or extend call finished.
         */
        data class ActionResultReceive(
            val result: Result<Unit>,
            val successMessage: Text,
        ) : Internal()
    }
}
