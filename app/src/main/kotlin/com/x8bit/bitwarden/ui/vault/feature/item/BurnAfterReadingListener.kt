package com.x8bit.bitwarden.ui.vault.feature.item

import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target

/**
 * Glide [RequestListener] that signals the ViewModel to delete the decrypted
 * temporary file as soon as the bitmap has been rendered into memory.
 *
 * This implements the core "burn-after-reading" security mechanism:
 * - [onResourceReady]: Bitmap is in RAM → delete file from disk.
 * - [onLoadFailed]: Loading failed → still delete file from disk.
 *
 * @param attachmentId The unique attachment ID for cleanup targeting.
 * @param onComplete Callback to signal the ViewModel (typically
 *   [VaultMediaViewerViewModel.onBitmapRenderComplete]).
 */
class BurnAfterReadingListener(
    private val attachmentId: String,
    private val onComplete: (String) -> Unit,
) : RequestListener<Drawable> {

    override fun onResourceReady(
        resource: Drawable,
        model: Any,
        target: Target<Drawable>?,
        dataSource: DataSource,
        isFirstResource: Boolean,
    ): Boolean {
        // Bitmap is now in Glide's LRU memory cache. Delete the source file.
        onComplete(attachmentId)
        return false
    }

    override fun onLoadFailed(
        e: GlideException?,
        model: Any?,
        target: Target<Drawable>,
        isFirstResource: Boolean,
    ): Boolean {
        // Even on failure, clean up the temp file.
        onComplete(attachmentId)
        return false
    }
}
