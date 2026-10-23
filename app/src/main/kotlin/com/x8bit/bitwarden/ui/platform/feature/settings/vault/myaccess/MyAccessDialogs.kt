package com.x8bit.bitwarden.ui.platform.feature.settings.vault.myaccess

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.bottomsheet.BitwardenModalBottomSheet
import com.bitwarden.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.ui.platform.components.dialog.BitwardenBasicDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenLoadingDialog
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString

/**
 * The dialogs shown over the my access requests screen.
 */
@Composable
internal fun MyAccessDialogs(
    dialogState: MyAccessState.DialogState?,
    onAction: (MyAccessAction) -> Unit,
) {
    when (dialogState) {
        null -> Unit
        MyAccessState.DialogState.Loading -> {
            BitwardenLoadingDialog(text = stringResource(id = BitwardenString.loading))
        }

        is MyAccessState.DialogState.Error -> {
            BitwardenBasicDialog(
                title = stringResource(id = BitwardenString.an_error_has_occurred),
                message = dialogState.message(),
                onDismissRequest = { onAction(MyAccessAction.DismissDialog) },
            )
        }

        is MyAccessState.DialogState.ConfirmCancel -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.pam_cancel_request_confirm_title),
                message = stringResource(id = BitwardenString.pam_cancel_request_confirm_message),
                confirmButtonText = stringResource(id = BitwardenString.yes),
                dismissButtonText = stringResource(id = BitwardenString.no),
                onConfirmClick = { onAction(MyAccessAction.ConfirmCancelRequestClick) },
                onDismissClick = { onAction(MyAccessAction.DismissDialog) },
                onDismissRequest = { onAction(MyAccessAction.DismissDialog) },
            )
        }

        is MyAccessState.DialogState.ConfirmEnd -> {
            BitwardenTwoButtonDialog(
                title = stringResource(id = BitwardenString.pam_end_access_confirm_title),
                message = stringResource(id = BitwardenString.pam_end_access_confirm_message),
                confirmButtonText = stringResource(id = BitwardenString.yes),
                dismissButtonText = stringResource(id = BitwardenString.no),
                onConfirmClick = { onAction(MyAccessAction.ConfirmEndAccessClick) },
                onDismissClick = { onAction(MyAccessAction.DismissDialog) },
                onDismissRequest = { onAction(MyAccessAction.DismissDialog) },
            )
        }

        is MyAccessState.DialogState.Extend -> ExtendSheet(
            dialogState = dialogState,
            onAction = onAction,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExtendSheet(
    dialogState: MyAccessState.DialogState.Extend,
    onAction: (MyAccessAction) -> Unit,
) {
    BitwardenModalBottomSheet(
        sheetTitle = stringResource(id = BitwardenString.pam_extend_dialog_title),
        onDismiss = { onAction(MyAccessAction.DismissDialog) },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.statusBarsPadding(),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(state = rememberScrollState())
                    .standardHorizontalMargin(),
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.pam_extend_duration_minutes),
                    value = dialogState.durationMinutes,
                    onValueChange = { onAction(MyAccessAction.ExtendDurationChange(it)) },
                    keyboardType = KeyboardType.Number,
                    supportingText = dialogState
                        .maxDurationMinutes
                        ?.let {
                            stringResource(
                                id = BitwardenString.pam_extend_max_hint,
                                it.toString(),
                            )
                        },
                    cardStyle = CardStyle.Full,
                    textFieldTestTag = "PamExtendDurationField",
                    modifier = Modifier.fillMaxWidth(),
                )
                BitwardenTextField(
                    label = stringResource(id = BitwardenString.pam_reason_required_label),
                    value = dialogState.reason,
                    onValueChange = { onAction(MyAccessAction.ExtendReasonChange(it)) },
                    singleLine = false,
                    cardStyle = CardStyle.Full,
                    textFieldTestTag = "PamExtendReasonField",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            BitwardenFilledButton(
                label = stringResource(id = BitwardenString.pam_request_extension),
                onClick = { onAction(MyAccessAction.SubmitExtendClick) },
                isEnabled = dialogState.isSubmittable,
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin()
                    .padding(vertical = 16.dp)
                    .navigationBarsPadding()
                    .imePadding()
                    .testTag("PamSubmitExtendButton"),
            )
        }
    }
}
