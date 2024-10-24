@file:OmitFromCoverage

package com.x8bit.bitwarden.data.platform.util

import android.os.Build
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * Returns true if the current OS build version is below the provided [version].
 *
 * @see Build.VERSION_CODES
 */
internal fun isBuildVersionBelow(version: Int): Boolean = version > Build.VERSION.SDK_INT
