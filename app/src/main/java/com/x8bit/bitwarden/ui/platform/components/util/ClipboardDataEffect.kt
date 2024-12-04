package com.x8bit.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onEach

/**
 * Side effect which is triggered when the clipboard data's text changes.
 *
 * @param onClipboardDataUpdated the action to perform when the clipboard data's text changes
 */
@Composable
fun ClipboardDataEffect(
    onClipboardDataUpdated: (String) -> Unit,
) {
    val localClipboardManager: ClipboardManager = LocalClipboardManager.current
    LaunchedEffect(localClipboardManager) {
        snapshotFlow { localClipboardManager.getText()?.text }
            .filterNotNull()
            .onEach {
                onClipboardDataUpdated(it)
            }
    }
}
