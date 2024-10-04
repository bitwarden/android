package com.x8bit.bitwarden.ui.platform.components.dialog.row

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A simple clickable row for use within a [BitwardenSelectionDialog] as an alternative to a
 * [BitwardenSelectionRow].
 *
 * @param text The text to display in the row.
 * @param onClick A callback to be invoked when the row is clicked.
 * @param modifier A [Modifier] for the composable.
 */
@Composable
fun BitwardenBasicDialogRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = BitwardenTheme.typography.bodyLarge,
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                onClick = onClick,
            )
            .padding(
                vertical = 16.dp,
                horizontal = 24.dp,
            )
            .fillMaxWidth(),
    )
}
