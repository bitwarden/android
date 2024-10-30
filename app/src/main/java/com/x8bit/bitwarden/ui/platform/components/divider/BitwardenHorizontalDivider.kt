package com.x8bit.bitwarden.ui.platform.components.divider

import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.x8bit.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * A divider line.
 *
 * @param modifier The [Modifier] to be applied to this divider.
 * @param thickness The thickness of this divider. Using [Dp.Hairline] will produce a single pixel
 * divider regardless of screen density.
 * @param color The color of this divider.
 */
@Composable
fun BitwardenHorizontalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = (0.5).dp,
    color: Color = BitwardenTheme.colorScheme.stroke.divider,
) {
    HorizontalDivider(
        modifier = modifier,
        thickness = thickness,
        color = color,
    )
}
