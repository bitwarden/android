package com.x8bit.bitwarden.ui.platform.components.model

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import com.x8bit.bitwarden.ui.platform.base.util.Text

/**
 * Data class representing the resources required for an icon.
 *
 * @property iconPainter Painter for the icon.
 * @property contentDescription String for the icon's content description.
 */
data class IconResource(
    val iconPainter: Painter,
    val contentDescription: String,
)

/**
 * Data class representing the resources required for an icon and is friendly to use in ViewModels.
 *
 * @property iconRes Resource for the icon.
 * @property contentDescription The icon's content description.
 */
data class IconRes(
    @DrawableRes
    val iconRes: Int,
    val contentDescription: Text,
)

/**
 * A helper method to convert a list of [IconRes] to a list of [IconResource].
 */
@Composable
fun List<IconRes>.toIconResources(): List<IconResource> = this.map { it.toIconResource() }

/**
 * A helper method to convert an [IconRes] to an [IconResource].
 */
@Composable
fun IconRes.toIconResource(): IconResource =
    IconResource(
        iconPainter = painterResource(id = iconRes),
        contentDescription = contentDescription(),
    )
