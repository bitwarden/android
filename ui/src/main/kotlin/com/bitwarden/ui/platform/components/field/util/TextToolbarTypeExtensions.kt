package com.bitwarden.ui.platform.components.field.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.text.input.TextFieldValue
import com.bitwarden.ui.platform.components.field.model.TextToolbarType
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenCutCopyTextToolbar
import com.bitwarden.ui.platform.components.field.toolbar.BitwardenEmptyTextToolbar
import kotlinx.coroutines.CoroutineScope

/**
 * Creates the appropriate type of [TextToolbar] based on the [TextToolbarType] and provided values.
 */
@Composable
fun TextToolbarType.toTextToolbar(
    textFieldValue: TextFieldValue,
    onValueChange: (String) -> Unit,
    clipboard: Clipboard = LocalClipboard.current,
    defaultTextToolbar: TextToolbar = LocalTextToolbar.current,
    focusManager: FocusManager = LocalFocusManager.current,
    scope: CoroutineScope = rememberCoroutineScope(),
): TextToolbar = when (this) {
    TextToolbarType.DEFAULT -> BitwardenCutCopyTextToolbar(
        value = textFieldValue,
        onValueChange = onValueChange,
        defaultTextToolbar = defaultTextToolbar,
        clipboard = clipboard,
        focusManager = focusManager,
        scope = scope,
    )

    TextToolbarType.NONE -> BitwardenEmptyTextToolbar
}
