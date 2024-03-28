package com.x8bit.bitwarden.authenticator.ui.platform.components.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.toPx
import com.x8bit.bitwarden.authenticator.ui.platform.base.util.withLineBreaksAtWidth
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconResource

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
    textStyle: TextStyle? = null,
    shouldAddCustomLineBreaks: Boolean = false,
    keyboardType: KeyboardType = KeyboardType.Text,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    var widthPx by remember { mutableIntStateOf(0) }

    val currentTextStyle = textStyle ?: LocalTextStyle.current
    val formattedText = if (shouldAddCustomLineBreaks) {
        value.withLineBreaksAtWidth(
            // Adjust for built in padding
            widthPx = widthPx - 16.dp.toPx(),
            monospacedTextStyle = currentTextStyle,
        )
    } else {
        value
    }

    OutlinedTextField(
        modifier = modifier
            .onGloballyPositioned { widthPx = it.size.width },
        enabled = enabled,
        label = { Text(text = label) },
        value = formattedText,
        leadingIcon = leadingIconResource?.let { iconResource ->
            {
                Icon(
                    painter = iconResource.iconPainter,
                    contentDescription = iconResource.contentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingIcon = trailingIconContent?.let {
            trailingIconContent
        },
        placeholder = placeholder?.let {
            { Text(text = it) }
        },
        supportingText = hint?.let {
            {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        },
        onValueChange = onValueChange,
        singleLine = singleLine,
        readOnly = readOnly,
        textStyle = currentTextStyle,
        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = keyboardType),
        isError = isError,
        visualTransformation = visualTransformation,
    )
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
