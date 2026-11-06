package com.x8bit.bitwarden.ui.vault.feature.pamapprovals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.components.icon.BitwardenIcon
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.theme.BitwardenTheme

/**
 * Displays the icon of the item an access request is for: its favicon, or the icon for its type
 * on the same tinted backdrop the vault list uses.
 */
@Composable
fun PamItemIcon(
    iconData: IconData,
    modifier: Modifier = Modifier,
    iconTestTag: String? = null,
) {
    val hasBackground = iconData is IconData.Local
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(36.dp)
            .then(
                if (hasBackground) {
                    Modifier.background(
                        color = BitwardenTheme.colorScheme.illustration.backgroundPrimary,
                        shape = BitwardenTheme.shapes.favicon,
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        BitwardenIcon(
            iconData = iconData,
            tint = if (hasBackground) {
                BitwardenTheme.colorScheme.illustration.outline
            } else {
                Color.Unspecified
            },
            modifier = Modifier
                .then(if (iconTestTag != null) Modifier.testTag(iconTestTag) else Modifier)
                .then(if (hasBackground) Modifier else Modifier.fillMaxSize()),
        )
    }
}
