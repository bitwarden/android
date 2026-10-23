package com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.ui.platform.base.BackgroundEvent
import com.bitwarden.ui.platform.base.BaseViewModel
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.snackbar.model.BitwardenSnackbarData
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.pam.repository.PamRepository
import com.x8bit.bitwarden.data.pam.repository.model.DecidePamAccessResult
import com.x8bit.bitwarden.data.pam.repository.model.PamApprovalRequest
import com.x8bit.bitwarden.data.pam.repository.model.PamDecisionVerdict
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.data.pam.repository.model.RevokePamAccessResult
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.SpecialCircumstanceManager
import com.x8bit.bitwarden.data.platform.manager.model.PamAlertType
import com.x8bit.bitwarden.data.platform.manager.model.SpecialCircumstance
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.SettingsRepository
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamNames
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.resolvePamItemIcon
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.resolvePamNames
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import java.time.Clock
import java.time.Duration
import java.time.Instant
import javax.inject.Inject

private const val KEY_STATE = "state"

/**
 * View model for the screen showing one access request to an approver, who can approve or deny it
 * or, once access was granted and started, revoke it.
 */
@Suppress("TooManyFunctions", "LongParameterList")
@HiltViewModel
class PamApprovalDetailViewModel @Inject constructor(
    private val pamRepository: PamRepository,
    private val vaultRepository: VaultRepository,
    private val authRepository: AuthRepository,
    private val environmentRepository: EnvironmentRepository,
    private val settingsRepository: SettingsRepository,
    private val featureFlagManager: FeatureFlagManager,
    private val clock: Clock,
    private val specialCircumstanceManager: SpecialCircumstanceManager,
    savedStateHandle: SavedStateHandle,
) : BaseViewModel<PamApprovalDetailState, PamApprovalDetailEvent, PamApprovalDetailAction>(
    initialState = savedStateHandle[KEY_STATE] ?: PamApprovalDetailState(
        requestId = savedStateHandle.toPamApprovalDetailArgs().requestId,
        viewState = PamApprovalDetailState.ViewState.Loading,
        dialogState = null,
    ),
) {
    /**
     * The request as last loaded, needed to send the decision.
     */
    private var loadedRequest: PamApprovalRequest? = null

    init {
        stateFlow
            .onEach { savedStateHandle[KEY_STATE] = it }
            .launchIn(viewModelScope)
        loadRequest()
    }

    override fun onCleared() {
        val circumstance = specialCircumstanceManager.specialCircumstance
        if (circumstance is SpecialCircumstance.PamAlert &&
            circumstance.pamAlertData.type == PamAlertType.ACCESS_REQUESTED &&
            circumstance.pamAlertData.requestId == state.requestId
        ) {
            specialCircumstanceManager.specialCircumstance = null
        }
        super.onCleared()
    }

    override fun handleAction(action: PamApprovalDetailAction) {
        when (action) {
            PamApprovalDetailAction.BackClick -> sendEvent(PamApprovalDetailEvent.NavigateBack)
            PamApprovalDetailAction.ApproveClick -> handleDecisionClick(PamDecisionVerdict.APPROVE)
            PamApprovalDetailAction.DenyClick -> handleDecisionClick(PamDecisionVerdict.DENY)
            is PamApprovalDetailAction.CommentChange -> handleCommentChange(action)
            PamApprovalDetailAction.ConfirmDecisionClick -> handleConfirmDecisionClick()
            PamApprovalDetailAction.RevokeClick -> handleRevokeClick()
            PamApprovalDetailAction.ConfirmRevokeClick -> handleConfirmRevokeClick()
            PamApprovalDetailAction.DismissDialog -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
            }

            is PamApprovalDetailAction.Internal.RequestReceive -> handleRequestReceive(action)
            is PamApprovalDetailAction.Internal.DecisionResultReceive -> {
                handleDecisionResultReceive(action)
            }

            is PamApprovalDetailAction.Internal.RevokeResultReceive -> {
                handleRevokeResultReceive(action)
            }
        }
    }

    private fun handleDecisionClick(verdict: PamDecisionVerdict) {
        val content = state.viewState as? PamApprovalDetailState.ViewState.Content ?: return
        if (!content.detail.canDecide) return
        mutableStateFlow.update {
            it.copy(
                dialogState = PamApprovalDetailState.DialogState.Decide(
                    verdict = verdict,
                    comment = "",
                ),
            )
        }
    }

    private fun handleRevokeClick() {
        val content = state.viewState as? PamApprovalDetailState.ViewState.Content ?: return
        if (!content.detail.canRevoke) return
        mutableStateFlow.update {
            it.copy(dialogState = PamApprovalDetailState.DialogState.ConfirmRevoke)
        }
    }

    private fun handleConfirmRevokeClick() {
        if (state.dialogState != PamApprovalDetailState.DialogState.ConfirmRevoke) return
        val request = loadedRequest ?: return
        mutableStateFlow.update {
            it.copy(dialogState = PamApprovalDetailState.DialogState.Loading)
        }
        viewModelScope.launch {
            sendAction(
                PamApprovalDetailAction.Internal.RevokeResultReceive(
                    result = pamRepository.revokeAccess(request = request),
                ),
            )
        }
    }

    private fun handleCommentChange(action: PamApprovalDetailAction.CommentChange) {
        val dialog = state.dialogState as? PamApprovalDetailState.DialogState.Decide ?: return
        mutableStateFlow.update {
            it.copy(dialogState = dialog.copy(comment = action.comment))
        }
    }

    private fun handleConfirmDecisionClick() {
        val dialog = state.dialogState as? PamApprovalDetailState.DialogState.Decide ?: return
        if (!dialog.isSubmittable) return
        val request = loadedRequest ?: return
        mutableStateFlow.update {
            it.copy(dialogState = PamApprovalDetailState.DialogState.Loading)
        }
        viewModelScope.launch {
            sendAction(
                PamApprovalDetailAction.Internal.DecisionResultReceive(
                    verdict = dialog.verdict,
                    result = pamRepository.decideAccessRequest(
                        request = request,
                        verdict = dialog.verdict,
                        comment = dialog.comment,
                    ),
                ),
            )
        }
    }

    private fun handleDecisionResultReceive(
        action: PamApprovalDetailAction.Internal.DecisionResultReceive,
    ) {
        when (val result = action.result) {
            DecidePamAccessResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    PamApprovalDetailEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = when (action.verdict) {
                                PamDecisionVerdict.APPROVE -> BitwardenString.pam_request_approved
                                PamDecisionVerdict.DENY,
                                PamDecisionVerdict.UNKNOWN,
                                    -> BitwardenString.pam_request_denied
                            }
                                .asText(),
                        ),
                    ),
                )
            }

            DecidePamAccessResult.AlreadyDecided -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PamApprovalDetailState.DialogState.Error(
                            message = BitwardenString.pam_already_decided.asText(),
                        ),
                    )
                }
            }

            DecidePamAccessResult.NotAllowed -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PamApprovalDetailState.DialogState.Error(
                            message = BitwardenString.pam_cannot_decide_own.asText(),
                        ),
                    )
                }
            }

            is DecidePamAccessResult.Error -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PamApprovalDetailState.DialogState.Error(
                            message = BitwardenString.pam_decision_failed.asText(),
                        ),
                    )
                }
            }
        }
        loadRequest()
    }

    private fun handleRevokeResultReceive(
        action: PamApprovalDetailAction.Internal.RevokeResultReceive,
    ) {
        when (action.result) {
            RevokePamAccessResult.Success -> {
                mutableStateFlow.update { it.copy(dialogState = null) }
                sendEvent(
                    PamApprovalDetailEvent.ShowSnackbar(
                        data = BitwardenSnackbarData(
                            message = BitwardenString.pam_access_revoked.asText(),
                        ),
                    ),
                )
            }

            RevokePamAccessResult.AlreadyEnded -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PamApprovalDetailState.DialogState.Error(
                            message = BitwardenString
                                .pam_this_access_already_ended_the_latest_status_is_shown
                                .asText(),
                        ),
                    )
                }
            }

            RevokePamAccessResult.NotAllowed,
            is RevokePamAccessResult.Error,
                -> {
                mutableStateFlow.update {
                    it.copy(
                        dialogState = PamApprovalDetailState.DialogState.Error(
                            message = BitwardenString.pam_could_not_revoke_access.asText(),
                        ),
                    )
                }
            }
        }
        loadRequest()
    }

    private fun loadRequest() {
        viewModelScope.launch {
            if (!featureFlagManager.getFeatureFlag(FlagKey.PamMobilePrivilegedControls)) {
                sendEvent(PamApprovalDetailEvent.NavigateBack)
                return@launch
            }
            val requestId = state.requestId
            val inbox = pamRepository.getApprovalInbox()
            val fromInbox = inbox.getOrNull()?.firstOrNull { it.id == requestId }
            val result = if (fromInbox != null) {
                Result.success(fromInbox)
            } else {
                val history = pamRepository.getApprovalHistory()
                val fromHistory = history.getOrNull()?.firstOrNull { it.id == requestId }
                when {
                    fromHistory != null -> Result.success(fromHistory)
                    inbox.isFailure && history.isFailure -> {
                        Result.failure(requireNotNull(history.exceptionOrNull()))
                    }

                    else -> Result.success(null)
                }
            }
            sendAction(PamApprovalDetailAction.Internal.RequestReceive(result = result))
        }
    }

    private fun handleRequestReceive(action: PamApprovalDetailAction.Internal.RequestReceive) {
        action.result.fold(
            onSuccess = { request ->
                loadedRequest = request
                val names = resolvePamNames(
                    vaultRepository = vaultRepository,
                    authRepository = authRepository,
                )
                mutableStateFlow.update {
                    it.copy(
                        viewState = request
                            ?.let { found ->
                                PamApprovalDetailState.ViewState.Content(found.toDetail(names))
                            }
                            ?: PamApprovalDetailState.ViewState.NotFound,
                    )
                }
            },
            onFailure = {
                mutableStateFlow.update { current ->
                    current.copy(
                        viewState = current
                            .viewState
                            .takeIf { it is PamApprovalDetailState.ViewState.Content }
                            ?: PamApprovalDetailState.ViewState.Error,
                    )
                }
            },
        )
    }

    private fun PamApprovalRequest.toDetail(names: PamNames): PamApprovalDetailState.Detail =
        PamApprovalDetailState.Detail(
            itemName = names.itemName(request = this),
            itemIcon = resolvePamItemIcon(
                vaultRepository = vaultRepository,
                cipherId = cipherId,
                baseIconUrl = environmentRepository.environment.baseIconUrl,
                isIconLoadingDisabled = settingsRepository.isIconLoadingDisabled,
            ),
            organizationName = names.organizationName(request = this),
            collectionName = names.collectionName(request = this),
            requesterName = requesterName?.takeIf { it.isNotBlank() },
            requesterEmail = requesterEmail?.takeIf { it.isNotBlank() },
            durationMinutes = Duration.between(windowStart, windowEnd).toMinutes(),
            windowStart = windowStart,
            windowEnd = windowEnd,
            reason = reason?.takeIf { it.isNotBlank() },
            status = status,
            isOwnRequest = isOwnRequest,
            canDecide = canBeDecidedAt(now = clock.instant()),
            canRevoke = canRevokeAccessAt(now = clock.instant()),
            accessEndsAt = lease?.takeIf { it.isActiveAt(clock.instant()) }?.notAfter,
            decidedBy = decision?.deciderName,
            decisionComment = decision?.comment?.takeIf { it.isNotBlank() },
        )
}

