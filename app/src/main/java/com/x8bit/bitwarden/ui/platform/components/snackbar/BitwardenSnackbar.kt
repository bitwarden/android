package com.x8bit.bitwarden.ui.platform.components.snackbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenOutlinedButton
import com.x8bit.bitwarden.ui.platform.components.button.color.bitwardenOutlinedButtonColors
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Custom snackbar for Bitwarden.
 * Shows a message with an optional actions and title.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenSnackbar(
    bitwardenSnackbarData: BitwardenSnackbarData,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onActionClick: () -> Unit = {},
) {
    Box(
        modifier = modifier.padding(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BitwardenTheme.colorScheme.background.alert,
                    shape = BitwardenTheme.shapes.snackbar,
                )
                // I there is no explicit dismiss action, the Snackbar can be dismissed by clicking
                // anywhere on the Snackbar.
                .clickable(
                    enabled = !bitwardenSnackbarData.withDismissAction,
                    onClick = onDismiss,
                )
                .padding(16.dp),
        ) {
            Column {
                bitwardenSnackbarData.messageHeader?.let {
                    Text(
                        text = it(),
                        color = BitwardenTheme.colorScheme.text.reversed,
                        style = BitwardenTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(
                    text = bitwardenSnackbarData.message(),
                    color = BitwardenTheme.colorScheme.text.reversed,
                    style = BitwardenTheme.typography.bodyMedium,
                )
                bitwardenSnackbarData.actionLabel?.let {
                    Spacer(Modifier.height(12.dp))
                    BitwardenOutlinedButton(
                        label = it(),
                        onClick = onActionClick,
                        colors = bitwardenOutlinedButtonColors(
                            contentColor = BitwardenTheme.colorScheme.text.reversed,
                            outlineColor = BitwardenTheme
                                .colorScheme
                                .outlineButton
                                .borderReversed,
                        ),
                    )
                }
            }
            if (bitwardenSnackbarData.withDismissAction) {
                Spacer(Modifier.weight(1f))
                IconButton(
                    onClick = onDismiss,
                    content = {
                        Icon(
                            rememberVectorPainter(R.drawable.ic_close),
                            contentDescription = stringResource(R.string.close),
                            tint = BitwardenTheme.colorScheme.icon.reversed,
                        )
                    },
                )
            }
        }
    }
}

@Preview
@Composable
private fun BitwardenCustomSnackbar_preview() {
    BitwardenTheme {
        Surface {
            BitwardenSnackbar(
                BitwardenSnackbarData(
                    messageHeader = "Header".asText(),
                    message = "Message".asText(),
                    actionLabel = "Action".asText(),
                    withDismissAction = true,
                ),
            )
        }
    }
}
