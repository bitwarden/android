@file:OmitFromCoverage

package com.x8bit.bitwarden.data.platform.util

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * A means of retrieving a [Parcelable] from an [Intent] using the given [name] in a manner that
 * is safe across SDK versions.
 */
inline fun <reified T> Intent.getSafeParcelableExtra(name: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelableExtra(
            name,
            T::class.java,
        )
    } else {
        @Suppress("DEPRECATION")
        getParcelableExtra(name)
    }

/**
 * A means of retrieving a [Parcelable] from a [Bundle] using the given [name] in a manner that
 * is safe across SDK versions.
 */
inline fun <reified T> Bundle.getSafeParcelableExtra(name: String): T? =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(
            name,
            T::class.java,
        )
    } else {
        @Suppress("DEPRECATION")
        getParcelable(name)
    }