/**
 * Models state for the access request detail screen.
 */
@Parcelize
data class PamApprovalDetailState(
    val requestId: String,
    val viewState: ViewState,
    val dialogState: DialogState?,
) : Parcelable {

    /**
     * Represents the specific view states of the screen.
     */
    @Parcelize
    sealed class ViewState : Parcelable {

        /**
         * The request is loading.
         */
        @Parcelize
        data object Loading : ViewState()

        /**
         * The request could not be loaded.
         */
        @Parcelize
        data object Error : ViewState()

        /**
         * The request is not in the inbox or the history any more.
         */
        @Parcelize
        data object NotFound : ViewState()

        /**
         * The loaded request.
         */
        @Parcelize
        data class Content(val detail: Detail) : ViewState()
    }

    /**
     * What is shown about the request.
     *
     * @property itemName The name of the item, if it is in the local vault.
     * @property itemIcon The icon of the item: its favicon or, when that is not available, the icon
     * for its type.
     * @property organizationName The name of the organization, if known.
     * @property collectionName The name of the collection, if it is in the local vault.
     * @property requesterName The name of the requester, if known.
     * @property requesterEmail The email of the requester, if known.
     * @property durationMinutes The length of the requested window, in minutes.
     * @property windowStart The start of the requested window.
     * @property windowEnd The end of the requested window.
     * @property reason The reason the requester gave, if any.
     * @property status The status of the request.
     * @property isOwnRequest Whether the user made the request themselves.
     * @property canDecide Whether the user can approve or deny the request.
     * @property canRevoke Whether the user can revoke the access the request granted.
     * @property accessEndsAt When the access ends, while it is active.
     * @property decidedBy Who decided the request, if a person did.
     * @property decisionComment The comment given with that decision.
     */
    @Parcelize
    data class Detail(
        val itemName: String?,
        val itemIcon: IconData,
        val organizationName: String?,
        val collectionName: String?,
        val requesterName: String?,
        val requesterEmail: String?,
        val durationMinutes: Long,
        val windowStart: Instant,
        val windowEnd: Instant,
        val reason: String?,
        val status: PamRequestStatus,
        val isOwnRequest: Boolean,
        val canDecide: Boolean,
        val decidedBy: String?,
        val decisionComment: String?,
        val canRevoke: Boolean = false,
        val accessEndsAt: Instant? = null,
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
         * The confirmation to revoke access that is in use.
         */
        @Parcelize
        data object ConfirmRevoke : DialogState()

        /**
         * The form to confirm a decision.
         *
         * @property verdict The verdict being given.
         * @property comment The comment (optional when approving) or reason (required when
         * denying) as typed.
         */
        @Parcelize
        data class Decide(
            val verdict: PamDecisionVerdict,
            val comment: String,
        ) : DialogState() {

            /**
             * Whether the form can be submitted: a denial needs a reason.
             */
            val isSubmittable: Boolean
                get() = verdict == PamDecisionVerdict.APPROVE || comment.isNotBlank()
        }
    }
}

