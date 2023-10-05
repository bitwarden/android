package com.x8bit.bitwarden.ui.platform.components

import android.os.Parcelable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
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
            title = {
                Text(
                    text = visibilityState.title(),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.headlineSmall,
                )
            },
            text = {
                Text(
                    text = visibilityState.message(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
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
        val title: Text,
        val message: Text,
    ) : BasicDialogState()
}
