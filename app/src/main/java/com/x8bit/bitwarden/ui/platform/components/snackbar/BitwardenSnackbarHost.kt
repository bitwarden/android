package com.x8bit.bitwarden.ui.platform.components.snackbar

import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * A custom Bitwarden-themed snackbar.
 *
 * @param bitwardenHostState The state of this snackbar.
 * @param modifier The [Modifier] to be applied to the [SnackbarHost].
 */
@Composable
fun BitwardenSnackbarHost(
    bitwardenHostState: BitwardenSnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = bitwardenHostState.snackbarHostState,
        modifier = modifier,
    ) { snackbarData ->
        val message = snackbarData.visuals.message
        val currentCustomSnackbarData = bitwardenHostState.currentSnackbarData
        if (currentCustomSnackbarData?.key == message) {
            BitwardenSnackbar(
                bitwardenSnackbarData = currentCustomSnackbarData,
                onDismiss = { snackbarData.dismiss() },
                onActionClick = { snackbarData.performAction() },
            )
        }
    }
}
