package com.bitwarden.authenticator.ui.authenticator.feature.qrcodescan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeightIn
import androidx.compose.foundation.layout.requiredWidthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.components.button.BitwardenTextButton
import com.bitwarden.ui.platform.components.dialog.util.maxDialogHeight
import com.bitwarden.ui.platform.components.dialog.util.maxDialogWidth
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.toggle.BitwardenSwitch
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays a dialog asking the user where they would like to save a new QR code.
 *
 * @param onSaveHereClick Invoked when the user clicks "Save here". The boolean parameter is true if
 * the user check "Save option as default".
 * @param onTakeMeToBitwardenClick Invoked when the user clicks "Take me to Bitwarden". The boolean
 * parameter is true if the user checked "Save option as default".
 */
@Composable
@Suppress("LongMethod")
fun ChooseSaveLocationDialog(
    onSaveHereClick: (Boolean) -> Unit,
    onTakeMeToBitwardenClick: (Boolean) -> Unit,
) {
    Dialog(
        onDismissRequest = { }, // Not dismissible
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        var isSaveAsDefaultChecked by remember { mutableStateOf(false) }
        val configuration = LocalConfiguration.current
        Column(
            modifier = Modifier
                .requiredHeightIn(
                    max = configuration.maxDialogHeight,
                )
                .requiredWidthIn(
                    max = configuration.maxDialogWidth,
                )
                .background(
                    color = BitwardenTheme.colorScheme.background.primary,
                    shape = BitwardenTheme.shapes.dialog,
                ),
            horizontalAlignment = Alignment.End,
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                text = stringResource(BitwardenString.verification_code_created),
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.headlineSmall,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .padding(horizontal = 24.dp)
                    .fillMaxWidth(),
                text = stringResource(BitwardenString.choose_save_location_message),
                color = BitwardenTheme.colorScheme.text.primary,
                style = BitwardenTheme.typography.bodyMedium,
            )
            Spacer(Modifier.height(16.dp))
            BitwardenSwitch(
                modifier = Modifier
                    .fillMaxWidth()
                    .standardHorizontalMargin(),
                label = stringResource(BitwardenString.save_option_as_default),
                isChecked = isSaveAsDefaultChecked,
                onCheckedChange = { isSaveAsDefaultChecked = !isSaveAsDefaultChecked },
                cardStyle = CardStyle.Full,
            )
            Spacer(Modifier.height(16.dp))
            FlowRow(
                horizontalArrangement = Arrangement.End,
                modifier = Modifier.padding(horizontal = 8.dp),
            ) {
                BitwardenTextButton(
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                    label = stringResource(BitwardenString.save_here),
                    onClick = { onSaveHereClick.invoke(isSaveAsDefaultChecked) },
                )
                BitwardenTextButton(
                    modifier = Modifier
                        .padding(horizontal = 4.dp),
                    label = stringResource(BitwardenString.save_to_bitwarden),
                    onClick = { onTakeMeToBitwardenClick.invoke(isSaveAsDefaultChecked) },
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
