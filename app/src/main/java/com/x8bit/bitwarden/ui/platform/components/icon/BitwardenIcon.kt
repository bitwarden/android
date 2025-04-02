package com.x8bit.bitwarden.ui.platform.components.icon

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.x8bit.bitwarden.ui.platform.base.util.nullableTestTag
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter

/**
 * Represents a Bitwarden icon that is either locally loaded or loaded using glide.
 *
 * @param iconData Label for the text field.
 * @param tint the color to be applied as the tint for the icon.
 * @param modifier A [Modifier] for the composable.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BitwardenIcon(
    iconData: IconData,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    when (iconData) {
        is IconData.Network -> {
            GlideImage(
                model = iconData.uri,
                failure = placeholder(iconData.fallbackIconRes),
                contentDescription = iconData.contentDescription?.invoke(),
                modifier = modifier.nullableTestTag(tag = iconData.testTag),
            ) {
                it.placeholder(iconData.fallbackIconRes)
            }
        }

        is IconData.Local -> {
            Icon(
                painter = rememberVectorPainter(id = iconData.iconRes),
                contentDescription = iconData.contentDescription?.invoke(),
                tint = tint,
                modifier = modifier.nullableTestTag(tag = iconData.testTag),
            )
        }
    }
}
