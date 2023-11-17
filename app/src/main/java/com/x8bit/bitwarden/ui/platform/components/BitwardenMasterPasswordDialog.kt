package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R

/**
 * Represents a Bitwarden-styled dialog for entering your master password.
 *
 * @param onConfirmClick called when the confirm button is clicked and emits the entered password.
 * @param onDismissRequest called when the user attempts to dismiss the dialog (for example by
 * tapping outside of it).
 */
@Composable
fun BitwardenMasterPasswordDialog(
    onConfirmClick: (masterPassword: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var masterPassword by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = stringResource(id = R.string.cancel),
                onClick = onDismissRequest,
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = stringResource(id = R.string.submit),
                isEnabled = masterPassword.isNotEmpty(),
                onClick = { onConfirmClick(masterPassword) },
            )
        },
        title = {
            Text(
                text = stringResource(id = R.string.password_confirmation),
                style = MaterialTheme.typography.headlineSmall,
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(id = R.string.password_confirmation_desc),
                    style = MaterialTheme.typography.bodyMedium,
                )

                Spacer(modifier = Modifier.height(8.dp))

                BitwardenPasswordField(
                    label = stringResource(id = R.string.master_password),
                    value = masterPassword,
                    onValueChange = { masterPassword = it },
                    modifier = Modifier.imePadding(),
                    autoFocus = true,
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
    )
}
