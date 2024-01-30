package com.x8bit.bitwarden.data.platform.util

import com.x8bit.bitwarden.BuildConfig

/**
 * A boolean property that indicates whether the current build flavor is "fdroid".
 */
val isFdroid: Boolean
    get() = BuildConfig.FLAVOR == "fdroid"
