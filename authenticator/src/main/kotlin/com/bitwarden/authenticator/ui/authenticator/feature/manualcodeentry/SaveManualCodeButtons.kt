package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.bitwarden.authenticator.R
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledButton
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenFilledTonalButton
import com.bitwarden.authenticator.ui.platform.components.button.BitwardenOutlinedButton

/**
 * Displays save buttons for saving a manually entered code.
 *
 * @param state State of the buttons to show.
 * @param onSaveLocallyClick Callback invoked when the user clicks save locally.
 * @param onSaveToBitwardenClick Callback invoked when the user clicks save to Bitwarden.
 * @param modifier The modifier to be applied to the composable.
 */
@Composable
fun SaveManualCodeButtons(
    state: ManualCodeEntryState.ButtonState,
    onSaveLocallyClick: () -> Unit,
    onSaveToBitwardenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (state) {
        ManualCodeEntryState.ButtonState.LocalOnly -> {
            BitwardenFilledTonalButton(
                label = stringResource(id = R.string.add_code),
                onClick = onSaveLocallyClick,
                modifier = modifier.testTag(tag = "AddCodeButton"),
            )
        }

        ManualCodeEntryState.ButtonState.SaveLocallyPrimary -> {
            Column(modifier = modifier) {
                BitwardenFilledButton(
                    label = stringResource(id = R.string.save_here),
                    onClick = onSaveLocallyClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                BitwardenOutlinedButton(
                    label = stringResource(R.string.save_to_bitwarden),
                    onClick = onSaveToBitwardenClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        ManualCodeEntryState.ButtonState.SaveToBitwardenPrimary -> {
            Column(modifier = modifier) {
                BitwardenFilledButton(
                    label = stringResource(id = R.string.save_to_bitwarden),
                    onClick = onSaveToBitwardenClick,
                    modifier = Modifier.fillMaxWidth(),
                )
                BitwardenOutlinedButton(
                    label = stringResource(R.string.save_here),
                    onClick = onSaveLocallyClick,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
