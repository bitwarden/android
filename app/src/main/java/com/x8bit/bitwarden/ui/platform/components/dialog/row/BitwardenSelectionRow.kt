package com.x8bit.bitwarden.ui.platform.components.dialog.row

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.components.radio.BitwardenRadioButton
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A clickable item that displays a radio button and text.
 *
 * @param text The text to display.
 * @param onClick Invoked when either the radio button or text is clicked.
 * @param isSelected Whether or not the radio button should be checked.
 */
@Composable
fun BitwardenSelectionRow(
    text: Text,
    onClick: () -> Unit,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("AlertRadioButtonOption")
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) {
                selected = isSelected
            },
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
