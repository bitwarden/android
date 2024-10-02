package com.x8bit.bitwarden.ui.platform.components.row

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a clickable row of text and can contains an optional [content] that appears to the
 * right of the [text].
 *
 * @param text The label for the row as a [String].
 * @param onClick The callback when the row is clicked.
 * @param modifier The modifier to be applied to the layout.
 * @param description An optional description label to be displayed below the [text].
 * @param isEnabled Indicates if the row is enabled or not, a disabled row will not be clickable
 * and it's contents will be dimmed.
 * @param withDivider Indicates if a divider should be drawn on the bottom of the row, defaults
 * to `false`.
 * @param content The content of the [BitwardenTextRow].
 */
@Composable
fun BitwardenTextRow(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    description: String? = null,
    isEnabled: Boolean = true,
    withDivider: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    Box(
        contentAlignment = Alignment.BottomCenter,
        modifier = modifier
            .clickable(
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                onClick = onClick,
            )
            .semantics(mergeDescendants = true) { },
    ) {
        Row(
            modifier = Modifier
                .defaultMinSize(minHeight = 56.dp)
                .padding(start = 16.dp, end = 24.dp, top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier
                    .padding(end = 16.dp)
                    .weight(1f),
            ) {
                Text(
                    text = text,
                    style = BitwardenTheme.typography.bodyLarge,
                    color = if (isEnabled) {
                        BitwardenTheme.colorScheme.text.primary
                    } else {
                        BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                    },
                )
                description?.let {
                    Text(
                        text = it,
                        style = BitwardenTheme.typography.bodyMedium,
                        color = if (isEnabled) {
                            BitwardenTheme.colorScheme.text.secondary
                        } else {
                            BitwardenTheme.colorScheme.filledButton.foregroundDisabled
                        },
                    )
                }
            }
            content?.invoke()
        }
        if (withDivider) {
            BitwardenHorizontalDivider(modifier = Modifier.padding(start = 16.dp))
        }
    }
}
