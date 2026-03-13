package com.bitwarden.ui.platform.components.preview

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import com.bitwarden.ui.platform.resource.BitwardenString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_ZOOM_SCALE = 5f
private const val MIN_ZOOM_SCALE = 1f
private const val TARGET_BITMAP_SIZE = 2048

/**
 * Displays a preview of the [file] if it's an image.
 */
@Composable
fun ImagePreviewContent(
    file: File,
    onMissingFile: () -> Unit,
    onLoaded: () -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var painter by remember { mutableStateOf<BitmapPainter?>(null) }

    LaunchedEffect(file) {
        val attachmentFile = file.takeIf { it.exists() } ?: run {
            onMissingFile()
            return@LaunchedEffect
        }
        val bitmap = withContext(Dispatchers.IO) {
            try {
                decodeDownsampledBitmap(file = attachmentFile)
            } finally {
                onLoaded()
            }
        }

        bitmap
            ?.let { painter = BitmapPainter(it.asImageBitmap()) }
            ?: onError()
    }

    painter?.let {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = it,
                contentDescription = stringResource(id = BitwardenString.preview),
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(MIN_ZOOM_SCALE, MAX_ZOOM_SCALE)
                            offset = if (scale > MIN_ZOOM_SCALE) offset + pan else Offset.Zero
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                    ),
            )
        }
    }
}

private fun decodeDownsampledBitmap(
    file: File,
    reqWidth: Int = TARGET_BITMAP_SIZE,
    reqHeight: Int = TARGET_BITMAP_SIZE,
): Bitmap? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeFile(file.path, options)

    options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
    options.inJustDecodeBounds = false

    return BitmapFactory.decodeFile(file.path, options)
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int,
): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {
        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
