package com.bitwarden.ui.platform.components.field.toolbar

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.core.os.persistableBundleOf
import com.bitwarden.annotation.OmitFromCoverage

/**
 * A custom [TextToolbar] that is obfuscates the copied or cut text.
 */
@OmitFromCoverage
class BitwardenCutCopyTextToolbar(
    private val value: TextFieldValue,
    private val onValueChange: (String) -> Unit,
    private val defaultTextToolbar: TextToolbar,
    private val clipboardManager: ClipboardManager,
    private val focusManager: FocusManager,
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
                {
                    clipboardManager.setPrimaryClip(
                        ClipData
                            .newPlainText("", value.getSelectedText())
                            .apply {
                                description.extras = persistableBundleOf(
                                    "android.content.extra.IS_SENSITIVE" to true,
                                )
                            },
                    )
                }
            },
            onPasteRequested = onPasteRequested,
            onCutRequested = onCutRequested?.let { _ ->
                {
                    clipboardManager.setPrimaryClip(
                        ClipData
                            .newPlainText("", value.getSelectedText())
                            .apply {
                                description.extras = persistableBundleOf(
                                    "android.content.extra.IS_SENSITIVE" to true,
                                )
                            },
                    )
                    // Clear selection
                    focusManager.clearFocus(force = true)
                    // Add correct text without selection
                    onValueChange(
                        value.text.replaceRange(
                            minOf(value.selection.start, value.selection.end),
                            maxOf(value.selection.start, value.selection.end),
                            "",
                        ),
                    )
                }
            },
            onSelectAllRequested = onSelectAllRequested,
        )
    }
}
