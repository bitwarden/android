package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.vault.repository.model.CreateAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.RestoreCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.ShareCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult

/**
 * Manages the creating, updating, and deleting ciphers and their attachments.
 */
@Suppress("TooManyFunctions")
interface CipherManager {
    /**
     * Attempt to create a cipher.
     */
    suspend fun createCipher(
        cipherView: CipherView,
    ): CreateCipherResult

    /**
     * Attempt to create a cipher that belongs to an organization.
     */
    suspend fun createCipherInOrganization(
        cipherView: CipherView,
        collectionIds: List<String>,
    ): CreateCipherResult

    /**
     * Attempt to create an attachment for the given [cipherView].
     */
    suspend fun createAttachment(
        cipherId: String,
        cipherView: CipherView,
        fileSizeBytes: String,
        fileName: String,
        fileUri: Uri,
    ): CreateAttachmentResult

    /**
     * Attempt to download an attachment file, specified by [attachmentId], for the given
     * [cipherView].
     */
    suspend fun downloadAttachment(
        cipherView: CipherView,
        attachmentId: String,
    ): DownloadAttachmentResult

    /**
     * Attempt to delete a cipher.
     */
    suspend fun hardDeleteCipher(
        cipherId: String,
    ): DeleteCipherResult

    /**
     * Attempt to soft delete a cipher.
     */
    suspend fun softDeleteCipher(
        cipherId: String,
        cipherView: CipherView,
    ): DeleteCipherResult

    /**
     * Attempt to delete an attachment from a send.
     */
    suspend fun deleteCipherAttachment(
        cipherId: String,
        attachmentId: String,
        cipherView: CipherView,
    ): DeleteAttachmentResult

    /**
     * Attempt to restore a cipher.
     */
    suspend fun restoreCipher(
        cipherId: String,
        cipherView: CipherView,
    ): RestoreCipherResult

    /**
     * Attempt to share a cipher to the collections with the given collectionIds.
     */
    suspend fun shareCipher(
        cipherId: String,
        organizationId: String,
        cipherView: CipherView,
        collectionIds: List<String>,
    ): ShareCipherResult

    /**
     * Attempt to update a cipher.
     */
    suspend fun updateCipher(
        cipherId: String,
        cipherView: CipherView,
    ): UpdateCipherResult

    /**
     * Attempt to update a cipher with the given collectionIds.
     */
    suspend fun updateCipherCollections(
        cipherId: String,
        cipherView: CipherView,
        collectionIds: List<String>,
    ): ShareCipherResult
}
