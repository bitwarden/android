package com.x8bit.bitwarden.ui.vault.feature.attachments.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockAttachmentView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.ui.vault.feature.attachments.AttachmentsState
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

class CipherViewExtensionsTest {

    @Test
    fun `toViewState should return content with items when CipherView has attachments`() {
        val cipherView = createMockCipherView(number = 1)

        val result = cipherView.toViewState()

        assertEquals(
            AttachmentsState.ViewState.Content(
                originalCipher = cipherView,
                attachments = listOf(
                    AttachmentsState.AttachmentItem(
                        id = "mockId-1",
                        title = "mockFileName-1",
                        displaySize = "mockSizeName-1",
                    ),
                ),
                newAttachment = null,
            ),
            result,
        )
    }

    @Test
    fun `toViewState should return content without item when CipherView has no attachments`() {
        val cipherView = createMockCipherView(number = 1).copy(
            attachments = null,
        )

        val result = cipherView.toViewState()

        assertEquals(
            AttachmentsState.ViewState.Content(
                originalCipher = cipherView,
                attachments = emptyList(),
                newAttachment = null,
            ),
            result,
        )
    }

    @Test
    fun `toViewState should return content without items that have a null attachment ID`() {
        val cipherView = createMockCipherView(number = 1).copy(
            attachments = listOf(
                createMockAttachmentView(number = 1).copy(
                    id = null,
                ),
            ),
        )

        val result = cipherView.toViewState()

        assertEquals(
            AttachmentsState.ViewState.Content(
                originalCipher = cipherView,
                attachments = emptyList(),
                newAttachment = null,
            ),
            result,
        )
    }
}
