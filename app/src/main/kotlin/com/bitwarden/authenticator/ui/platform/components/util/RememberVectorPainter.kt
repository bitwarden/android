package com.bitwarden.authenticator.ui.platform.components.util

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.vectorResource

/**
 * Returns a [VectorPainter] built from the given [id] to circumvent issues with painter resources
 * recomposing unnecessarily.
 */
@Composable
fun rememberVectorPainter(
    @DrawableRes id: Int,
): VectorPainter = rememberVectorPainter(
    image = ImageVector.vectorResource(id),
)
