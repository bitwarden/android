package com.x8bit.bitwarden.ui.platform.components.dropdown

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.cardBackground
import com.x8bit.bitwarden.ui.platform.base.util.cardPadding
import com.x8bit.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenSelectionDialog
import com.x8bit.bitwarden.ui.platform.components.dialog.row.BitwardenSelectionRow
import com.x8bit.bitwarden.ui.platform.components.divider.BitwardenHorizontalDivider
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldButtonColors
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.components.model.TooltipData
import com.x8bit.bitwarden.ui.platform.components.row.BitwardenRowOfActions
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

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
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param supportingText A optional supporting text that will appear below the text field.
 * @param tooltip A nullable [TooltipData], representing the tooltip icon.
 * @param insets Inner padding to be applied withing the card.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the app bar's trailing side. This lambda extends [RowScope], allowing flexibility in
 * defining the layout of the actions.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenMultiSelectButton(
    label: String,
    options: ImmutableList<String>,
    selectedOption: String?,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingText: String? = null,
    tooltip: TooltipData? = null,
    insets: PaddingValues = PaddingValues(),
    cardStyle: CardStyle? = null,
    actionsPadding: PaddingValues = PaddingValues(),
    actions: @Composable RowScope.() -> Unit = {},
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .clearAndSetSemantics {
                role = Role.DropdownList
                contentDescription = supportingText
                    ?.let { "$selectedOption. $label. $it" }
                    ?: "$selectedOption. $label"
                customActions = listOfNotNull(
                    tooltip?.let {
                        CustomAccessibilityAction(
                            label = it.contentDescription,
                            action = {
                                it.onClick()
                                true
                            },
                        )
                    },
                )
            }
            .cardBackground(cardStyle = cardStyle)
            .clickable(
                indication = ripple(
                    color = BitwardenTheme.colorScheme.background.pressed,
                ),
                enabled = isEnabled,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                shouldShowDialog = !shouldShowDialog
            }
            .cardPadding(cardStyle = cardStyle, vertical = 6.dp)
            .padding(paddingValues = insets),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                textStyle = BitwardenTheme.typography.bodyLarge,
                readOnly = true,
                label = {
                    Row {
                        Text(
                            text = label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        tooltip?.let {
                            Spacer(modifier = Modifier.width(3.dp))
                            BitwardenStandardIconButton(
                                vectorIconRes = R.drawable.ic_question_circle_small,
                                contentDescription = it.contentDescription,
                                onClick = it.onClick,
                                isEnabled = isEnabled,
                                contentColor = BitwardenTheme.colorScheme.icon.secondary,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                },
                value = selectedOption.orEmpty(),
                onValueChange = onOptionSelected,
                enabled = shouldShowDialog,
                trailingIcon = {
                    Icon(
                        painter = rememberVectorPainter(id = R.drawable.ic_chevron_down),
                        contentDescription = null,
                    )
                },
                colors = bitwardenTextFieldButtonColors(),
                modifier = Modifier
                    .weight(weight = 1f)
                    .fillMaxWidth(),
            )
            BitwardenRowOfActions(
                modifier = Modifier.padding(paddingValues = actionsPadding),
                actions = actions,
            )
        }
        supportingText?.let {
            Spacer(modifier = Modifier.height(height = 8.dp))
            BitwardenHorizontalDivider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 12.dp))
            Text(
                text = it,
                style = BitwardenTheme.typography.bodySmall,
                color = BitwardenTheme.colorScheme.text.secondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp),
            )
            Spacer(modifier = Modifier.height(height = 8.dp))
        }
    }
    if (shouldShowDialog) {
        BitwardenSelectionDialog(
            title = label,
            onDismissRequest = { shouldShowDialog = false },
        ) {
            options.forEach { optionString ->
                BitwardenSelectionRow(
                    text = optionString.asText(),
                    isSelected = optionString == selectedOption,
                    onClick = {
                        shouldShowDialog = false
                        onOptionSelected(optionString)
                    },
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
        )
    }
}
