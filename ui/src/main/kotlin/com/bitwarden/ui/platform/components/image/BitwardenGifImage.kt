package com.bitwarden.ui.platform.components.image

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import coil3.compose.AsyncImage

/**
 * A composable that displays a gif image.
 *
 * The content will also be scaled to fit the image to the container.
 */
@Composable
fun BitwardenGifImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    // Coil automatically registers the `coil-gif` decoder, which handles the animation.
    AsyncImage(
        model = resId,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
