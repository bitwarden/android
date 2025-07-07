@file:OmitFromCoverage

package com.bitwarden.ui.platform.util

import android.content.Intent
import android.os.BadParcelableException
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import com.bitwarden.annotation.OmitFromCoverage

/**
 * A means of retrieving a [Parcelable] from an [Intent] using the given [name] in a manner that
 * is safe across SDK versions.
 */
inline fun <reified T> Intent.getSafeParcelableExtra(
    name: String,
): T? = IntentCompat.getParcelableExtra(this, name, T::class.java)

/**
 * A means of retrieving a [Parcelable] from a [Bundle] using the given [name] in a manner that
 * is safe across SDK versions.
 */
inline fun <reified T> Bundle.getSafeParcelableExtra(
    name: String,
): T? = BundleCompat.getParcelable(this, name, T::class.java)

/**
 * Validate if there's anything suspicious with the intent received.
 */
fun Intent.validate() {
    try {
        // This will force Android to attempt unparcelling the extras
        this.extras?.getBundle("trashstringwhichhasnousebuttocheckunparcel")
    } catch (_: BadParcelableException) {
        this.replaceExtras(null as Bundle?)
    } catch (_: ClassNotFoundException) {
        this.replaceExtras(null as Bundle?)
    } catch (_: RuntimeException) {
        this.replaceExtras(null as Bundle?)
    }
}
