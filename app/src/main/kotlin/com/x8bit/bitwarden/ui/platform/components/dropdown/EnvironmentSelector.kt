package com.x8bit.bitwarden.ui.platform.components.dropdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.displayLabel

/**
 * A dropdown selector UI component specific to region url selection.
 *
 * This composable displays a dropdown menu allowing users to select a region
 * from a list of options. When an option is selected, it invokes the provided callback
 * and displays the currently selected region on the UI.
 *
 * @param labelText The text displayed near the selector button.
 * @param dialogTitle The title displayed in the selection dialog.
 * @param selectedOption The currently selected environment option.
 * @param onOptionSelected A callback that gets invoked when an environment option is selected
 * and passes the selected option as an argument.
 * @param onHelpClick A callback that gets invoked when the help button is clicked.
 * @param modifier A [Modifier] for the composable.
 * @param isHelpEnabled Indicates if the help button should be available.
 */
@Suppress("LongMethod")
@Composable
fun EnvironmentSelector(
    labelText: String,
    dialogTitle: String,
    selectedOption: Environment.Type,
    onOptionSelected: (Environment.Type) -> Unit,
    onHelpClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHelpEnabled: Boolean = true,
) {
    val options = Environment.Type.entries.toTypedArray()
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 48.dp)
            .clickable(
                indication = ripple(color = BitwardenTheme.colorScheme.background.pressed),
                interactionSource = remember { MutableInteractionSource() },
                onClick = { shouldShowDialog = !shouldShowDialog },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = labelText,
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier.padding(end = 4.dp),
            )
            Text(
                text = selectedOption.displayLabel(),
                style = BitwardenTheme.typography.labelMedium,
                color = BitwardenTheme.colorScheme.text.interaction,
                modifier = Modifier.padding(end = 8.dp),
            )
            Icon(
                painter = rememberVectorPainter(id = BitwardenDrawable.ic_chevron_down_small),
                contentDescription = stringResource(id = BitwardenString.region),
                tint = BitwardenTheme.colorScheme.icon.secondary,
            )
        }
        Spacer(modifier = Modifier.weight(weight = 1f))
        if (isHelpEnabled) {
            BitwardenStandardIconButton(
                vectorIconRes = BitwardenDrawable.ic_question_circle_small,
                contentDescription = stringResource(BitwardenString.help_with_server_geolocations),
                onClick = onHelpClick,
                contentColor = BitwardenTheme.colorScheme.icon.secondary,
                // Align with design but keep accessible touch target of IconButton.
                modifier = Modifier.offset(x = 16.dp),
            )
        }
    }

    if (shouldShowDialog) {
        BitwardenSelectionDialog(
            title = dialogTitle,
            onDismissRequest = { shouldShowDialog = false },
        ) {
            options.forEach {
                BitwardenSelectionRow(
                    text = it.displayLabel,
                    onClick = {
                        onOptionSelected(it)
                        shouldShowDialog = false
                    },
                    isSelected = it == selectedOption,
                )
            }
        }
    }
}
