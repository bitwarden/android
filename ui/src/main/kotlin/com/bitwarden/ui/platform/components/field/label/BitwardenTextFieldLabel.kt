package com.bitwarden.ui.platform.components.field.label

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.button.BitwardenHelpIconButton
import com.bitwarden.ui.platform.components.button.model.BitwardenHelpButtonData
import com.bitwarden.ui.platform.components.field.BitwardenPasswordField
import com.bitwarden.ui.platform.components.field.BitwardenTextField

/**
 * Represents a Bitwarden-styled label for a text field.
 *
 * Intended to be reused for all implementations of a TextField, such as the [BitwardenTextField]
 * and [BitwardenPasswordField].
 */
@Composable
internal fun BitwardenTextFieldLabel(
    label: String,
    textFieldValue: TextFieldValue,
    helpData: BitwardenHelpButtonData?,
    isFieldFocused: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        Text(text = label)
        helpData?.let { helpButtonData ->
            val size by animateDpAsState(
                targetValue = if (textFieldValue.text.isEmpty() || isFieldFocused) 16.dp else 12.dp,
                label = "${helpButtonData.contentDescription}_animation",
            )
            Spacer(modifier = Modifier.width(width = 8.dp))
            BitwardenHelpIconButton(
                helpData = helpButtonData,
                modifier = Modifier.size(size = size),
            )
        }
    }
}
