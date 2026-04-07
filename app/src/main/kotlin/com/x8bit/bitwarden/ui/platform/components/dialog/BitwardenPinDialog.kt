package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled dialog for the user to enter their PIN.
 *
 * @param onConfirmClick called when the confirm button is clicked and emits the entered PIN.
 * @param onDismissRequest called when the user attempts to dismiss the dialog (for example by
 * tapping outside of it).
 */
@Composable
fun BitwardenPinDialog(
    onConfirmClick: (pin: String) -> Unit,
    onDismissRequest: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        dismissButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.cancel),
                onClick = onDismissRequest,
                modifier = Modifier.testTag("DismissAlertButton"),
            )
        },
        confirmButton = {
            BitwardenTextButton(
                label = stringResource(id = BitwardenString.submit),
                isEnabled = pin.isNotEmpty(),
                onClick = { onConfirmClick(pin) },
                modifier = Modifier.testTag("AcceptAlertButton"),
            )
        },
        title = {
            Text(
                text = stringResource(id = BitwardenString.verify_pin),
                style = BitwardenTheme.typography.headlineSmall,
                modifier = Modifier.testTag("AlertTitleText"),
            )
        },
        text = {
            BitwardenPasswordField(
                label = stringResource(id = BitwardenString.pin),
                value = pin,
                onValueChange = { pin = it },
                autoFocus = true,
                passwordFieldTestTag = "AlertInputField",
                cardStyle = CardStyle.Full,
                modifier = Modifier.fillMaxWidth(),
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
