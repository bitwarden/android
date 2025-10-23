package com.bitwarden.ui.platform.manager

import androidx.core.text.BidiFormatter
import androidx.core.text.TextDirectionHeuristicsCompat
import com.bitwarden.annotation.OmitFromCoverage

/**
 * Default implementation of [BidiTextManager] using Android's [BidiFormatter] with
 * appropriate [TextDirectionHeuristicsCompat] heuristics for bidirectional text handling.
 */
@OmitFromCoverage
internal class BidiTextManagerImpl : BidiTextManager {

    private val bidiFormatter: BidiFormatter = BidiFormatter.getInstance()

    companion object {
        private const val CARD_NUMBER_CHUNK_SIZE = 4
    }

    override fun unicodeWrap(text: String): String {
        return bidiFormatter.unicodeWrap(
            text,
            TextDirectionHeuristicsCompat.ANYRTL_LTR,
        )
    }

    override fun forceLtr(text: String): String {
        return bidiFormatter.unicodeWrap(
            text,
            TextDirectionHeuristicsCompat.LTR,
        )
    }

    override fun forceRtl(text: String): String {
        return bidiFormatter.unicodeWrap(
            text,
            TextDirectionHeuristicsCompat.RTL,
        )
    }

    override fun formatVerificationCode(code: String, chunkSize: Int): String {
        if (code.isEmpty()) return ""

        val chunks = code.chunked(chunkSize)
        val formatted = chunks.joinToString(" ")
        return forceLtr(formatted)
    }

    override fun formatPhoneNumber(phone: String): String = forceLtr(phone)

    override fun formatCardNumber(number: String): String {
        if (number.isEmpty()) return ""

        val chunks = number.chunked(CARD_NUMBER_CHUNK_SIZE)
        val formatted = chunks.joinToString(" ")
        return forceLtr(formatted)
    }
}
