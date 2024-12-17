package com.x8bit.bitwarden.ui.autofill.fido2.util

import android.content.Context
import androidx.annotation.DrawableRes
import androidx.core.graphics.drawable.IconCompat
import com.bumptech.glide.Glide
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import timber.log.Timber
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException

/**
 * Creates an IconCompat from an IconData, or falls back to a default resource if IconData is null
 * or is not of type Network.
 *
 * @param iconData The IconData to create the IconCompat from.
 * @param defaultResourceId The resource ID of the default icon to use if IconData is null or not of
 * type Network.
 * @return An IconCompat created from the IconData or the default resource.
 */
suspend fun Context.createFido2IconCompatFromIconDataOrDefault(
    iconData: IconData?,
    @DrawableRes defaultResourceId: Int,
): IconCompat = if (iconData != null && iconData is IconData.Network) {
    createFido2IconCompatFromRemoteUriOrDefaultResource(
        uri = iconData.uri,
        defaultResourceId = defaultResourceId,
    )
} else {
    createFido2IconCompatFromResource(defaultResourceId)
}

/**
 * Creates an IconCompat from a drawable resource ID.
 */
fun Context.createFido2IconCompatFromResource(@DrawableRes resourceId: Int) =
    IconCompat.createWithResource(this, resourceId)

// futureTargetBitmap.get() is a blocking call so this function must be called from a coroutine.
@Suppress("RedundantSuspendModifier")
private suspend fun Context.createFido2IconCompatFromRemoteUriOrDefaultResource(
    uri: String,
    @DrawableRes defaultResourceId: Int,
): IconCompat {
    val futureTargetBitmap = Glide
        .with(this)
        .asBitmap()
        .load(uri)
        .placeholder(defaultResourceId)
        .submit()
    return try {
        IconCompat.createWithBitmap(futureTargetBitmap.get())
    } catch (e: CancellationException) {
        Timber.e(e, "Cancellation exception while loading icon.")
        IconCompat.createWithResource(this, defaultResourceId)
    } catch (e: ExecutionException) {
        Timber.e(e, "Execution exception while loading icon.")
        IconCompat.createWithResource(this, defaultResourceId)
    } catch (e: InterruptedException) {
        Timber.e(e, "Interrupted while loading icon.")
        IconCompat.createWithResource(this, defaultResourceId)
    }
}
