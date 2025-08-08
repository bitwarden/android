package com.x8bit.bitwarden.ui.platform.components.dropdown

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.components.button.BitwardenTextSelectionButton
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.components.model.TooltipData
import com.bitwarden.ui.platform.theme.BitwardenTheme
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

/**
 * A custom composable representing a multi-select button.
 *
 * This composable displays an [OutlinedTextField] with a dropdown icon as a trailing icon.
 * When the field is clicked, a dropdown menu appears with a list of options to select from.
 *
 * @param label The descriptive text label for the [OutlinedTextField].
 * @param options A list of strings representing the available options in the dialog.
 * @param selectedOption The currently selected option that is displayed in the [OutlinedTextField]
 * (or `null` if no option is selected).
 * @param onOptionSelected A lambda that is invoked when an option
 * is selected from the dropdown menu.
 * @param isEnabled Whether or not the button is enabled.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param supportingText A optional supporting text that will appear below the text field.
 * @param tooltip A nullable [TooltipData], representing the tooltip icon.
 * @param insets Inner padding to be applied withing the card.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenMultiSelectButton(
    label: String,
    options: ImmutableList<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingText: String? = null,
    tooltip: TooltipData? = null,
    insets: PaddingValues = PaddingValues(),
    textFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable RowScope.() -> Unit = {},
) {
    BitwardenMultiSelectButton(
        label = label,
        options = options.map { MultiSelectOption.Row(it) }.toImmutableList(),
        selectedOption = selectedOption?.let { MultiSelectOption.Row(it) },
        onOptionSelected = { onOptionSelected(it.title) },
        cardStyle = cardStyle,
        modifier = modifier,
        isEnabled = isEnabled,
        supportingText = supportingText,
        supportingContent = supportingText?.let {
            {
                Text(
                    text = it,
                    style = BitwardenTheme.typography.bodySmall,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        tooltip = tooltip,
        insets = insets,
        textFieldTestTag = textFieldTestTag,
        actionsPadding = actionsPadding,
        actions = actions,
    )
}

/**
 * A custom composable representing a multi-select button.
 *
 * This composable displays an [OutlinedTextField] with a dropdown icon as a trailing icon.
 * When the field is clicked, a dropdown menu appears with a list of options to select from.
 *
 * @param label The descriptive text label for the [OutlinedTextField].
 * @param options A list of strings representing the available options in the dialog.
 * @param selectedOption The currently selected option that is displayed in the [OutlinedTextField]
 * (or `null` if no option is selected).
 * @param onOptionSelected A lambda that is invoked when an option
 * is selected from the dropdown menu.
 * @param isEnabled Whether or not the button is enabled.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param supportingContent An optional supporting content composable that will appear below the
 * text input.
 * @param tooltip A nullable [TooltipData], representing the tooltip icon.
 * @param insets Inner padding to be applied withing the card.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Composable
fun BitwardenMultiSelectButton(
    label: String,
    options: ImmutableList<MultiSelectOption>,
    selectedOption: MultiSelectOption.Row?,
    onOptionSelected: (MultiSelectOption.Row) -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingText: String? = null,
    supportingContent: @Composable (ColumnScope.() -> Unit)?,
    tooltip: TooltipData? = null,
    insets: PaddingValues = PaddingValues(),
    textFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    actions: @Composable RowScope.() -> Unit = {},
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    BitwardenTextSelectionButton(
        label = label,
        selectedOption = selectedOption?.title,
        onClick = {
            shouldShowDialog = true
        },
        cardStyle = cardStyle,
        enabled = isEnabled,
        supportingText = supportingText,
        supportingContent = supportingContent,
        tooltip = tooltip,
        insets = insets,
        textFieldTestTag = textFieldTestTag,
        actionsPadding = actionsPadding,
        actions = actions,
        semanticRole = Role.DropdownList,
        modifier = modifier,
    )

    if (shouldShowDialog) {
        BitwardenSelectionDialog(
            title = label,
            onDismissRequest = { shouldShowDialog = false },
        ) {
            BitwardenMultiSelectDialogContent(
                options = options,
                selectedOption = selectedOption,
                onOptionSelected = { selectedItem ->
                    shouldShowDialog = false
                    onOptionSelected(selectedItem)
                },
            )
        }
    }
}

/**
 * Renders the list of items and an optional section within a multi-select dialog.
 *
 * This composable is typically used as the content for [BitwardenSelectionDialog].
 *
 * @param options A list of [MultiSelectOption] representing the available options in the dialog.
 * @param selectedOption The currently selected option that is displayed in the [OutlinedTextField]
 * (or `null` if no option is selected).
 * @param onOptionSelected A lambda that is invoked when an option
 * is selected from the dropdown menu.
 */
@Composable
fun BitwardenMultiSelectDialogContent(
    options: ImmutableList<MultiSelectOption>,
    selectedOption: MultiSelectOption.Row?,
    onOptionSelected: (MultiSelectOption.Row) -> Unit,
) {
    options.forEach {
        when (it) {
            is MultiSelectOption.Header -> {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .nullableTestTag(it.testTag),
                    text = it.title,
                    color = BitwardenTheme.colorScheme.text.secondary,
                    style = BitwardenTheme.typography.titleSmall,
                )
            }

            is MultiSelectOption.Row -> {
                BitwardenSelectionRow(
                    text = it.title.asText(),
                    isSelected = it == selectedOption,
                    onClick = { onOptionSelected(it) },
                )
            }
        }
    }
}

@Preview
@Composable
private fun BitwardenMultiSelectButton_preview() {
    BitwardenTheme {
        BitwardenMultiSelectButton(
            label = "Label",
            options = persistentListOf("a", "b"),
            selectedOption = "",
            onOptionSelected = {},
            cardStyle = CardStyle.Full,
        )
    }
}

/**
 * Represents an option in a multi-select list, which can either be a header or a selectable row.
 *
 * This sealed class is used to define the structure of items within an optional section
 * of a [BitwardenMultiSelectDialogContent].
 */
sealed class MultiSelectOption {
    /**
     * Represents a header item in a multi-select list.
     *
     * Headers are used to visually group related options within the list.
     * They are not selectable.
     *
     * @param title The text to display for the header.
     * @param testTag An optional test tag for UI testing purposes.
     */
    data class Header(
        val title: String,
        val testTag: String? = null,
    ) : MultiSelectOption()

    /**
     * Represents a row item in a multi-select list.
     * Should be preceded by a [Header] item.
     *
     * @param title The text to display for the header.
     */
    data class Row(
        val title: String,
    ) : MultiSelectOption()
}
