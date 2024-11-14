package com.x8bit.bitwarden.ui.platform.components.snackbar

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A custom Bitwarden-themed snackbar.
 *
 * @param bitwardenHostState The state of this snackbar.
 * @param modifier The [Modifier] to be applied to the [SnackbarHost].
 * @param windowInsets The insets to be applied to this composable.
 */
@Composable
fun BitwardenSnackbarHost(
    bitwardenHostState: BitwardenSnackbarHostState,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = WindowInsets.displayCutout.union(WindowInsets.navigationBars),
) {
    SnackbarHost(
        hostState = bitwardenHostState.snackbarHostState,
        modifier = modifier.windowInsetsPadding(insets = windowInsets),
    ) { snackbarData ->
        val message = snackbarData.visuals.message
        val currentCustomSnackbarData = bitwardenHostState.currentSnackbarData
        if (currentCustomSnackbarData?.key == message) {
            BitwardenSnackbar(
                bitwardenSnackbarData = currentCustomSnackbarData,
                onDismiss = snackbarData::dismiss,
                onActionClick = snackbarData::performAction,
            )
        }
    }
}
