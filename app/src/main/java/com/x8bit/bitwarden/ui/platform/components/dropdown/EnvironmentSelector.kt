package com.x8bit.bitwarden.ui.platform.components.dropdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import com.x8bit.bitwarden.ui.platform.util.displayLabel

/**
 * A dropdown selector UI component specific to region url selection.
 *
 * This composable displays a dropdown menu allowing users to select a region
 * from a list of options. When an option is selected, it invokes the provided callback
 * and displays the currently selected region on the UI.
 *
 * @param labelText The text displayed near the selector button.
 * @param selectedOption The currently selected environment option.
 * @param onOptionSelected A callback that gets invoked when an environment option is selected
 * and passes the selected option as an argument.
 * @param modifier A [Modifier] for the composable.
 *
 */
@Composable
fun EnvironmentSelector(
    labelText: String,
    selectedOption: Environment.Type,
    onOptionSelected: (Environment.Type) -> Unit,
    modifier: Modifier = Modifier,
) {
    val options = Environment.Type.entries.toTypedArray()
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(28.dp))
                .clickable(
                    indication = ripple(
                        bounded = true,
                        color = BitwardenTheme.colorScheme.background.pressed,
                    ),
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = { shouldShowDialog = !shouldShowDialog },
                )
                .padding(
                    vertical = 8.dp,
                    horizontal = 16.dp,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = labelText,
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.primary,
                modifier = Modifier.padding(end = 12.dp),
            )
            Text(
                text = selectedOption.displayLabel(),
                style = BitwardenTheme.typography.labelLarge,
                color = BitwardenTheme.colorScheme.text.interaction,
                modifier = Modifier.padding(end = 8.dp),
            )
            Icon(
                painter = rememberVectorPainter(id = R.drawable.ic_chevron_down_small),
                contentDescription = stringResource(id = R.string.region),
                tint = BitwardenTheme.colorScheme.icon.secondary,
            )
        }

        if (shouldShowDialog) {
            BitwardenSelectionDialog(
                title = stringResource(id = R.string.logging_in_on),
                onDismissRequest = { shouldShowDialog = false },
            ) {
                options.forEach {
                    BitwardenSelectionRow(
                        text = it.displayLabel,
                        onClick = {
                            onOptionSelected.invoke(it)
                            shouldShowDialog = false
                        },
                        isSelected = it == selectedOption,
                    )
                }
            }
        }
    }
}
