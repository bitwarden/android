package com.bitwarden.authenticator.ui.platform.components.toggle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * Represents a Bitwarden-styled [Switch].
 *
 * @param label The label for the switch.
 * @param isChecked Whether or not the switch is currently checked.
 * @param onCheckedChange A callback for when the checked state changes.
 * @param modifier The [Modifier] to be applied to the button.
 * @param description The description of the switch to be displayed below the [label].
 */
@Composable
fun BitwardenSwitch(
    label: String,
    isChecked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    Row(
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .run {
                if (onCheckedChange != null) {
                    this.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(color = MaterialTheme.colorScheme.primary),
                        onClick = { onCheckedChange.invoke(!isChecked) },
                    )
                } else {
                    this
                }
            }
            .semantics(mergeDescendants = true) {
                toggleableState = ToggleableState(isChecked)
            }
            .then(modifier),
    ) {
        Switch(
            modifier = Modifier
                .padding(vertical = 8.dp)
                .height(32.dp)
                .width(52.dp),
            checked = isChecked,
            onCheckedChange = null,
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )

            description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenSwitch_preview_isChecked() {
    BitwardenSwitch(
        label = "Label",
        description = "Description",
        isChecked = true,
        onCheckedChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}

@Preview(showBackground = true)
@Composable
private fun BitwardenSwitch_preview_isNotChecked() {
    BitwardenSwitch(
        label = "Label",
        isChecked = false,
        onCheckedChange = {},
        modifier = Modifier.fillMaxWidth(),
    )
}
