package com.x8bit.bitwarden.ui.platform.components.segment

import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays a Bitwarden styled row of segmented buttons.
 *
 * @param options List of options to display.
 * @param modifier Modifier.
 */
@Composable
fun BitwardenSegmentedButton(
    options: ImmutableList<SegmentedButtonState>,
    modifier: Modifier = Modifier,
) {
    SingleChoiceSegmentedButtonRow(
        modifier = modifier,
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option.isChecked,
                onClick = option.onClick,
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                label = { Text(text = option.text) },
                modifier = Modifier.semantics { option.testTag?.let { testTag = it } },
            )
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
    val testTag: String? = null,
)
