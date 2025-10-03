package com.bitwarden.ui.platform.manager

import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Default implementation of [BidiTextManager].
 */
@OmitFromCoverage
internal class BidiTextManagerImpl : BidiTextManager {

    private val bidiFormatter: BidiFormatter = BidiFormatter.getInstance()

    override fun unicodeWrap(text: String): String {
        return bidiFormatter.unicodeWrap(
            text,
            TextDirectionHeuristicsCompat.ANYRTL_LTR,
        )
    }
}
