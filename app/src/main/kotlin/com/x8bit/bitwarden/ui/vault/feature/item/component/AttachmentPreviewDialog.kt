package com.x8bit.bitwarden.ui.vault.feature.item.component

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.bitwarden.ui.platform.resource.BitwardenString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Displays a secure, temporary image attachment in a full-screen dialog with zoom and pan
 * capabilities. The dialog can be dismissed by clicking the close button or pressing the back button.
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
        var scale by remember { mutableFloatStateOf(1f) }
        var offset by remember { mutableStateOf(Offset.Zero) }
        var painter by remember { mutableStateOf<Painter?>(null) }
        var showContent by remember { mutableStateOf(false) }

        LaunchedEffect(attachmentFile) {
            val loadedPainter = withContext(Dispatchers.IO) {
                try {
                    val bitmap = BitmapFactory.decodeFile(attachmentFile.path)
                    if (bitmap != null) {
                        BitmapPainter(bitmap.asImageBitmap())
                    } else {
                        null
                    }
                } finally {
                    onLoaded()
                }
            }

            if (loadedPainter != null) {
                painter = loadedPainter
                showContent = true
            } else {
                // If the bitmap fails to load, dismiss the dialog.
                onDismissRequest()
            }
        }

        if (showContent && painter != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painter!!,
                    contentDescription = stringResource(BitwardenString.preview),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(1f, 5f)
                                offset = if (scale > 1f) offset + pan else Offset.Zero
                            }
                        }
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )

                IconButton(
                    onClick = onDismissRequest,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(BitwardenString.close),
                        tint = Color.White
                    )
                }
            }
        }
    }
}