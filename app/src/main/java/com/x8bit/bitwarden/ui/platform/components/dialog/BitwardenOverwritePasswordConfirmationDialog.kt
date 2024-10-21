package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.OverwritePasswordConfirmationPromptReason

/**
 * A reusable dialog for confirming whether or not the user wants to overwrite an existing FIDO 2
 * credential.
 *
 * @param onConfirmClick A callback for when the overwrite confirmation button is clicked.
 * @param onDismissRequest A callback for when the dialog is requesting dismissal.
 */
@Suppress("MaxLineLength")
@Composable
fun BitwardenOverwritePasswordConfirmationDialog(
    reason: OverwritePasswordConfirmationPromptReason,
    onConfirmClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    BitwardenTwoButtonDialog(
        title = stringResource(
            id = when (reason) {
                OverwritePasswordConfirmationPromptReason.UsernameAndPassword -> R.string.overwrite_username_and_password
                OverwritePasswordConfirmationPromptReason.Username -> R.string.overwrite_username
                OverwritePasswordConfirmationPromptReason.Password -> R.string.overwrite_password
            }
        ),
        message = stringResource(
            id = when (reason) {
                OverwritePasswordConfirmationPromptReason.UsernameAndPassword -> R.string.this_item_already_contains_a_username_and_password_are_you_sure_you_want_to_overwrite_the_current_passkey
                OverwritePasswordConfirmationPromptReason.Username -> R.string.this_item_already_contains_a_username_are_you_sure_you_want_to_overwrite_the_current_passkey
                OverwritePasswordConfirmationPromptReason.Password -> R.string.this_item_already_contains_a_password_are_you_sure_you_want_to_overwrite_the_current_passkey
            }
        ),
        confirmButtonText = stringResource(id = R.string.ok),
        dismissButtonText = stringResource(id = R.string.cancel),
        onConfirmClick = onConfirmClick,
        onDismissClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    )
}
