@file:OmitFromCoverage

package com.x8bit.bitwarden.ui.platform.util

import android.content.res.Configuration
import com.x8bit.bitwarden.data.platform.annotation.OmitFromCoverage

/**
 * A helper method to indicate if the current UI configuration is portrait or not.
 */
val Configuration.isPortrait: Boolean
    get() = when (this.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> false
        else -> true
    }
