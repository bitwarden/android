package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriMatchDisplayType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UriMatchDisplayUtilTest {
    @Test
    fun `toDisplayMatchType should correctly convert to UriMatchDisplayType`() {
        URI_MATCH_TYPE_MAP.forEach {
            assertEquals(
                it.key.toDisplayMatchType(),
                it.value,
            )
        }
    }

    @Test
    fun `toUriMatchType should correctly convert to UriMatchType`() {
        URI_MATCH_TYPE_MAP.forEach {
            assertEquals(
                it.key,
                it.value.toUriMatchType(),
            )
        }
    }
}

private val URI_MATCH_TYPE_MAP: Map<UriMatchType?, UriMatchDisplayType> =
    mapOf(
        Pair(null, UriMatchDisplayType.DEFAULT),
        Pair(UriMatchType.DOMAIN, UriMatchDisplayType.BASE_DOMAIN),
        Pair(UriMatchType.HOST, UriMatchDisplayType.HOST),
        Pair(UriMatchType.EXACT, UriMatchDisplayType.EXACT),
        Pair(UriMatchType.STARTS_WITH, UriMatchDisplayType.STARTS_WITH),
        Pair(UriMatchType.REGULAR_EXPRESSION, UriMatchDisplayType.REGULAR_EXPRESSION),
        Pair(UriMatchType.EXACT, UriMatchDisplayType.EXACT),
        Pair(UriMatchType.NEVER, UriMatchDisplayType.NEVER),
    )
