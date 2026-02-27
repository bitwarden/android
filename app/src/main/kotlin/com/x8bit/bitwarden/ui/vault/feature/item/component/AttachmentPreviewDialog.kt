package com.x8bit.bitwarden.ui.vault.feature.item.component

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bitwarden.ui.platform.components.button.BitwardenStandardIconButton
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

private const val MAX_ZOOM_SCALE = 5f
private const val MIN_ZOOM_SCALE = 1f
private const val TARGET_BITMAP_SIZE = 2048

/**
 * Displays a secure, temporary image attachment in a full-screen dialog with zoom and pan
 * capabilities. The dialog can be dismissed by clicking the close button or pressing the back
 * button.
 *
 * @param attachmentFile The temporary [File] object representing the decrypted image.
 * @param onDismissRequest A lambda to be invoked when the dialog is dismissed.
 * @param onLoaded A security-critical callback invoked once the [attachmentFile] has been read
 * from disk. It signals that the temporary file can be deleted to minimize its on-disk lifetime.
 */
@Composable
fun AttachmentPreviewDialog(
    attachmentFile: File,
    onDismissRequest: () -> Unit,
    onLoaded: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = true,
            dismissOnBackPress = true,
        ),
    ) {
        var scale by remember { mutableFloatStateOf(MIN_ZOOM_SCALE) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var painter by remember { mutableStateOf<BitmapPainter?>(null) }

        LaunchedEffect(attachmentFile) {
            val bitmap = withContext(Dispatchers.IO) {
                try {
                    decodeDownsampledBitmap(
                        file = attachmentFile,
                        reqWidth = TARGET_BITMAP_SIZE,
                        reqHeight = TARGET_BITMAP_SIZE,
                    )
                } finally {
                    onLoaded()
                }
            }

            if (bitmap != null) {
                painter = BitmapPainter(bitmap.asImageBitmap())
            } else {
                // If the bitmap fails to load, dismiss the dialog.
                onDismissRequest()
            }
        }

        painter?.let {
            Box(
                modifier = Modifier.fillMaxSize(),
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

                BitwardenStandardIconButton(
                    vectorIconRes = BitwardenDrawable.ic_close,
                    contentDescription = stringResource(id = BitwardenString.close),
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .padding(top = 8.dp, end = 24.dp)
                        .align(Alignment.TopEnd),
                )
            }
        }
    }
}

private fun decodeDownsampledBitmap(file: File, reqWidth: Int, reqHeight: Int): Bitmap? {
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
