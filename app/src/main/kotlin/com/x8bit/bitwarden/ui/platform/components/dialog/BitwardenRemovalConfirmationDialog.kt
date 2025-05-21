package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.AccountSummary

/**
 * A reusable dialog for confirming whether or not the user wants to remove their account.
 *
 * @param onDismissRequest A callback for when the dialog is requesting dismissal.
 * @param onConfirmClick A callback for when the log out confirmation button is clicked.
 * @param accountSummary Optional account information that may be used to provide additional
 * information.
 */
@Composable
fun BitwardenRemovalConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
    accountSummary: AccountSummary? = null,
) {
    BitwardenTwoButtonDialog(
        title = stringResource(id = R.string.remove_account),
        message = removalConfirmationMessage(accountSummary = accountSummary),
        confirmButtonText = stringResource(id = R.string.yes),
        onConfirmClick = onConfirmClick,
        dismissButtonText = stringResource(id = R.string.cancel),
        onDismissClick = onDismissRequest,
        onDismissRequest = onDismissRequest,
    )
}

@Composable
private fun removalConfirmationMessage(accountSummary: AccountSummary?): String {
    val baseConfirmationMessage = stringResource(id = R.string.remove_account_confirmation)
    return accountSummary
        ?.let { "$baseConfirmationMessage\n\n${it.email}\n${it.environmentLabel}" }
        ?: baseConfirmationMessage
}
