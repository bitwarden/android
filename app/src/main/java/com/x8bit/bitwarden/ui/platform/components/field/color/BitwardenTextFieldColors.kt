package com.x8bit.bitwarden.ui.platform.components.field.color

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Provides a default set of Bitwarden-styled colors for a read-only text field button.
 */
@Composable
fun bitwardenTextFieldButtonColors(): TextFieldColors = bitwardenTextFieldColors(
    unfocusedBorderColor = BitwardenTheme.colorScheme.stroke.divider,
    focusedBorderColor = BitwardenTheme.colorScheme.stroke.divider,
    disabledTextColor = BitwardenTheme.colorScheme.text.primary,
    disabledBorderColor = BitwardenTheme.colorScheme.stroke.divider,
    disabledLeadingIconColor = BitwardenTheme.colorScheme.icon.primary,
    disabledTrailingIconColor = BitwardenTheme.colorScheme.icon.primary,
    disabledLabelColor = BitwardenTheme.colorScheme.text.secondary,
    disabledPlaceholderColor = BitwardenTheme.colorScheme.text.secondary,
    disabledSupportingTextColor = BitwardenTheme.colorScheme.text.secondary,
)

/**
 * Provides a default set of Bitwarden-styled colors for text fields.
 */
@Composable
fun bitwardenTextFieldColors(
    unfocusedBorderColor: Color = BitwardenTheme.colorScheme.stroke.divider,
    focusedBorderColor: Color = BitwardenTheme.colorScheme.stroke.border,
    disabledTextColor: Color = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    disabledBorderColor: Color = BitwardenTheme.colorScheme.outlineButton.borderDisabled,
    disabledLeadingIconColor: Color = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    disabledTrailingIconColor: Color = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    disabledLabelColor: Color = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    disabledPlaceholderColor: Color = BitwardenTheme.colorScheme.text.secondary,
    disabledSupportingTextColor: Color = BitwardenTheme
        .colorScheme
        .outlineButton
        .foregroundDisabled,
): TextFieldColors = TextFieldColors(
    focusedTextColor = BitwardenTheme.colorScheme.text.primary,
    unfocusedTextColor = BitwardenTheme.colorScheme.text.primary,
    disabledTextColor = disabledTextColor,
    errorTextColor = BitwardenTheme.colorScheme.text.primary,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    cursorColor = BitwardenTheme.colorScheme.icon.primary,
    errorCursorColor = BitwardenTheme.colorScheme.icon.primary,
    textSelectionColors = TextSelectionColors(
        handleColor = BitwardenTheme.colorScheme.stroke.border,
        backgroundColor = BitwardenTheme.colorScheme.stroke.border.copy(alpha = 0.4f),
    ),
    focusedIndicatorColor = focusedBorderColor,
    unfocusedIndicatorColor = unfocusedBorderColor,
    disabledIndicatorColor = disabledBorderColor,
    errorIndicatorColor = BitwardenTheme.colorScheme.status.error,
    focusedLeadingIconColor = BitwardenTheme.colorScheme.icon.primary,
    unfocusedLeadingIconColor = BitwardenTheme.colorScheme.icon.primary,
    disabledLeadingIconColor = disabledLeadingIconColor,
    errorLeadingIconColor = BitwardenTheme.colorScheme.icon.primary,
    focusedTrailingIconColor = BitwardenTheme.colorScheme.icon.primary,
    unfocusedTrailingIconColor = BitwardenTheme.colorScheme.icon.primary,
    disabledTrailingIconColor = disabledTrailingIconColor,
    errorTrailingIconColor = BitwardenTheme.colorScheme.status.error,
    focusedLabelColor = BitwardenTheme.colorScheme.text.secondary,
    unfocusedLabelColor = BitwardenTheme.colorScheme.text.secondary,
    disabledLabelColor = disabledLabelColor,
    errorLabelColor = BitwardenTheme.colorScheme.status.error,
    focusedPlaceholderColor = BitwardenTheme.colorScheme.text.secondary,
    unfocusedPlaceholderColor = BitwardenTheme.colorScheme.text.secondary,
    disabledPlaceholderColor = disabledPlaceholderColor,
    errorPlaceholderColor = BitwardenTheme.colorScheme.text.secondary,
    focusedSupportingTextColor = BitwardenTheme.colorScheme.text.secondary,
    unfocusedSupportingTextColor = BitwardenTheme.colorScheme.text.secondary,
    disabledSupportingTextColor = disabledSupportingTextColor,
    errorSupportingTextColor = BitwardenTheme.colorScheme.text.secondary,
    focusedPrefixColor = BitwardenTheme.colorScheme.text.secondary,
    unfocusedPrefixColor = BitwardenTheme.colorScheme.text.secondary,
    disabledPrefixColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    errorPrefixColor = BitwardenTheme.colorScheme.status.error,
    focusedSuffixColor = BitwardenTheme.colorScheme.text.secondary,
    unfocusedSuffixColor = BitwardenTheme.colorScheme.text.secondary,
    disabledSuffixColor = BitwardenTheme.colorScheme.outlineButton.foregroundDisabled,
    errorSuffixColor = BitwardenTheme.colorScheme.status.error,
)
