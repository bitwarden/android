@file:OmitFromCoverage

package com.bitwarden.ui.platform.composition

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import com.bitwarden.annotation.OmitFromCoverage
import com.bitwarden.ui.platform.manager.BidiTextManager

/**
 * CompositionLocal for [BidiTextManager].
 */
val LocalBidiTextManager: ProvidableCompositionLocal<BidiTextManager> = compositionLocalOf {
    error("CompositionLocal BidiTextManager not present")
}
