package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.Collection
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class VaultSdkCollectionExtensionsTest {
    @Test
    fun `toEncryptedSdkCollection should convert a network Collection to an SDK Collection`() {
        assertEquals(
            Collection(
                organizationId = "organizationId",
                hidePasswords = true,
                name = "name",
                externalId = "externalId",
                readOnly = true,
                id = "id",
            ),
            SyncResponseJson.Collection(
                organizationId = "organizationId",
                shouldHidePasswords = true,
                name = "name",
                externalId = "externalId",
                isReadOnly = true,
                id = "id",
            )
                .toEncryptedSdkCollection(),
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toEncryptedSdkCollectionList should convert a list of network Collections to a list of SDK Collections`() {
        assertEquals(
            listOf(
                Collection(
                    organizationId = "organizationId",
                    hidePasswords = true,
                    name = "name",
                    externalId = "externalId",
                    readOnly = true,
                    id = "id",
                ),
            ),
            listOf(
                SyncResponseJson.Collection(
                    organizationId = "organizationId",
                    shouldHidePasswords = true,
                    name = "name",
                    externalId = "externalId",
                    isReadOnly = true,
                    id = "id",
                )
                    .toEncryptedSdkCollection(),
            ),
        )
    }
}
