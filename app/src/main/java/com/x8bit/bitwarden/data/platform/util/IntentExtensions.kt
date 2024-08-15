@file:OmitFromCoverage

package com.x8bit.bitwarden.data.platform.util

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.core.content.IntentCompat
import androidx.core.os.BundleCompat
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

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
