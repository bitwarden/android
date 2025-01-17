package com.x8bit.bitwarden.ui.platform.components.field.toolbar

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * A custom [TextToolbar] that is completely empty.
 */
@OmitFromCoverage
object BitwardenEmptyTextToolbar : TextToolbar {
    override val status: TextToolbarStatus = TextToolbarStatus.Hidden

    override fun hide() = Unit

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) = Unit
}
