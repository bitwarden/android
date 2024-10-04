package com.x8bit.bitwarden.ui.platform.components.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.base.util.toPx
import com.x8bit.bitwarden.ui.platform.base.util.withLineBreaksAtWidth
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.x8bit.bitwarden.ui.platform.components.model.IconResource
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Component that allows the user to input text. This composable will manage the state of
 * the user's input.
 * @param label label for the text field.
 * @param value current next on the text field.
 * @param modifier modifier for the composable.
 * @param onValueChange callback that is triggered when the input of the text field changes.
 * @param placeholder the optional placeholder to be displayed when the text field is in focus and
 * the [value] is empty.
 * @param leadingIconResource the optional resource for the leading icon on the text field.
 * @param trailingIconContent the content for the trailing icon in the text field.
 * @param hint optional hint text that will appear below the text input.
 * @param singleLine when `true`, this text field becomes a single line that horizontally scrolls
 * instead of wrapping onto multiple lines.
 * @param readOnly `true` if the input should be read-only and not accept user interactions.
 * @param enabled Whether or not the text field is enabled.
 * @param textStyle An optional style that may be used to override the default used.
 * @param shouldAddCustomLineBreaks If `true`, line breaks will be inserted to allow for filling
 * an entire line before breaking. `false` by default.
 * @param visualTransformation Transforms the visual representation of the input [value].
 * @param keyboardType the preferred type of keyboard input.
 */
@Composable
fun BitwardenTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIconResource: IconResource? = null,
    trailingIconContent: (@Composable () -> Unit)? = null,
    hint: String? = null,
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    textStyle: TextStyle = BitwardenTheme.typography.bodyLarge,
    shouldAddCustomLineBreaks: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    autoFocus: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var widthPx by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }
    val formattedText = if (shouldAddCustomLineBreaks) {
        value.withLineBreaksAtWidth(
            // Adjust for built in padding
            widthPx = widthPx - 16.dp.toPx(),
            monospacedTextStyle = textStyle,
        )
    } else {
        value
    }

    OutlinedTextField(
        colors = bitwardenTextFieldColors(),
        modifier = modifier
            .onGloballyPositioned { widthPx = it.size.width }
            .focusRequester(focusRequester),
        enabled = enabled,
        label = { Text(text = label) },
        value = formattedText,
        leadingIcon = leadingIconResource?.let { iconResource ->
            {
                Icon(
                    painter = iconResource.iconPainter,
                    contentDescription = iconResource.contentDescription,
                    tint = BitwardenTheme.colorScheme.icon.primary,
                )
            }
        },
        trailingIcon = trailingIconContent,
        placeholder = placeholder?.let {
            {
                Text(
                    text = it,
                    color = BitwardenTheme.colorScheme.text.primary,
                )
            }
        },
        supportingText = hint?.let {
            {
                Text(
                    text = hint,
                    style = BitwardenTheme.typography.bodySmall,
                )
            }
        },
        onValueChange = onValueChange,
        singleLine = singleLine,
        readOnly = readOnly,
        textStyle = textStyle,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
        isError = isError,
        visualTransformation = visualTransformation,
    )
    if (autoFocus) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }
    }
}

@Preview
@Composable
private fun BitwardenTextField_preview_withInput() {
    BitwardenTextField(
        label = "Label",
        value = "Input",
        onValueChange = {},
        hint = "Hint",
    )
}

@Preview
@Composable
private fun BitwardenTextField_preview_withoutInput() {
    BitwardenTextField(
        label = "Label",
        value = "",
        onValueChange = {},
        hint = "Hint",
    )
}
