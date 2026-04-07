package com.x8bit.bitwarden.ui.platform.components.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled dialog for entering client certificate password and alias.
 *
 * @param onConfirmClick called when the confirm button is clicked and emits the input values.
 * @param onDismissRequest called when the user attempts to dismiss the dialog (for example by
 * tapping outside of it).
 */
@Suppress("LongMethod")
@Composable
fun BitwardenClientCertificateDialog(
    onConfirmClick: (alias: String, password: String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var alias by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
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
                isEnabled = password.isNotEmpty(),
                onClick = { onConfirmClick(alias, password) },
                modifier = Modifier.testTag("AcceptAlertButton"),
            )
        },
        title = {
            Text(
                text = stringResource(BitwardenString.import_client_certificate),
                style = BitwardenTheme.typography.headlineSmall,
                modifier = Modifier.testTag("AlertTitleText"),
            )
        },
        text = {
            Column {
                Text(
                    text = stringResource(
                        BitwardenString.enter_the_client_certificate_password_and_alias,
                    ),
                    style = BitwardenTheme.typography.bodyMedium,
                    modifier = Modifier.testTag("AlertContentText"),
                )

                Spacer(modifier = Modifier.height(24.dp))

                BitwardenTextField(
                    label = stringResource(BitwardenString.alias),
                    value = alias,
                    onValueChange = { alias = it },
                    autoFocus = true,
                    cardStyle = CardStyle.Top(dividerPadding = 0.dp),
                    textFieldTestTag = "AlertClientCertificateAliasInputField",
                    modifier = Modifier.fillMaxWidth(),
                )

                BitwardenPasswordField(
                    label = stringResource(BitwardenString.password),
                    value = password,
                    onValueChange = { password = it },
                    cardStyle = CardStyle.Bottom,
                    passwordFieldTestTag = "AlertClientCertificatePasswordInputField",
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        shape = BitwardenTheme.shapes.dialog,
        containerColor = BitwardenTheme.colorScheme.background.primary,
        iconContentColor = BitwardenTheme.colorScheme.icon.secondary,
        titleContentColor = BitwardenTheme.colorScheme.text.primary,
        textContentColor = BitwardenTheme.colorScheme.text.primary,
        modifier = modifier.semantics {
            testTagsAsResourceId = true
            testTag = "AlertPopup"
        },
    )
}

@Preview(showBackground = true)
@PreviewScreenSizes
@Composable
private fun BitwardenClientCertificateDialogPreview() {
    BitwardenTheme {
        BitwardenClientCertificateDialog(
            onConfirmClick = { alias, password -> },
            onDismissRequest = {},
        )
    }
}
