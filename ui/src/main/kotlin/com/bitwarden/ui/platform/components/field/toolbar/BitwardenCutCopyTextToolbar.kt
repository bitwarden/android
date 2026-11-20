package com.bitwarden.ui.platform.components.field.toolbar

import android.content.ClipData
import android.content.ClipDescription
import android.os.Build
import android.os.PersistableBundle
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.toClipEntry
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.core.util.isBuildVersionAtLeast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * A custom [TextToolbar] that obfuscates the copied or cut text.
 */
@OmitFromCoverage
class BitwardenCutCopyTextToolbar(
    private val value: TextFieldValue,
    private val onValueChange: (String) -> Unit,
    private val defaultTextToolbar: TextToolbar,
    private val clipboard: Clipboard,
    private val focusManager: FocusManager,
    private val scope: CoroutineScope,
) : TextToolbar {
    override val status: TextToolbarStatus get() = defaultTextToolbar.status

    override fun hide() = defaultTextToolbar.hide()

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        defaultTextToolbar.showMenu(
            rect = rect,
            onCopyRequested = onCopyRequested?.let { _ ->
                { scope.launch { clipboard.setClipData(text = value.getSelectedText()) } }
            },
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested?.let { _ ->
                {
                    scope.launch {
                        clipboard.setClipData(text = value.getSelectedText())
                        // Clear selection
                        focusManager.clearFocus(force = true)
                        // Add correct text without selection
                        onValueChange(
                            value.text.replaceRange(
                                startIndex = minOf(value.selection.start, value.selection.end),
                                endIndex = maxOf(value.selection.start, value.selection.end),
                                replacement = "",
                            ),
                        )
                    }
                }
            },
            onSelectAllRequested = onSelectAllRequested,
        )
    }
}

private suspend fun Clipboard.setClipData(text: AnnotatedString) {
    setClipEntry(
        clipEntry = ClipData
            .newPlainText("", text)
            .apply {
                description.extras = PersistableBundle(1).apply {
                    putBoolean(
                        if (isBuildVersionAtLeast(version = Build.VERSION_CODES.TIRAMISU)) {
                            ClipDescription.EXTRA_IS_SENSITIVE
                        } else {
                            "android.content.extra.IS_SENSITIVE"
                        },
                        true,
                    )
                }
            }
            .toClipEntry(),
    )
}
