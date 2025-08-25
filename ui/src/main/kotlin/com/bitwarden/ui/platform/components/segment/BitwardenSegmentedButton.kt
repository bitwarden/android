package com.bitwarden.ui.platform.components.segment

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SingleChoiceSegmentedButtonRowScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.base.util.toDp
import com.bitwarden.ui.platform.base.util.toMaxScale
import com.bitwarden.ui.platform.components.segment.color.bitwardenSegmentedButtonColors
import com.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays a Bitwarden styled row of segmented buttons.
 *
 * @param options List of options to display.
 * @param modifier Modifier.
 * @param windowInsets The insets to be applied to this composable.
 * @param optionContent For outer context the content lambda passes back the index of the option,
 * the weighted width (in [Dp]) per option (total width / # number of options), and the
 * corresponding [SegmentedButtonState].
 */
@Composable
fun BitwardenSegmentedButton(
    options: ImmutableList<SegmentedButtonState>,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout
        .union(WindowInsets.navigationBars)
        .only(WindowInsetsSides.Horizontal),
    density: Density = LocalDensity.current,
    optionContent: @Composable SingleChoiceSegmentedButtonRowScope.(
        Int,
        Dp,
        SegmentedButtonState,
    ) -> Unit = { _, _, optionState ->
        this.SegmentedButtonOptionContent(
            option = optionState,
        )
    },
) {
    if (options.isEmpty()) return
    Box(
        modifier = modifier
            .background(color = BitwardenTheme.colorScheme.background.secondary)
            .padding(top = 4.dp, bottom = 8.dp, start = 16.dp, end = 16.dp)
            .windowInsetsPadding(insets = windowInsets),
    ) {
        var weightedWidth by remember {
            mutableStateOf(0.dp)
        }
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = BitwardenTheme.colorScheme.background.primary,
                    shape = BitwardenTheme.shapes.segmentedControl,
                )
                .onGloballyPositioned {
                    weightedWidth = (it.size.width / options.size).toDp(density)
                }
                .padding(horizontal = 2.dp, vertical = 2.dp)
                .height(IntrinsicSize.Min),
            space = 0.dp,
        ) {
            options.forEachIndexed { index, option ->
                optionContent(index, weightedWidth, option)
            }
        }
    }
}

/**
 * Default content definition for each option in a [BitwardenSegmentedButton].
 */
@Composable
fun SingleChoiceSegmentedButtonRowScope.SegmentedButtonOptionContent(
    option: SegmentedButtonState,
    modifier: Modifier = Modifier,
) {
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
                style = BitwardenTheme.typography.labelLarge
                    .copy(lineBreak = LineBreak.Heading)
                    .toMaxScale(maxScaleFactor = 2f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        },
        icon = {
            // No icon required
        },
        modifier = modifier.nullableTestTag(tag = option.testTag),
    )
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
