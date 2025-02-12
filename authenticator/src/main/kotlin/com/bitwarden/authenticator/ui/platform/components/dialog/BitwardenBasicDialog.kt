package com.bitwarden.authenticator.ui.platform.components.dialog

import android.os.Parcelable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.base.util.Text
import com.bitwarden.authenticator.ui.platform.base.util.asText
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.authenticator.ui.platform.theme.AuthenticatorTheme
import kotlinx.parcelize.Parcelize

/**
 * Represents a Bitwarden-styled dialog that is hidden or shown based on [visibilityState].
 *
 * @param visibilityState the [BasicDialogState] used to populate the dialog.
 * @param onDismissRequest called when the user has requested to dismiss the dialog, whether by
 * tapping "OK", tapping outside the dialog, or pressing the back button.
 */
@Composable
fun BitwardenBasicDialog(
    visibilityState: BasicDialogState,
    onDismissRequest: () -> Unit,
): Unit = when (visibilityState) {
    BasicDialogState.Hidden -> Unit
    is BasicDialogState.Shown -> {
        AlertDialog(
            onDismissRequest = onDismissRequest,
            confirmButton = {
                BitwardenTextButton(
                    label = stringResource(id = R.string.ok),
                    onClick = onDismissRequest,
                )
            },
            title = visibilityState.title?.let {
                {
                    Text(
                        text = it(),
                        style = MaterialTheme.typography.headlineSmall,
                    )
                }
            },
            text = {
                Text(
                    text = visibilityState.message(),
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        )
    }
}

@Preview
@Composable
private fun BitwardenBasicDialog_preview() {
    AuthenticatorTheme {
        BitwardenBasicDialog(
            visibilityState = BasicDialogState.Shown(
                title = "An error has occurred.".asText(),
                message = "Username or password is incorrect. Try again.".asText(),
            ),
            onDismissRequest = {},
        )
    }
}

/**
 * Models display of a [BitwardenBasicDialog].
 */
sealed class BasicDialogState : Parcelable {

    /**
     * Hide the dialog.
     */
    @Parcelize
    data object Hidden : BasicDialogState()

    /**
     * Show the dialog with the given values.
     */
    @Parcelize
    data class Shown(
        val title: Text?,
        val message: Text,
    ) : BasicDialogState()
}
