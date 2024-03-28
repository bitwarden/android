package com.x8bit.bitwarden.authenticator.ui.platform.components.icon

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.x8bit.bitwarden.authenticator.ui.platform.components.model.IconData

/**
 * Represents a Bitwarden icon that is either locally loaded or loaded using glide.
 *
 * @param iconData Label for the text field.
 * @param tint the color to be applied as the tint for the icon.
 * @param modifier A [Modifier] for the composable.
 * @param contentDescription A description of the switch's UI for accessibility purposes.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BitwardenIcon(
    iconData: IconData,
    tint: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    when (iconData) {
        is IconData.Network -> {
            GlideImage(
                model = iconData.uri,
                failure = placeholder(iconData.fallbackIconRes),
                contentDescription = contentDescription,
                modifier = modifier,
            ) {
                it.placeholder(iconData.fallbackIconRes)
            }
        }

        is IconData.Local -> {
            Icon(
                painter = painterResource(id = iconData.iconRes),
                contentDescription = contentDescription,
                tint = tint,
                modifier = modifier,
            )
        }
    }
}
