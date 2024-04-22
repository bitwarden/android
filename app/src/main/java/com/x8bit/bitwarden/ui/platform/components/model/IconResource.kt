package com.x8bit.bitwarden.ui.platform.components.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.components.util.rememberVectorPainter
import kotlinx.parcelize.Parcelize

/**
 * Data class representing the resources required for an icon.
 *
 * @property iconPainter Painter for the icon.
 * @property contentDescription String for the icon's content description.
 * @property testTag The optional test tag to associate with this icon.
 */
data class IconResource(
    val iconPainter: Painter,
    val contentDescription: String,
    val testTag: String? = null,
)

/**
 * Data class representing the resources required for an icon and is friendly to use in ViewModels.
 *
 * @property iconRes Resource for the icon.
 * @property contentDescription The icon's content description.
 * @property testTag The optional test tag to associate with this icon.
 */
@Parcelize
data class IconRes(
    @DrawableRes
    val iconRes: Int,
    val contentDescription: Text,
    val testTag: String? = null,
) : Parcelable

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
        iconPainter = rememberVectorPainter(id = iconRes),
        contentDescription = contentDescription(),
        testTag = testTag,
    )
