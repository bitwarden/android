package com.bitwarden.ui.platform.components.dialog.row

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.interactiveBorder
import com.bitwarden.ui.platform.components.radio.BitwardenRadioButton
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.Text

/**
 * A clickable item that displays a radio button and text.
 *
 * @param text The text to display.
 * @param onClick Invoked when either the radio button or text is clicked.
 * @param isSelected Whether the radio button should be checked.
 * @param modifier The modifier for this composable.
 * @param interactionSource A [MutableInteractionSource] for observing and emitting interactions
 * for this component.
 */
@Composable
fun BitwardenSelectionRow(
    text: Text,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("AlertRadioButtonOption")
            .selectable(
                interactionSource = interactionSource,
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton,
            )
            .interactiveBorder(
                interactionSource = interactionSource,
                shape = BitwardenTheme.shapes.row,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BitwardenRadioButton(
            modifier = Modifier.padding(16.dp),
            isSelected = isSelected,
            onClick = null,
        )
        Text(
            text = text(),
            color = BitwardenTheme.colorScheme.text.primary,
            style = BitwardenTheme.typography.bodyLarge,
            modifier = Modifier.testTag("AlertRadioButtonOptionName"),
        )
    }
}
