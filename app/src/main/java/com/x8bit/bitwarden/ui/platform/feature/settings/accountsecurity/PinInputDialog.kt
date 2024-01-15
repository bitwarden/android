package com.x8bit.bitwarden.ui.platform.feature.settings.accountsecurity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.BitwardenFilledButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextButton
import com.x8bit.bitwarden.ui.platform.components.BitwardenTextField
import com.x8bit.bitwarden.ui.platform.components.util.maxDialogHeight

/**
 * A dialog for setting a user's PIN.
 *
 * @param pin The current value of the PIN.
 * @param onPinChange A callback for internal changes to the PIN.
 * @param onCancelClick A callback for when the "Cancel" button is clicked.
 * @param onSubmitClick A callback for when the "Submit" button is clicked.
 * @param onDismissRequest A callback for when the dialog is requesting to be dismissed.
 */
@Suppress("LongMethod")
@Composable
fun PinInputDialog(
    pin: String,
    onPinChange: (String) -> Unit,
    onCancelClick: () -> Unit,
    onSubmitClick: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        val configuration = LocalConfiguration.current
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .requiredHeightIn(
                    max = configuration.maxDialogHeight,
                )
                // This background is necessary for the dialog to not be transparent.
                .background(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(28.dp),
                ),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                text = stringResource(id = R.string.enter_pin),
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.headlineSmall,
            )
            if (scrollState.canScrollBackward) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(scrollState),
            ) {
                Text(
                    modifier = Modifier
                        .padding(24.dp)
                        .fillMaxWidth(),
                    text = stringResource(id = R.string.set_pin_description),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium,
                )

                BitwardenTextField(
                    label = stringResource(id = R.string.pin),
                    value = pin,
                    onValueChange = onPinChange,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                )
            }
            if (scrollState.canScrollForward) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(24.dp),
            ) {
                BitwardenTextButton(
                    label = stringResource(id = R.string.cancel),
                    onClick = onCancelClick,
                )

                BitwardenFilledButton(
                    label = stringResource(id = R.string.submit),
                    onClick = onSubmitClick,
                )
            }
        }
    }
}
