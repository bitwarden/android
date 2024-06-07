package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import com.bitwarden.core.AttachmentView
import com.bitwarden.core.CipherView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.util.flatMap
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.network.model.AttachmentJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateCipherInOrganizationJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherCollectionsJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.service.CiphersService
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.DownloadResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.CreateCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.DownloadAttachmentResult
import com.x8bit.bitwarden.data.vault.repository.model.RestoreCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.ShareCipherResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateCipherResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipher
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkCipherResponse
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkCipher
import java.io.File
import java.time.Clock

/**
 * The default implementation of the [CipherManager].
 */
@Suppress("TooManyFunctions")
class CipherManagerImpl(
    private val fileManager: FileManager,
    private val authDiskSource: AuthDiskSource,
    private val ciphersService: CiphersService,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val clock: Clock,
) : CipherManager {
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    override suspend fun createCipher(cipherView: CipherView): CreateCipherResult {
        val userId = activeUserId ?: return CreateCipherResult.Error
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap { ciphersService.createCipher(body = it.toEncryptedNetworkCipher()) }
            .onSuccess { vaultDiskSource.saveCipher(userId = userId, cipher = it) }
            .fold(
                onFailure = { CreateCipherResult.Error },
                onSuccess = { CreateCipherResult.Success },
            )
    }

    override suspend fun createCipherInOrganization(
        cipherView: CipherView,
        collectionIds: List<String>,
    ): CreateCipherResult {
        val userId = activeUserId ?: return CreateCipherResult.Error
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap { cipher ->
                ciphersService.createCipherInOrganization(
                    body = CreateCipherInOrganizationJsonRequest(
                        cipher = cipher.toEncryptedNetworkCipher(),
                        collectionIds = collectionIds,
                    ),
                )
            }
            .onSuccess {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = it.copy(collectionIds = collectionIds),
                )
            }
            .fold(
                onFailure = { CreateCipherResult.Error },
                onSuccess = { CreateCipherResult.Success },
            )
    }

    override suspend fun hardDeleteCipher(cipherId: String): DeleteCipherResult {
        val userId = activeUserId ?: return DeleteCipherResult.Error
        return ciphersService
            .hardDeleteCipher(cipherId = cipherId)
            .onSuccess { vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId) }
            .fold(
                onSuccess = { DeleteCipherResult.Success },
                onFailure = { DeleteCipherResult.Error },
            )
    }

    override suspend fun softDeleteCipher(
        cipherId: String,
        cipherView: CipherView,
    ): DeleteCipherResult {
        val userId = activeUserId ?: return DeleteCipherResult.Error
        return ciphersService
            .softDeleteCipher(cipherId = cipherId)
            .onSuccess {
                vaultSdkSource
                    .encryptCipher(
                        userId = userId,
                        cipherView = cipherView.copy(deletedDate = clock.instant()),
                    )
                    .onSuccess { cipher ->
                        vaultDiskSource.saveCipher(
                            userId = userId,
                            cipher = cipher.toEncryptedNetworkCipherResponse(),
                        )
                    }
            }
            .fold(
                onSuccess = { DeleteCipherResult.Success },
                onFailure = { DeleteCipherResult.Error },
            )
    }

    override suspend fun deleteCipherAttachment(
        cipherId: String,
        attachmentId: String,
        cipherView: CipherView,
    ): DeleteAttachmentResult {
        val userId = activeUserId ?: return DeleteAttachmentResult.Error
        return ciphersService
            .deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
            )
            .flatMap {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = cipherView.copy(
                        attachments = cipherView.attachments?.mapNotNull {
                            if (it.id == attachmentId) null else it
                        },
                    ),
                )
            }
            .onSuccess { cipher ->
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = cipher.toEncryptedNetworkCipherResponse(),
                )
            }
            .fold(
                onSuccess = { DeleteAttachmentResult.Success },
                onFailure = { DeleteAttachmentResult.Error },
            )
    }

    override suspend fun restoreCipher(
        cipherId: String,
        cipherView: CipherView,
    ): RestoreCipherResult {
        val userId = activeUserId ?: return RestoreCipherResult.Error
        return ciphersService
            .restoreCipher(cipherId = cipherId)
            .flatMap {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = cipherView.copy(deletedDate = null),
                )
            }
            .onSuccess { cipher ->
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = cipher.toEncryptedNetworkCipherResponse(),
                )
            }
            .fold(
                onSuccess = { RestoreCipherResult.Success },
                onFailure = { RestoreCipherResult.Error },
            )
    }

    override suspend fun updateCipher(
        cipherId: String,
        cipherView: CipherView,
    ): UpdateCipherResult {
        val userId = activeUserId ?: return UpdateCipherResult.Error(errorMessage = null)
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap { cipher ->
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = cipher.toEncryptedNetworkCipher(),
                )
            }
            .map { response ->
                when (response) {
                    is UpdateCipherResponseJson.Invalid -> {
                        UpdateCipherResult.Error(errorMessage = response.message)
                    }

                    is UpdateCipherResponseJson.Success -> {
                        vaultDiskSource.saveCipher(
                            userId = userId,
                            cipher = response.cipher.copy(collectionIds = cipherView.collectionIds),
                        )
                        UpdateCipherResult.Success
                    }
                }
            }
            .fold(
                onFailure = { UpdateCipherResult.Error(errorMessage = null) },
                onSuccess = { it },
            )
    }

    override suspend fun shareCipher(
        cipherId: String,
        organizationId: String,
        cipherView: CipherView,
        collectionIds: List<String>,
    ): ShareCipherResult {
        val userId = activeUserId ?: return ShareCipherResult.Error
        return vaultSdkSource
            .moveToOrganization(
                userId = userId,
                organizationId = organizationId,
                cipherView = cipherView,
            )
            .flatMap { vaultSdkSource.encryptCipher(userId = userId, cipherView = it) }
            .flatMap { cipher ->
                ciphersService.shareCipher(
                    cipherId = cipherId,
                    body = ShareCipherJsonRequest(
                        cipher = cipher.toEncryptedNetworkCipher(),
                        collectionIds = collectionIds,
                    ),
                )
            }
            .onSuccess {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = it.copy(collectionIds = collectionIds),
                )
            }
            .fold(
                onFailure = { ShareCipherResult.Error },
                onSuccess = { ShareCipherResult.Success },
            )
    }

    override suspend fun updateCipherCollections(
        cipherId: String,
        cipherView: CipherView,
        collectionIds: List<String>,
    ): ShareCipherResult {
        val userId = activeUserId ?: return ShareCipherResult.Error
        return ciphersService
            .updateCipherCollections(
                cipherId = cipherId,
                body = UpdateCipherCollectionsJsonRequest(collectionIds = collectionIds),
            )
            .flatMap {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = cipherView.copy(collectionIds = collectionIds),
                )
            }
            .onSuccess { cipher ->
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = cipher.toEncryptedNetworkCipherResponse(),
                )
            }
            .fold(
                onSuccess = { ShareCipherResult.Success },
                onFailure = { ShareCipherResult.Error },
            )
    }

    @Suppress("LongMethod")
    override suspend fun createAttachment(
        cipherId: String,
        cipherView: CipherView,
        fileSizeBytes: String,
        fileName: String,
        fileUri: Uri,
    ): CreateAttachmentResult {
        val userId = activeUserId ?: return CreateAttachmentResult.Error
        val attachmentView = AttachmentView(
            id = null,
            url = null,
            size = fileSizeBytes,
            sizeName = null,
            fileName = fileName,
            key = null,
        )
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap { cipher ->
                fileManager
                    .writeUriToCache(fileUri = fileUri)
                    .flatMap { cacheFile ->
                        vaultSdkSource
                            .encryptAttachment(
                                userId = userId,
                                cipher = cipher,
                                attachmentView = attachmentView,
                                decryptedFilePath = cacheFile.absolutePath,
                                encryptedFilePath = "${cacheFile.absolutePath}.enc",
                            )
                            .flatMap { attachment ->
                                ciphersService
                                    .createAttachment(
                                        cipherId = cipherId,
                                        body = AttachmentJsonRequest(
                                            // We know these values are present because
                                            // - the filename/size are passed into the function
                                            // - the SDK call fills in the key
                                            fileName = requireNotNull(attachment.fileName),
                                            key = requireNotNull(attachment.key),
                                            fileSize = requireNotNull(attachment.size),
                                        ),
                                    )
                                    .flatMap { attachmentJsonResponse ->
                                        val encryptedFile = File("${cacheFile.absolutePath}.enc")
                                        ciphersService
                                            .uploadAttachment(
                                                attachmentJsonResponse = attachmentJsonResponse,
                                                encryptedFile = encryptedFile,
                                            )
                                            .onSuccess {
                                                fileManager.delete(cacheFile, encryptedFile)
                                            }
                                            .onFailure {
                                                fileManager.delete(cacheFile, encryptedFile)
                                            }
                                    }
                            }
                    }
            }
            .map { it.copy(collectionIds = cipherView.collectionIds) }
            .onSuccess {
                // Save the send immediately, regardless of whether the decrypt succeeds
                vaultDiskSource.saveCipher(userId = userId, cipher = it)
            }
            .flatMap {
                vaultSdkSource.decryptCipher(
                    userId = userId,
                    cipher = it.toEncryptedSdkCipher(),
                )
            }
            .fold(
                onFailure = { CreateAttachmentResult.Error },
                onSuccess = { CreateAttachmentResult.Success(cipherView = it) },
            )
    }

    @Suppress("ReturnCount")
    override suspend fun downloadAttachment(
        cipherView: CipherView,
        attachmentId: String,
    ): DownloadAttachmentResult {
        val userId = activeUserId ?: return DownloadAttachmentResult.Failure

        val cipher = vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .fold(
                onSuccess = { it },
                onFailure = { return DownloadAttachmentResult.Failure },
            )
        val attachment = cipher.attachments?.find { it.id == attachmentId }
            ?: return DownloadAttachmentResult.Failure

        val attachmentData = ciphersService
            .getCipherAttachment(
                cipherId = requireNotNull(cipher.id),
                attachmentId = attachmentId,
            )
            .fold(
                onSuccess = { it },
                onFailure = { return DownloadAttachmentResult.Failure },
            )

        val url = attachmentData.url ?: return DownloadAttachmentResult.Failure

        val encryptedFile = when (val result = fileManager.downloadFileToCache(url)) {
            DownloadResult.Failure -> return DownloadAttachmentResult.Failure
            is DownloadResult.Success -> result.file
        }

        val decryptedFile = File(encryptedFile.path + "_decrypted")
        return vaultSdkSource
            .decryptFile(
                userId = userId,
                cipher = cipher,
                attachment = attachment,
                encryptedFilePath = encryptedFile.path,
                decryptedFilePath = decryptedFile.path,
            )
            .onSuccess { fileManager.delete(encryptedFile) }
            .onFailure { fileManager.delete(encryptedFile) }
            .fold(
                onSuccess = { DownloadAttachmentResult.Success(file = decryptedFile) },
                onFailure = { DownloadAttachmentResult.Failure },
            )
    }
}
