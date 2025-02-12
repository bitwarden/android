package com.bitwarden.authenticator.ui.authenticator.feature.manualcodeentry

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
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
 */
@Composable
fun SaveManualCodeButtons(
    state: ManualCodeEntryState.ButtonState,
    onSaveLocallyClick: () -> Unit,
    onSaveToBitwardenClick: () -> Unit,
) {

    when (state) {
        ManualCodeEntryState.ButtonState.LocalOnly -> {
            BitwardenFilledTonalButton(
                label = stringResource(id = R.string.add_code),
                onClick = onSaveLocallyClick,
                modifier = Modifier
                    .semantics { testTag = "AddCodeButton" }
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            )
        }

        ManualCodeEntryState.ButtonState.SaveLocallyPrimary -> {
            Column {
                BitwardenFilledButton(
                    label = stringResource(id = R.string.save_here),
                    onClick = onSaveLocallyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                BitwardenOutlinedButton(
                    label = stringResource(R.string.save_to_bitwarden),
                    onClick = onSaveToBitwardenClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }

        ManualCodeEntryState.ButtonState.SaveToBitwardenPrimary -> {
            Column {
                BitwardenFilledButton(
                    label = stringResource(id = R.string.save_to_bitwarden),
                    onClick = onSaveToBitwardenClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
                BitwardenOutlinedButton(
                    label = stringResource(R.string.save_here),
                    onClick = onSaveLocallyClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                )
            }
        }
    }
}
