package com.bitwarden.authenticator.ui.platform.components.row

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.mirrorIfRtl
import com.bitwarden.authenticator.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.authenticator.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme

/**
 * Represents a row of text that can be clicked on and contains an external link.
 * A confirmation dialog will always be displayed before [onConfirmClick] is invoked.
 *
 * @param text The label for the row as a [String].
 * @param onConfirmClick The callback when the confirm button of the dialog is clicked.
 * @param modifier The modifier to be applied to the layout.
 * @param withDivider Indicates if a divider should be drawn on the bottom of the row, defaults
 * to `true`.
 * @param dialogTitle The title of the dialog displayed when the user clicks this item.
 * @param dialogMessage The message of the dialog displayed when the user clicks this item.
 * @param dialogConfirmButtonText The text on the confirm button of the dialog displayed when the
 * user clicks this item.
 * @param dialogDismissButtonText The text on the dismiss button of the dialog displayed when the
 * user clicks this item.
 */
@Composable
fun BitwardenExternalLinkRow(
    text: String,
    onConfirmClick: () -> Unit,
    modifier: Modifier = Modifier,
    withDivider: Boolean = true,
    dialogTitle: String,
    dialogMessage: String,
    dialogConfirmButtonText: String = stringResource(id = R.string.continue_text),
    dialogDismissButtonText: String = stringResource(id = R.string.cancel),
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }
    BitwardenTextRow(
        text = text,
        onClick = { shouldShowDialog = true },
        modifier = modifier,
        withDivider = withDivider,
    ) {
        Icon(
            modifier = Modifier.mirrorIfRtl(),
            painter = rememberVectorPainter(id = R.drawable.ic_external_link),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
        )
    }

    if (shouldShowDialog) {
        BitwardenTwoButtonDialog(
            title = dialogTitle,
            message = dialogMessage,
            confirmButtonText = dialogConfirmButtonText,
            dismissButtonText = dialogDismissButtonText,
            onConfirmClick = {
                shouldShowDialog = false
                onConfirmClick()
            },
            onDismissClick = { shouldShowDialog = false },
            onDismissRequest = { shouldShowDialog = false },
        )
    }
}

@Preview
@Composable
private fun BitwardenExternalLinkRow_preview() {
    AuthenticatorTheme {
        BitwardenExternalLinkRow(
            text = "Linked Text",
            onConfirmClick = { },
            dialogTitle = "",
            dialogMessage = "",
        )
    }
}
