package com.x8bit.bitwarden.ui.platform.components.image

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage

/**
 * A composable that displays a gif image.
 *
 * The content will also be scaled to fit the image to the container.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BitwardenGifImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val configuration = LocalConfiguration.current
    GlideImage(
        model = remember(configuration) {
            resId
        },
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
