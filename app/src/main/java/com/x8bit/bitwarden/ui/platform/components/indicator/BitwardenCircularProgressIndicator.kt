package com.x8bit.bitwarden.ui.platform.components.indicator

import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A Bitwarden-styled [CircularProgressIndicator].
 */
@Composable
fun BitwardenCircularProgressIndicator(
    modifier: Modifier = Modifier,
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = BitwardenTheme.colorScheme.stroke.border,
    )
}
