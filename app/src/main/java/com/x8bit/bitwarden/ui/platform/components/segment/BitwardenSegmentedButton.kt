package com.x8bit.bitwarden.ui.platform.components.segment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.components.segment.color.bitwardenSegmentedButtonColors
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays a Bitwarden styled row of segmented buttons.
 *
 * @param options List of options to display.
 * @param modifier Modifier.
 * @param windowInsets The insets to be applied to this composable.
 */
@Composable
fun BitwardenSegmentedButton(
    options: ImmutableList<SegmentedButtonState>,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout
        .union(WindowInsets.navigationBars)
        .only(WindowInsetsSides.Horizontal),
) {
    if (options.isEmpty()) return
    Box(
        modifier = modifier
            .background(color = BitwardenTheme.colorScheme.background.secondary)
            .padding(top = 4.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            .windowInsetsPadding(insets = windowInsets),
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BitwardenTheme.colorScheme.background.primary,
                    shape = BitwardenTheme.shapes.segmentedControl,
                )
                .padding(horizontal = 4.dp),
            space = 0.dp,
        ) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    enabled = option.isEnabled,
                    selected = option.isChecked,
                    onClick = option.onClick,
                    colors = bitwardenSegmentedButtonColors(),
                    shape = BitwardenTheme.shapes.segmentedControl,
                    border = BorderStroke(width = 0.dp, color = Color.Transparent),
                    label = {
                        Text(
                            text = option.text,
                            style = BitwardenTheme.typography.labelLarge,
                        )
                    },
                    icon = {
                        // No icon required
                    },
                    modifier = Modifier.semantics { option.testTag?.let { testTag = it } },
                )
            }
        }
    }
}

/**
 * Models state for an individual button in a [BitwardenSegmentedButton].
 */
data class SegmentedButtonState(
    val text: String,
    val onClick: () -> Unit,
    val isChecked: Boolean,
    val isEnabled: Boolean = true,
    val testTag: String? = null,
)
