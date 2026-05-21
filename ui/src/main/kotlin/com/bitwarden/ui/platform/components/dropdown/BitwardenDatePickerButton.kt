package com.bitwarden.ui.platform.components.dropdown

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bitwarden.core.data.util.toFormattedDateStyle
import com.bitwarden.ui.platform.components.button.BitwardenTextSelectionButton
import com.bitwarden.ui.platform.components.button.model.BitwardenHelpButtonData
import com.bitwarden.ui.platform.components.dialog.BitwardenDatePickerDialog
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.composition.LocalClock
import com.bitwarden.ui.platform.theme.BitwardenTheme
import java.time.Clock
import java.time.LocalDate
import java.time.format.FormatStyle

/**
 * A button that displays a selected date and opens a date picker dialog when clicked.
 *
 * @param label The descriptive text label for the [OutlinedTextField].
 * @param currentDate The currently selected [LocalDate] value.
 * @param onDateSelect A lambda invoked with the newly selected [LocalDate] when confirmed.
 * @param cardStyle Indicates the type of card style to be applied.
 * @param modifier A [Modifier] that you can use to apply custom modifications to the composable.
 * @param isEnabled Whether the button is enabled.
 * @param supportingContent An optional supporting content that will appear below the button.
 * @param helpData An optional [BitwardenHelpButtonData], representing the help button.
 * @param insets Inner padding to be applied within the card.
 * @param textFieldTestTag The optional test tag associated with the inner text field.
 * @param actionsPadding Padding to be applied to the [actions] block.
 * @param actions A lambda containing the set of actions (usually icons or similar) to display
 * in the trailing side. This lambda extends [RowScope], allowing flexibility in defining the
 * layout of the actions.
 */
@Composable
fun BitwardenDatePickerButton(
    label: String,
    currentDate: LocalDate?,
    onDateSelect: (LocalDate?) -> Unit,
    cardStyle: CardStyle?,
    modifier: Modifier = Modifier,
    isEnabled: Boolean = true,
    supportingContent: @Composable (ColumnScope.() -> Unit)? = null,
    helpData: BitwardenHelpButtonData? = null,
    insets: PaddingValues = PaddingValues(),
    textFieldTestTag: String? = null,
    actionsPadding: PaddingValues = PaddingValues(end = 4.dp),
    clock: Clock = LocalClock.current,
    actions: @Composable RowScope.() -> Unit = {},
) {
    var shouldShowDialog by rememberSaveable { mutableStateOf(value = false) }
    BitwardenTextSelectionButton(
        label = label,
        selectedOption = currentDate?.toFormattedDateStyle(
            dateStyle = FormatStyle.LONG,
            clock = clock,
        ),
        onClick = { shouldShowDialog = true },
        cardStyle = cardStyle,
        enabled = isEnabled,
        showChevron = true,
        supportingContent = supportingContent,
        helpData = helpData,
        insets = insets,
        textFieldTestTag = textFieldTestTag,
        actionsPadding = actionsPadding,
        actions = actions,
        modifier = modifier,
    )
    if (shouldShowDialog) {
        BitwardenDatePickerDialog(
            initialDate = currentDate,
            onDateSelect = { date ->
                onDateSelect(date)
                shouldShowDialog = false
            },
            onDismissRequest = { shouldShowDialog = false },
        )
    }
}

@Suppress("MagicNumber")
@Preview
@Composable
private fun BitwardenDatePickerButton_preview() {
    BitwardenTheme {
        BitwardenDatePickerButton(
            label = "Date of birth",
            currentDate = LocalDate.of(2026, 6, 15),
            onDateSelect = {},
            cardStyle = CardStyle.Full,
        )
    }
}
