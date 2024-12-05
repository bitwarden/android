package com.x8bit.bitwarden.ui.platform.components.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager

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
    var clipDataText: String? by rememberSaveable { mutableStateOf(null) }
    val listener: android.content.ClipboardManager.OnPrimaryClipChangedListener =
        remember(localClipboardManager) {
            android.content.ClipboardManager.OnPrimaryClipChangedListener {
                clipDataText = localClipboardManager.getText().toString()
            }
        }
    LaunchedEffect(clipDataText) {
        clipDataText?.let {
            onClipboardDataUpdated(it)
        }
    }
    DisposableEffect(localClipboardManager) {
        localClipboardManager.nativeClipboard.addPrimaryClipChangedListener(listener)
        onDispose {
            localClipboardManager.nativeClipboard.removePrimaryClipChangedListener(listener)
        }
    }
}
