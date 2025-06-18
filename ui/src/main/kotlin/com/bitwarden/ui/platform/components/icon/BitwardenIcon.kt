package com.bitwarden.ui.platform.components.icon

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.bitwarden.ui.platform.base.util.nullableTestTag
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.util.rememberVectorPainter
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder

/**
 * Represents a Bitwarden icon that is either locally loaded or loaded using glide.
 *
 * @param iconData Label for the text field.
 * @param modifier A [Modifier] for the composable.
 * @param tint the color to be applied as the tint for the icon.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BitwardenIcon(
    iconData: IconData,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
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
