package com.bitwarden.authenticator.ui.platform.components.listitem.model.util

import com.bitwarden.authenticator.ui.platform.components.listitem.model.SharedCodesDisplayState
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import kotlinx.collections.immutable.persistentListOf
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SharedCodesAccountSectionExtensionsTest {
    @Test
    fun `toSortAlphabetically should sort ciphers by sortKey`() {
        val codes = persistentListOf(
            SharedCodesDisplayState.SharedCodesAccountSection(
                id = "user1",
                label = BitwardenString.shared_accounts_header.asText(
                    "John@test.com",
                    "bitwarden.com",
                    1,
                ),
                codes = persistentListOf(),
                isExpanded = true,
                sortKey = "John@test.com",
            ),
            SharedCodesDisplayState.SharedCodesAccountSection(
                id = "user1",
                label = BitwardenString.shared_accounts_header.asText(
                    "Jane@test.com",
                    "bitwarden.eu",
                    1,
                ),
                codes = persistentListOf(),
                isExpanded = true,
                sortKey = "Jane@test.com",
            ),
        )
        val expected = persistentListOf(
            SharedCodesDisplayState.SharedCodesAccountSection(
                id = "user1",
                label = BitwardenString.shared_accounts_header.asText(
                    "Jane@test.com",
                    "bitwarden.eu",
                    1,
                ),
                codes = persistentListOf(),
                isExpanded = true,
                sortKey = "Jane@test.com",
            ),
            SharedCodesDisplayState.SharedCodesAccountSection(
                id = "user1",
                label = BitwardenString.shared_accounts_header.asText(
                    "John@test.com",
                    "bitwarden.com",
                    1,
                ),
                codes = persistentListOf(),
                isExpanded = true,
                sortKey = "John@test.com",
            ),
        )

        assertEquals(
            expected,
            codes.sortAlphabetically(),
        )
    }
}
