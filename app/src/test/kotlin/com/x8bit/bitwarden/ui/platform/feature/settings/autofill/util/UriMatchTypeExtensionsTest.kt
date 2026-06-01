package com.x8bit.bitwarden.ui.platform.feature.settings.autofill.util

import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.data.platform.repository.model.UriMatchType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class UriMatchTypeExtensionsTest {
    @Test
    fun `displayLabel should return the correct value for each type`() {
        mapOf(
            UriMatchType.DOMAIN to BitwardenString.base_domain.asText(),
            UriMatchType.HOST to BitwardenString.host.asText(),
            UriMatchType.STARTS_WITH to BitwardenString.starts_with.asText(),
            UriMatchType.REGULAR_EXPRESSION to BitwardenString.reg_ex.asText(),
            UriMatchType.EXACT to BitwardenString.exact.asText(),
            UriMatchType.NEVER to BitwardenString.never.asText(),
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

    @Test
    fun `isAdvancedMatching should return the correct value for each type`() {
        assertEquals(
            false,
            UriMatchType.DOMAIN.isAdvancedMatching(),
        )
        assertEquals(
            false,
            UriMatchType.EXACT.isAdvancedMatching(),
        )
        assertEquals(
            false,
            UriMatchType.HOST.isAdvancedMatching(),
        )
        assertEquals(
            false,
            UriMatchType.NEVER.isAdvancedMatching(),
        )
        assertEquals(
            true,
            UriMatchType.REGULAR_EXPRESSION.isAdvancedMatching(),
        )
        assertEquals(
            true,
            UriMatchType.STARTS_WITH.isAdvancedMatching(),
        )
    }
}
