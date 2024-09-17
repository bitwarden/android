package com.x8bit.bitwarden.ui.platform.components.image

import android.content.res.Configuration
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
 *
 * **NOTE:** Due to using Glide under the hood, this does not respect the themed resource management
 * of the passed in [resId] automatically. If there is a specific version of the `.gif` you can pass
 * in the optional [darkModeResId]. You would need to save these in your resources with unique
 * names.
 *
 * @sample
 * ```Kotlin
 * BitwardenGifImage(
 *   resId = R.drawable.img_setup_autofill_light,
 *   darkModeResId = R.drawable.img_setup_autofill_dark,
 * )
 * ```
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun BitwardenGifImage(
    @DrawableRes resId: Int,
    modifier: Modifier = Modifier,
    @DrawableRes darkModeResId: Int? = null,
    contentDescription: String? = null,
) {
    val uiMode = LocalConfiguration.current.uiMode and Configuration.UI_MODE_NIGHT_MASK
    GlideImage(
        model = remember(uiMode) {
            when (uiMode) {
                Configuration.UI_MODE_NIGHT_YES -> darkModeResId ?: resId
                else -> resId
            }
        },
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier,
    )
}
