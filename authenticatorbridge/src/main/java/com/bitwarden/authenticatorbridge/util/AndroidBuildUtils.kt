package com.bitwarden.authenticatorbridge.util

import android.os.Build

/**
 * Returns true if the current OS build version is below the provided [version].
 *
 * @see Build.VERSION_CODES
 */
internal fun isBuildVersionBelow(version: Int): Boolean = version > Build.VERSION.SDK_INT
