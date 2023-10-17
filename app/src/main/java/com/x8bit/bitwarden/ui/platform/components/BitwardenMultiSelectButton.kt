package com.x8bit.bitwarden.ui.platform.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.toDp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A custom composable representing a multi-select button.
 *
 * This composable displays an [OutlinedTextField] with a dropdown icon as a trailing icon.
 * When the field is clicked, a dropdown menu appears with a list of options to select from.
 *
 * @param label The descriptive text label for the [OutlinedTextField].
 * @param options A list of strings representing the available options in the dropdown menu.
 * @param selectedOption The currently selected option that is displayed in the [OutlinedTextField].
 * @param onOptionSelected A lambda that is invoked when an option
 * is selected from the dropdown menu.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 */
@Suppress("LongMethod")
@Composable
fun BitwardenMultiSelectButton(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    var textFieldWidth by remember { mutableIntStateOf(0) }

    // Watch for changes to 'expanded' and request focus if needed
    LaunchedEffect(expanded) {
        if (expanded) focusRequester.requestFocus() else focusRequester.freeFocus()
    }

    Box(
        modifier = modifier
            .semantics(mergeDescendants = true) {},
    ) {
        OutlinedTextField(
            // TODO: Update with final accessibility reading (BIT-752)
            modifier = Modifier
                .clearAndSetSemantics {
                    this.role = Role.DropdownList
                    contentDescription = "$label, $selectedOption"
                }
                .fillMaxWidth()
                .clickable(
                    // Disable the ripple
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    expanded = !expanded
                }
                .onGloballyPositioned { coordinates ->
                    textFieldWidth = coordinates.size.width
                }
                .focusRequester(focusRequester),
            textStyle = MaterialTheme.typography.bodyLarge,
            readOnly = true,
            label = {
                Text(
                    text = label,
                )
            },
            value = selectedOption,
            onValueChange = onOptionSelected,
            enabled = expanded,
            trailingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_region_select_dropdown),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline,
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        )
        DropdownMenu(
            modifier = Modifier.width(textFieldWidth.toDp()),
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                focusRequester.freeFocus()
            },
        ) {
            options.forEach { optionString ->
                DropdownMenuItem(
                    text = { Text(text = optionString) },
                    onClick = {
                        expanded = false
                        onOptionSelected(optionString)
                        focusRequester.freeFocus()
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
            options = listOf("a", "b"),
            selectedOption = "",
            onOptionSelected = {},
        )
    }
}
