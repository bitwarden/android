package com.x8bit.bitwarden.ui.platform.components.dialog

import android.os.Parcelable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.parcelize.Parcelize

/**
 * Represents a Bitwarden-styled dialog that is hidden or shown based on [visibilityState].
 *
 * @param visibilityState the [BasicDialogState] used to populate the dialog.
 * @param onDismissRequest called when the user has requested to dismiss the dialog, whether by
 * tapping "OK", tapping outside the dialog, or pressing the back button.
 */
@OptIn(ExperimentalComposeUiApi::class)
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
                    modifier = Modifier.testTag("AcceptAlertButton"),
                )
            },
            title = visibilityState.title?.let {
                {
                    Text(
                        text = it(),
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.testTag("AlertTitleText"),
                    )
                }
            },
            text = {
                Text(
                    text = visibilityState.message(),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("AlertContentText"),
                )
            },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
                testTag = "AlertPopup"
            },
        )
    }
}

@Preview
@Composable
private fun BitwardenBasicDialog_preview() {
    BitwardenTheme {
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
