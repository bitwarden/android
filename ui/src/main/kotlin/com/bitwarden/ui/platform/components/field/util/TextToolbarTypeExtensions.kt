package com.bitwarden.ui.platform.components.field.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.input.TextFieldValue
import com.bitwarden.ui.platform.components.field.model.TextToolbarType
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenCutCopyTextToolbar
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenEmptyTextToolbar

/**
 * Creates the appropriate type of [TextToolbar] based on the [TextToolbarType] and provided values.
 */
@Composable
fun TextToolbarType.toTextToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (String) -> Unit,
): TextToolbar = when (this) {
    TextToolbarType.DEFAULT -> BitwardenCutCopyTextToolbar(
        value = textFieldValue,
        onValueChange = onValueChange,
        defaultTextToolbar = LocalTextToolbar.current,
        clipboardManager = LocalClipboard.current.nativeClipboard,
        focusManager = LocalFocusManager.current,
    )

    TextToolbarType.NONE -> BitwardenEmptyTextToolbar
}
