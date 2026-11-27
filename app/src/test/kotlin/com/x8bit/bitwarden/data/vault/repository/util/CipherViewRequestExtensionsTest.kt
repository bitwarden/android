package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.vault.CipherCreateRequest
import com.bitwarden.vault.CipherEditRequest
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherViewType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CipherViewRequestExtensionsTest {

    @Test
    fun `toCipherViewType for a login should wrap the login view`() {
        val cipherView = createMockCipherView(number = 1)

        assertEquals(
            CipherViewType.Login(v1 = requireNotNull(cipherView.login)),
            cipherView.toCipherViewType(),
        )
    }

    @Test
    fun `toCipherViewType for a card should wrap the card view`() {
        val cipherView = createMockCipherView(number = 1, cipherType = CipherType.CARD)

        assertEquals(
            CipherViewType.Card(v1 = requireNotNull(cipherView.card)),
            cipherView.toCipherViewType(),
        )
    }

    @Test
    fun `toCipherViewType for an identity should wrap the identity view`() {
        val cipherView = createMockCipherView(number = 1, cipherType = CipherType.IDENTITY)

        assertEquals(
            CipherViewType.Identity(v1 = requireNotNull(cipherView.identity)),
            cipherView.toCipherViewType(),
        )
    }

    @Test
    fun `toCipherViewType when the type data is missing should return null`() {
        val cipherView = createMockCipherView(number = 1).copy(login = null)

        assertNull(cipherView.toCipherViewType())
    }

    @Test
    fun `toCipherCreateRequest should map the cipher fields and use the given collections`() {
        val cipherView = createMockCipherView(number = 1)
        val collectionIds = listOf("mockCollectionId-1")

        assertEquals(
            CipherCreateRequest(
                organizationId = cipherView.organizationId,
                collectionIds = collectionIds,
                folderId = cipherView.folderId,
                name = cipherView.name,
                notes = cipherView.notes,
                favorite = cipherView.favorite,
                reprompt = cipherView.reprompt,
                type = CipherViewType.Login(v1 = requireNotNull(cipherView.login)),
                fields = cipherView.fields.orEmpty(),
                archivedDate = cipherView.archivedDate,
            ),
            cipherView.toCipherCreateRequest(collectionIds = collectionIds),
        )
    }

    @Test
    fun `toCipherEditRequest should map the cipher fields and use the given id`() {
        val cipherView = createMockCipherView(number = 1, isArchived = true)

        assertEquals(
            CipherEditRequest(
                id = "mockCipherId",
                organizationId = cipherView.organizationId,
                folderId = cipherView.folderId,
                favorite = cipherView.favorite,
                reprompt = cipherView.reprompt,
                name = cipherView.name,
                notes = cipherView.notes,
                fields = cipherView.fields.orEmpty(),
                type = CipherViewType.Login(v1 = requireNotNull(cipherView.login)),
                revisionDate = cipherView.revisionDate,
                archivedDate = cipherView.archivedDate,
                attachments = cipherView.attachments.orEmpty(),
                key = cipherView.key,
            ),
            cipherView.toCipherEditRequest(cipherId = "mockCipherId"),
        )
    }
}
