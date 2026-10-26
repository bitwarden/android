package com.x8bit.bitwarden.ui.vault.feature.pamapprovals.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.ui.platform.base.util.EventsEffect
import com.bitwarden.ui.platform.base.util.cardStyle
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.appbar.BitwardenTopAppBar
import com.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.button.BitwardenFilledErrorButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.bitwarden.ui.platform.components.button.BitwardenOutlinedErrorButton
import com.bitwarden.ui.platform.components.content.BitwardenErrorContent
import com.bitwarden.ui.platform.components.content.BitwardenLoadingContent
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.scaffold.BitwardenScaffold
import com.bitwarden.ui.platform.components.snackbar.BitwardenSnackbarHost
import com.bitwarden.ui.platform.components.snackbar.model.rememberBitwardenSnackbarHostState
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.composition.LocalClock
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.data.pam.repository.model.PamDecisionVerdict
import com.x8bit.bitwarden.data.pam.repository.model.PamRequestStatus
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamItemIcon
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.PamStatusBadge
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.badgeTone
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.labelRes
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.pamDurationText
import com.x8bit.bitwarden.ui.vault.feature.pamapprovals.pamWindowText
import java.time.format.FormatStyle

/**
 * Displays one access request to an approver, who can approve or deny it or, once access was
 * granted and started, revoke it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PamApprovalDetailScreen(
    onNavigateBack: () -> Unit,
    viewModel: PamApprovalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    val snackbarHostState = rememberBitwardenSnackbarHostState()
    EventsEffect(viewModel = viewModel) { event ->
        when (event) {
            PamApprovalDetailEvent.NavigateBack -> onNavigateBack()
            is PamApprovalDetailEvent.ShowSnackbar -> snackbarHostState.showSnackbar(event.data)
        }
    }

    PamApprovalDetailDialogs(
        dialogState = state.dialogState,
        onAction = viewModel::trySendAction,
    )

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    BitwardenScaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            BitwardenTopAppBar(
                title = stringResource(id = BitwardenString.pam_approval_detail_title),
                scrollBehavior = scrollBehavior,
                navigationIcon = rememberVectorPainter(id = BitwardenDrawable.ic_back),
                navigationIconContentDescription = stringResource(id = BitwardenString.back),
                onNavigationIconClick = {
                    viewModel.trySendAction(PamApprovalDetailAction.BackClick)
                },
            )
        },
        snackbarHost = { BitwardenSnackbarHost(bitwardenHostState = snackbarHostState) },
    ) {
        when (val viewState = state.viewState) {
            PamApprovalDetailState.ViewState.Loading -> {
                BitwardenLoadingContent(modifier = Modifier.fillMaxSize())
            }

            PamApprovalDetailState.ViewState.Error -> {
                BitwardenErrorContent(
                    message = stringResource(id = BitwardenString.generic_error_message),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            PamApprovalDetailState.ViewState.NotFound -> {
                BitwardenErrorContent(
                    message = stringResource(id = BitwardenString.pam_approval_not_found),
                    modifier = Modifier.fillMaxSize(),
                )
            }

            is PamApprovalDetailState.ViewState.Content -> {
                PamApprovalDetailContent(
                    detail = viewState.detail,
                    onAction = viewModel::trySendAction,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Suppress("LongMethod")
@Composable
private fun PamApprovalDetailContent(
    detail: PamApprovalDetailState.Detail,
    onAction: (PamApprovalDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .standardHorizontalMargin(),
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("PamApprovalDetails")
                .cardStyle(cardStyle = CardStyle.Full, paddingHorizontal = 16.dp),
        ) {
            DetailHeader(detail = detail)
            detail.organizationName?.let {
                DetailField(
                    label = stringResource(id = BitwardenString.pam_label_organization),
                    value = it,
                    testTag = "PamDetailOrganization",
                )
            }
            detail.collectionName?.let {
                DetailField(
                    label = stringResource(id = BitwardenString.pam_label_collection),
                    value = it,
                    testTag = "PamDetailCollection",
                )
            }
            DetailField(
                label = stringResource(id = BitwardenString.pam_label_requester),
                value = listOfNotNull(detail.requesterName, detail.requesterEmail)
                    .joinToString(separator = "\n")
                    .ifEmpty { stringResource(id = BitwardenString.pam_unknown_requester) },
                testTag = "PamDetailRequester",
            )
            DetailField(
                label = stringResource(id = BitwardenString.pam_label_duration),
                value = pamDurationText(minutes = detail.durationMinutes),
                testTag = "PamDetailDuration",
            )
            DetailField(
                label = stringResource(id = BitwardenString.pam_label_window),
                value = pamWindowText(start = detail.windowStart, end = detail.windowEnd),
                testTag = "PamDetailWindow",
            )
            DetailField(
                label = stringResource(id = BitwardenString.pam_reason),
                value = detail.reason
                    ?: stringResource(id = BitwardenString.pam_no_reason_provided),
                testTag = "PamDetailReason",
            )
            detail.accessEndsAt?.let {
                DetailField(
                    label = stringResource(id = BitwardenString.pam_access_ends),
                    value = it.toFormattedDateTimeStyle(
                        dateStyle = FormatStyle.SHORT,
                        timeStyle = FormatStyle.SHORT,
                        clock = LocalClock.current,
                    ),
                    testTag = "PamDetailAccessEnds",
                )
            }
            detail.decidedBy?.let {
                DetailField(
                    label = stringResource(id = BitwardenString.pam_label_decided_by),
                    value = it,
                    testTag = "PamDetailDecidedBy",
                )
            }
            detail.decisionComment?.let {
                DetailField(
                    label = stringResource(id = BitwardenString.pam_comment),
                    value = it,
                    testTag = "PamDetailDecisionComment",
                )
            }
        }
        if (detail.isOwnRequest && detail.status == PamRequestStatus.PENDING) {
            Text(
                text = stringResource(id = BitwardenString.pam_cannot_decide_own),
                style = BitwardenTheme.typography.bodyMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier.testTag("PamCannotDecideOwn"),
            )
        }
        if (detail.canDecide) {
            BitwardenFilledButton(
                label = stringResource(id = BitwardenString.pam_approve),
                onClick = { onAction(PamApprovalDetailAction.ApproveClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("PamApproveButton"),
            )
            BitwardenOutlinedButton(
                label = stringResource(id = BitwardenString.pam_deny),
                onClick = { onAction(PamApprovalDetailAction.DenyClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("PamDenyButton"),
            )
        }
        if (detail.canRevoke) {
            BitwardenOutlinedErrorButton(
                label = stringResource(id = BitwardenString.pam_revoke_access),
                onClick = { onAction(PamApprovalDetailAction.RevokeClick) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("PamRevokeAccessButton"),
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Spacer(modifier = Modifier.navigationBarsPadding())
    }
}

@Composable
private fun DetailHeader(
    detail: PamApprovalDetailState.Detail,
    modifier: Modifier = Modifier,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        PamItemIcon(
            iconData = detail.itemIcon,
            iconTestTag = "PamDetailItemIcon",
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = detail.itemName
                    ?: stringResource(id = BitwardenString.pam_item_fallback_name),
                style = BitwardenTheme.typography.titleMedium,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.testTag("PamDetailItem"),
            )
            PamStatusBadge(
                label = stringResource(id = detail.status.labelRes()),
                tone = detail.status.badgeTone(),
                modifier = Modifier.testTag("PamDetailStatus"),
            )
        }
    }
}

@Composable
private fun DetailField(
    label: String,
    value: String,
    testTag: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = BitwardenTheme.typography.labelMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
        )
        Text(
            text = value,
            style = BitwardenTheme.typography.bodyLarge,
            color = BitwardenTheme.colorScheme.text.primary,
            modifier = Modifier.testTag(testTag),
        )
    }
}

@Composable
private fun PamApprovalDetailDialogs(
    dialogState: PamApprovalDetailState.DialogState?,
    onAction: (PamApprovalDetailAction) -> Unit,
) {
    when (dialogState) {
        null -> Unit
        PamApprovalDetailState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = stringResource(id = BitwardenString.loading))
        }

        is PamApprovalDetailState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.an_error_has_occurred),
                message = dialogState.message(),
                onDismissRequest = { onAction(PamApprovalDetailAction.DismissDialog) },
            )
        }

        PamApprovalDetailState.DialogState.ConfirmRevoke -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.pam_revoke_access),
                message = stringResource(
                    id = BitwardenString
                        .pam_this_ends_their_access_now_they_will_need_a_new_approval_to_regain_it,
                ),
                confirmButtonText = stringResource(id = BitwardenString.pam_revoke_access),
                dismissButtonText = stringResource(id = BitwardenString.cancel),
                onConfirmClick = { onAction(PamApprovalDetailAction.ConfirmRevokeClick) },
                onDismissClick = { onAction(PamApprovalDetailAction.DismissDialog) },
                onDismissRequest = { onAction(PamApprovalDetailAction.DismissDialog) },
                confirmTextColor = BitwardenTheme.colorScheme.status.error,
            )
        }

        is PamApprovalDetailState.DialogState.Decide -> DecideSheet(
            dialogState = dialogState,
            onAction = onAction,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DecideSheet(
    dialogState: PamApprovalDetailState.DialogState.Decide,
    onAction: (PamApprovalDetailAction) -> Unit,
) {
    val isApprove = dialogState.verdict == PamDecisionVerdict.APPROVE
    BitwardenModalBottomSheet(
        sheetTitle = stringResource(
            id = if (isApprove) {
                BitwardenString.pam_approve_dialog_title
            } else {
                BitwardenString.pam_deny_dialog_title
            },
        ),
        onDismiss = { onAction(PamApprovalDetailAction.DismissDialog) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                DecideSheetField(
                    dialogState = dialogState,
                    onAction = onAction,
                    modifier = Modifier
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
            val confirmModifier = Modifier
                .fillMaxWidth()
                .standardHorizontalMargin()
                .padding(vertical = 16.dp)
                .navigationBarsPadding()
                .imePadding()
                .testTag("PamConfirmDecisionButton")
            if (isApprove) {
                BitwardenFilledButton(
                    label = stringResource(id = BitwardenString.pam_confirm_approve),
                    onClick = { onAction(PamApprovalDetailAction.ConfirmDecisionClick) },
                    isEnabled = dialogState.isSubmittable,
                    modifier = confirmModifier,
                )
            } else {
                BitwardenFilledErrorButton(
                    label = stringResource(id = BitwardenString.pam_confirm_deny),
                    onClick = { onAction(PamApprovalDetailAction.ConfirmDecisionClick) },
                    isEnabled = dialogState.isSubmittable,
                    modifier = confirmModifier,
                )
            }
        }
    }
}

@Composable
private fun DecideSheetField(
    dialogState: PamApprovalDetailState.DialogState.Decide,
    onAction: (PamApprovalDetailAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val isApprove = dialogState.verdict == PamDecisionVerdict.APPROVE
    BitwardenTextField(
        label = stringResource(
            id = if (isApprove) {
                BitwardenString.pam_comment
            } else {
                BitwardenString.pam_deny_reason_required_label
            },
        ),
        value = dialogState.comment,
        onValueChange = { onAction(PamApprovalDetailAction.CommentChange(it)) },
        singleLine = false,
        supportingText = stringResource(id = BitwardenString.pam_comment_optional_hint)
            .takeIf { isApprove },
        cardStyle = CardStyle.Full,
        textFieldTestTag = "PamDecisionCommentField",
        modifier = modifier,
    )
}
