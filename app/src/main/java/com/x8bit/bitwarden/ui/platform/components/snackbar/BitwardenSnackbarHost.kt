package com.x8bit.bitwarden.ui.platform.components.snackbar

import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A custom Bitwarden-themed snackbar.
 *
 * @param hostState The state of this snackbar.
 * @param modifier The [Modifier] to be applied to this radio button.
 */
@Composable
fun BitwardenSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(
        hostState = hostState,
        modifier = modifier,
    ) {
        Snackbar(
            snackbarData = it,
            containerColor = BitwardenTheme.colorScheme.background.alert,
            contentColor = BitwardenTheme.colorScheme.text.reversed,
            actionColor = BitwardenTheme.colorScheme.background.alert,
            actionContentColor = BitwardenTheme.colorScheme.icon.reversed,
            dismissActionContentColor = BitwardenTheme.colorScheme.icon.reversed,
        )
    }
}
