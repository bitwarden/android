package com.x8bit.bitwarden.data.auth.repository.util

import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class SyncResponseJsonExtensionsTest {
    @Test
    fun `toOrganization should output the correct organization`() {
        assertEquals(
            Organization(
                id = "mockId-1",
                name = "mockName-1",
            ),
            createMockOrganization(number = 1).toOrganization(),
        )
    }

    @Test
    fun `toOrganizations should output the correct list of organizations`() {
        assertEquals(
            listOf(
                Organization(
                    id = "mockId-1",
                    name = "mockName-1",
                ),
                Organization(
                    id = "mockId-2",
                    name = "mockName-2",
                ),
            ),
            listOf(
                createMockOrganization(number = 1),
                createMockOrganization(number = 2),
            )
                .toOrganizations(),
        )
    }
}
