package com.x8bit.bitwarden.ui.platform.components.segment

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import kotlinx.collections.immutable.ImmutableList

/**
 * Displays a Bitwarden styled row of segmented buttons.
 *
 * @param options List of options to display.
 * @param modifier Modifier.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BitwardenSegmentedButton(
    modifier: Modifier = Modifier,
    options: ImmutableList<SegmentedButtonState>,
) {
    MultiChoiceSegmentedButtonRow(
        modifier = modifier,
    ) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                checked = option.isChecked,
                onCheckedChange = { option.onClick() },
                shape = SegmentedButtonDefaults.itemShape(
                    index = index,
                    count = options.size,
                ),
                label = { Text(text = option.text) },
                modifier = Modifier.run {
                    option.testTag?.let { testTag(it) } ?: this
                },
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
