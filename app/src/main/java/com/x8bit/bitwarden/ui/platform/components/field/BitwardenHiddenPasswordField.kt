package com.x8bit.bitwarden.ui.platform.components.field

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.base.util.cardBackground
import com.x8bit.bitwarden.ui.platform.base.util.cardPadding
import com.x8bit.bitwarden.ui.platform.components.field.color.bitwardenTextFieldColors
import com.x8bit.bitwarden.ui.platform.components.field.toolbar.BitwardenEmptyTextToolbar
import com.x8bit.bitwarden.ui.platform.components.model.CardStyle
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Represents a Bitwarden-styled password field that is completely hidden and non-interactable.
 *
 * @param label Label for the text field.
 * @param value Current text on the text field.
 * @param modifier Modifier for the composable.
 * @param cardStyle Indicates the type of card style to be applied.
 */
@Composable
fun BitwardenHiddenPasswordField(
    label: String?,
    value: String,
    modifier: Modifier = Modifier,
    cardStyle: CardStyle? = null,
) {
    CompositionLocalProvider(value = LocalTextToolbar provides BitwardenEmptyTextToolbar) {
        TextField(
            modifier = modifier
                .cardBackground(cardStyle = cardStyle)
                .cardPadding(cardStyle = cardStyle, vertical = 6.dp),
            textStyle = BitwardenTheme.typography.sensitiveInfoSmall,
            label = label?.let { { Text(text = it) } },
            value = value,
            onValueChange = { },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            enabled = false,
            readOnly = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            colors = bitwardenTextFieldColors(),
        )
    }
}

@Preview
@Composable
private fun BitwardenHiddenPasswordField_preview() {
    BitwardenTheme {
        BitwardenHiddenPasswordField(
            label = "Label",
            value = "Password",
            cardStyle = CardStyle.Full,
        )
    }
}
