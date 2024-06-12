package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util

import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import com.x8bit.bitwarden.ui.platform.base.util.asText
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UriMatchTypeExtensionsTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            UriMatchType.DOMAIN to R.string.base_domain.asText(),
            UriMatchType.HOST to R.string.host.asText(),
            UriMatchType.STARTS_WITH to R.string.starts_with.asText(),
            UriMatchType.REGULAR_EXPRESSION to R.string.reg_ex.asText(),
            UriMatchType.EXACT to R.string.exact.asText(),
            UriMatchType.NEVER to R.string.never.asText(),
        )
            .forEach { (type, label) ->
                assertEquals(
                    label,
                    type.displayLabel,
                )
            }
    }

    @Test
    fun `toSdkUriMatchType should return the correct value for each type`() {
        assertEquals(
            com.bitwarden.vault.UriMatchType.DOMAIN,
            UriMatchType.DOMAIN.toSdkUriMatchType(),
        )
        assertEquals(
            com.bitwarden.vault.UriMatchType.EXACT,
            UriMatchType.EXACT.toSdkUriMatchType(),
        )
        assertEquals(
            com.bitwarden.vault.UriMatchType.HOST,
            UriMatchType.HOST.toSdkUriMatchType(),
        )
        assertEquals(
            com.bitwarden.vault.UriMatchType.NEVER,
            UriMatchType.NEVER.toSdkUriMatchType(),
        )
        assertEquals(
            com.bitwarden.vault.UriMatchType.REGULAR_EXPRESSION,
            UriMatchType.REGULAR_EXPRESSION.toSdkUriMatchType(),
        )
        assertEquals(
            com.bitwarden.vault.UriMatchType.STARTS_WITH,
            UriMatchType.STARTS_WITH.toSdkUriMatchType(),
        )
    }
}
