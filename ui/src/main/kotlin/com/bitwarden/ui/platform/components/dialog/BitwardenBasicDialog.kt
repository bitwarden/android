package com.bitwarden.ui.platform.components.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.composition.LocalIntentManager
import com.bitwarden.ui.platform.manager.IntentManager
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled dialog.
 *
 * @param title The optional title to be displayed by the dialog.
 * @param message The message to be displayed under the [title] by the dialog.
 * @param throwable An optional [Throwable] that can be shared from this dialog.
 * @param onDismissRequest A lambda that is invoked when the user has requested to dismiss the
 * dialog, whether by tapping "OK", tapping outside the dialog, or pressing the back button.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenBasicDialog(
    title: String?,
    message: String,
    onDismissRequest: () -> Unit,
    throwable: Throwable? = null,
    intentManager: IntentManager = LocalIntentManager.current,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        confirmButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.okay),
                onClick = onDismissRequest,
                modifier = Modifier.testTag(tag = "AcceptAlertButton"),
            )
        },
        dismissButton = throwable
            ?.let { error ->
                {
                    BitwardenTextButton(
                        label = stringResource(id = BitwardenString.share_error_details),
                        onClick = {
                            intentManager.shareErrorReport(throwable = error)
                            onDismissRequest()
                        },
                        modifier = Modifier.testTag(tag = "ShareErrorDetailsAlertButton"),
                    )
                }
            },
        title = title?.let {
            {
                Text(
                    text = it,
                    style = BitwardenTheme.typography.headlineSmall,
                    modifier = Modifier.testTag(tag = "AlertTitleText"),
                )
            }
        },
        text = {
            Text(
                text = message,
                style = BitwardenTheme.typography.bodyMedium,
                modifier = Modifier.testTag(tag = "AlertContentText"),
            )
        },
        shape = BitwardenTheme.shapes.dialog,
        containerColor = BitwardenTheme.colorScheme.background.primary,
        iconContentColor = BitwardenTheme.colorScheme.icon.secondary,
        titleContentColor = BitwardenTheme.colorScheme.text.primary,
        textContentColor = BitwardenTheme.colorScheme.text.primary,
        modifier = Modifier.semantics {
            testTagsAsResourceId = true
            testTag = "AlertPopup"
        },
    )
}

@Preview
@Composable
private fun BitwardenBasicDialog_preview() {
    BitwardenTheme {
        BitwardenBasicDialog(
            title = "An error has occurred",
            message = "Username or password is incorrect. Try again.",
            onDismissRequest = {},
        )
    }
}