/**
 * Models events for the access request detail screen.
 */
sealed class PamApprovalDetailEvent {
    /**
     * Navigate back.
     */
    data object NavigateBack : PamApprovalDetailEvent()

    /**
     * Show a snackbar with the given [data].
     */
    data class ShowSnackbar(
        val data: BitwardenSnackbarData,
    ) : PamApprovalDetailEvent(), BackgroundEvent
}

/**
 * Models actions for the access request detail screen.
 */
sealed class PamApprovalDetailAction {
    /**
     * The user clicked the back button.
     */
    data object BackClick : PamApprovalDetailAction()

    /**
     * The user tapped Approve.
     */
    data object ApproveClick : PamApprovalDetailAction()

    /**
     * The user tapped Deny.
     */
    data object DenyClick : PamApprovalDetailAction()

    /**
     * The comment or reason typed in the decision dialog changed.
     */
    data class CommentChange(val comment: String) : PamApprovalDetailAction()

    /**
     * The user confirmed the decision in the dialog.
     */
    data object ConfirmDecisionClick : PamApprovalDetailAction()

    /**
     * The user tapped Revoke access.
     */
    data object RevokeClick : PamApprovalDetailAction()

    /**
     * The user confirmed revoking the access.
     */
    data object ConfirmRevokeClick : PamApprovalDetailAction()

    /**
     * The user dismissed the dialog.
     */
    data object DismissDialog : PamApprovalDetailAction()

    /**
     * Internal actions not performed by the user.
     */
    sealed class Internal : PamApprovalDetailAction() {
        /**
         * The request was loaded. The request is `null` if it was not found.
         */
        data class RequestReceive(
            val result: Result<PamApprovalRequest?>,
        ) : Internal()

        /**
         * The decision was sent.
         */
        data class DecisionResultReceive(
            val verdict: PamDecisionVerdict,
            val result: DecidePamAccessResult,
        ) : Internal()

        /**
         * The revocation was sent.
         */
        data class RevokeResultReceive(
            val result: RevokePamAccessResult,
        ) : Internal()
    }
}
