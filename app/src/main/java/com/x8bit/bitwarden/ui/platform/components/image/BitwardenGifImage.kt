package com.x8bit.bitwarden.ui.platform.components.image

import androidx.annotation.DrawableRes
import androidx.appcompat.content.res.AppCompatResources
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
    // The need for this workaround of loading into a drawable first is related to issue reported
    // [here](https://github.com/bumptech/glide/issues/3751)
    val context = LocalContext.current
    val drawable = AppCompatResources.getDrawable(context, resId)
    GlideImage(
        model = drawable,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
