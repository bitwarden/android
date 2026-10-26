package com.bitwarden.ui.platform.components.header

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A Bitwarden-styled list section header that can be clicked to expand or collapse the content
 * below it.
 *
 * @param label The text content for the header, displayed in uppercase.
 * @param isExpanded Whether the section content is currently expanded.
 * @param onClick Invoked when the header is clicked.
 * @param modifier The [Modifier] to be applied to the header.
 * @param supportingLabel The optional text displayed at the end of the header, before the
 * expansion indicator.
 * @param shape The [Shape] used to clip the header and draw the interactive border.
 * @param interactionSource A [MutableInteractionSource] for observing and emitting interactions
 * for this component.
 */
@Composable
fun BitwardenExpandingListHeader(
    label: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingLabel: String? = null,
    shape: Shape = BitwardenTheme.shapes.content,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    ExpandingHeaderRow(
        isExpanded = isExpanded,
        onClick = onClick,
        shape = shape,
        insets = PaddingValues(),
        showExpansionIndicator = true,
        expansionIndicatorColor = BitwardenTheme.colorScheme.text.secondary,
        interactionSource = interactionSource,
        modifier = modifier,
    ) {
        Text(
            text = label.toUpperCase(locale = LocalLocale.current),
            style = BitwardenTheme.typography.eyebrowMedium,
            color = BitwardenTheme.colorScheme.text.secondary,
            modifier = Modifier.weight(weight = 1f),
        )
        supportingLabel?.let {
            Spacer(modifier = Modifier.width(width = 8.dp))
            Text(
                text = it,
                style = BitwardenTheme.typography.labelMedium,
                color = BitwardenTheme.colorScheme.text.secondary,
            )
        }
        Spacer(modifier = Modifier.width(width = 8.dp))
    }
}

@Preview(showBackground = true)
@Composable
private fun BitwardenExpandingListHeader_preview() {
    BitwardenTheme {
        Column {
            var isExpanded by remember { mutableStateOf(value = true) }
            BitwardenExpandingListHeader(
                label = "Item types",
                supportingLabel = "8",
                isExpanded = isExpanded,
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.fillMaxWidth(),
            )
            BitwardenExpandingListHeader(
                label = "No folder",
                isExpanded = false,
                onClick = {},
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
