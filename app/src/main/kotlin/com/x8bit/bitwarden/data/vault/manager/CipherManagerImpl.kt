package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import androidx.core.net.toUri
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.data.manager.model.DownloadResult
import com.bitwarden.network.model.AttachmentJsonResponse
import com.bitwarden.network.model.CreateCipherInOrganizationJsonRequest
import com.bitwarden.network.model.CreateCipherResponseJson
import com.bitwarden.network.model.ShareCipherJsonRequest
import com.bitwarden.network.model.UpdateCipherCollectionsJsonRequest
import com.bitwarden.network.model.UpdateCipherResponseJson
import com.bitwarden.network.service.CiphersService
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.EncryptionContext
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherUpsertData
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
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
import com.x8bit.bitwarden.data.vault.repository.util.toNetworkAttachmentRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import retrofit2.HttpException
import java.io.File
import java.time.Clock

/**
 * The default implementation of the [CipherManager].
 */
@Suppress("TooManyFunctions", "LongParameterList", "LargeClass")
class CipherManagerImpl(
    private val fileManager: FileManager,
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val ciphersService: CiphersService,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val clock: Clock,
    private val reviewPromptManager: ReviewPromptManager,
    dispatcherManager: DispatcherManager,
    pushManager: PushManager,
) : CipherManager {
    private val ioScope = CoroutineScope(dispatcherManager.io)
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    init {
        pushManager
            .syncCipherDeleteFlow
            .onEach(::deleteCipher)
            .launchIn(unconfinedScope)
        pushManager
            .syncCipherUpsertFlow
            .onEach(::syncCipherIfNecessary)
            .launchIn(ioScope)
    }

    override suspend fun createCipher(cipherView: CipherView): CreateCipherResult {
        val userId = activeUserId
            ?: return CreateCipherResult.Error(
                error = NoActiveUserException(),
                errorMessage = null,
            )
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap { ciphersService.createCipher(body = it.toEncryptedNetworkCipher()) }
            .map { response ->
                when (response) {
                    is CreateCipherResponseJson.Invalid -> {
                        CreateCipherResult.Error(
                            errorMessage = response.firstValidationErrorMessage,
                            error = null,
                        )
                    }

                    is CreateCipherResponseJson.Success -> {
                        vaultDiskSource.saveCipher(userId = userId, cipher = response.cipher)
                        CreateCipherResult.Success
                    }
                }
            }
            .fold(
                onFailure = { CreateCipherResult.Error(errorMessage = null, error = it) },
                onSuccess = {
                    reviewPromptManager.registerAddCipherAction()
                    it
                },
            )
    }

    override suspend fun createCipherInOrganization(
        cipherView: CipherView,
        collectionIds: List<String>,
    ): CreateCipherResult {
        val userId = activeUserId
            ?: return CreateCipherResult.Error(errorMessage = null, error = NoActiveUserException())
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap {
                ciphersService.createCipherInOrganization(
                    body = CreateCipherInOrganizationJsonRequest(
                        cipher = it.toEncryptedNetworkCipher(),
                        collectionIds = collectionIds,
                    ),
                )
            }
            .map { response ->
                when (response) {
                    is CreateCipherResponseJson.Invalid -> {
                        CreateCipherResult.Error(
                            errorMessage = response.firstValidationErrorMessage,
                            error = null,
                        )
                    }

                    is CreateCipherResponseJson.Success -> {
                        vaultDiskSource.saveCipher(
                            userId = userId,
                            cipher = response.cipher.copy(collectionIds = collectionIds),
                        )
                        CreateCipherResult.Success
                    }
                }
            }
            .fold(
                onFailure = { CreateCipherResult.Error(errorMessage = null, error = it) },
                onSuccess = {
                    reviewPromptManager.registerAddCipherAction()
                    it
                },
            )
    }

    override suspend fun hardDeleteCipher(cipherId: String): DeleteCipherResult {
        val userId = activeUserId
            ?: return DeleteCipherResult.Error(error = NoActiveUserException())
        return ciphersService
            .hardDeleteCipher(cipherId = cipherId)
            .onSuccess { vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId) }
            .fold(
                onSuccess = { DeleteCipherResult.Success },
                onFailure = { DeleteCipherResult.Error(error = it) },
            )
    }

    override suspend fun softDeleteCipher(
        cipherId: String,
        cipherView: CipherView,
    ): DeleteCipherResult {
        val userId = activeUserId
            ?: return DeleteCipherResult.Error(error = NoActiveUserException())
        return cipherView
            .encryptCipherAndCheckForMigration(userId = userId, cipherId = cipherId)
            .flatMap { encryptionContext ->
                ciphersService
                    .softDeleteCipher(cipherId = cipherId)
                    .flatMap {
                        vaultSdkSource.decryptCipher(
                            userId = userId,
                            cipher = encryptionContext.cipher,
                        )
                    }
            }
            .flatMap {
                vaultSdkSource.encryptCipher(
                    userId = userId,
                    cipherView = it.copy(deletedDate = clock.instant()),
                )
            }
            .onSuccess {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = it.toEncryptedNetworkCipherResponse(),
                )
            }
            .fold(
                onSuccess = { DeleteCipherResult.Success },
                onFailure = { DeleteCipherResult.Error(error = it) },
            )
    }

    override suspend fun deleteCipherAttachment(
        cipherId: String,
        attachmentId: String,
        cipherView: CipherView,
    ): DeleteAttachmentResult =
        deleteCipherAttachmentForResult(
            cipherId = cipherId,
            attachmentId = attachmentId,
            cipherView = cipherView,
        )
            .fold(
                onSuccess = { DeleteAttachmentResult.Success },
                onFailure = { DeleteAttachmentResult.Error(error = it) },
            )

    private suspend fun deleteCipherAttachmentForResult(
        cipherId: String,
        attachmentId: String,
        cipherView: CipherView,
    ): Result<EncryptionContext> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        return ciphersService
            .deleteCipherAttachment(
                cipherId = cipherId,
                attachmentId = attachmentId,
            )
            .flatMap {
                cipherView
                    .copy(
                        attachments = cipherView.attachments?.mapNotNull {
                            if (it.id == attachmentId) null else it
                        },
                    )
                    .encryptCipherAndCheckForMigration(userId = userId, cipherId = cipherId)
            }
            .onSuccess { encryptionContext ->
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = encryptionContext.toEncryptedNetworkCipherResponse(),
                )
            }
    }

    override suspend fun getCipher(cipherId: String): GetCipherResult {
        val userId = activeUserId ?: return GetCipherResult.Failure(NoActiveUserException())
        return vaultDiskSource
            .getCipher(userId = userId, cipherId = cipherId)
            ?.let { syncResponseCipher ->
                vaultSdkSource
                    .decryptCipher(
                        userId = userId,
                        cipher = syncResponseCipher.toEncryptedSdkCipher(),
                    )
                    .fold(
                        onSuccess = { GetCipherResult.Success(it) },
                        onFailure = { GetCipherResult.Failure(it) },
                    )
            }
            ?: GetCipherResult.CipherNotFound
    }

    override suspend fun restoreCipher(
        cipherId: String,
        cipherView: CipherView,
    ): RestoreCipherResult {
        val userId = activeUserId
            ?: return RestoreCipherResult.Error(error = NoActiveUserException())
        return ciphersService
            .restoreCipher(cipherId = cipherId)
            .onSuccess {
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = it.copy(collectionIds = cipherView.collectionIds),
                )
            }
            .fold(
                onSuccess = { RestoreCipherResult.Success },
                onFailure = { RestoreCipherResult.Error(error = it) },
            )
    }

    override suspend fun updateCipher(
        cipherId: String,
        cipherView: CipherView,
    ): UpdateCipherResult {
        val userId = activeUserId
            ?: return UpdateCipherResult.Error(errorMessage = null, error = NoActiveUserException())
        return vaultSdkSource
            .encryptCipher(
                userId = userId,
                cipherView = cipherView,
            )
            .flatMap {
                ciphersService.updateCipher(
                    cipherId = cipherId,
                    body = it.toEncryptedNetworkCipher(),
                )
            }
            .map { response ->
                when (response) {
                    is UpdateCipherResponseJson.Invalid -> {
                        UpdateCipherResult.Error(
                            errorMessage = response.firstValidationErrorMessage,
                            error = null,
                        )
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
                onFailure = { UpdateCipherResult.Error(errorMessage = null, error = it) },
                onSuccess = { it },
            )
    }

    override suspend fun shareCipher(
        cipherId: String,
        organizationId: String,
        cipherView: CipherView,
        collectionIds: List<String>,
    ): ShareCipherResult {
        val userId = activeUserId ?: return ShareCipherResult.Error(error = NoActiveUserException())
        return migrateAttachments(userId = userId, cipherView = cipherView)
            .flatMap {
                vaultSdkSource.moveToOrganization(
                    userId = userId,
                    organizationId = organizationId,
                    cipherView = it,
                )
            }
            .flatMap { vaultSdkSource.encryptCipher(userId = userId, cipherView = it) }
            .flatMap {
                ciphersService.shareCipher(
                    cipherId = cipherId,
                    body = ShareCipherJsonRequest(
                        cipher = it.toEncryptedNetworkCipher(),
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
                onFailure = { ShareCipherResult.Error(error = it) },
                onSuccess = { ShareCipherResult.Success },
            )
    }

    override suspend fun updateCipherCollections(
        cipherId: String,
        cipherView: CipherView,
        collectionIds: List<String>,
    ): ShareCipherResult {
        val userId = activeUserId ?: return ShareCipherResult.Error(error = NoActiveUserException())
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
            .onSuccess { encryptionContext ->
                vaultDiskSource.saveCipher(
                    userId = userId,
                    cipher = encryptionContext.toEncryptedNetworkCipherResponse(),
                )
            }
            .fold(
                onSuccess = { ShareCipherResult.Success },
                onFailure = { ShareCipherResult.Error(error = it) },
            )
    }

    override suspend fun createAttachment(
        cipherId: String,
        cipherView: CipherView,
        fileSizeBytes: String,
        fileName: String,
        fileUri: Uri,
    ): CreateAttachmentResult =
        createAttachmentForResult(
            cipherId = cipherId,
            cipherView = cipherView,
            fileSizeBytes = fileSizeBytes,
            fileName = fileName,
            fileUri = fileUri,
        )
            .fold(
                onFailure = {
                    CreateAttachmentResult.Error(
                        error = it,
                        message = when (it) {
                            is IllegalStateException -> it.message
                            else -> null
                        },
                    )
                },
                onSuccess = { CreateAttachmentResult.Success(cipherView = it) },
            )

    @Suppress("LongMethod")
    private suspend fun createAttachmentForResult(
        cipherId: String,
        cipherView: CipherView,
        fileSizeBytes: String?,
        fileName: String?,
        fileUri: Uri,
    ): Result<CipherView> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()
        val attachmentView = AttachmentView(
            id = null,
            url = null,
            size = fileSizeBytes,
            sizeName = null,
            fileName = fileName,
            key = null,
        )
        return cipherView
            .encryptCipherAndCheckForMigration(
                userId = userId,
                cipherId = requireNotNull(cipherView.id),
            )
            .flatMap { encryptionContext ->
                fileManager
                    .writeUriToCache(fileUri = fileUri)
                    .flatMap { cacheFile ->
                        vaultSdkSource
                            .encryptAttachment(
                                userId = userId,
                                cipher = encryptionContext.cipher,
                                attachmentView = attachmentView,
                                decryptedFilePath = cacheFile.absolutePath,
                                encryptedFilePath = "${cacheFile.absolutePath}.enc",
                            )
                            .flatMap { attachment ->
                                ciphersService
                                    .createAttachment(
                                        cipherId = cipherId,
                                        body = attachment.toNetworkAttachmentRequest(),
                                    )
                            }
                            .flatMap { attachmentResponse ->
                                when (attachmentResponse) {
                                    is AttachmentJsonResponse.Invalid -> {
                                        return IllegalStateException(
                                            attachmentResponse.message,
                                        ).asFailure()
                                    }

                                    is AttachmentJsonResponse.Success -> {
                                        val encryptedFile = File(
                                            "${cacheFile.absolutePath}.enc",
                                        )
                                        ciphersService
                                            .uploadAttachment(
                                                attachment = attachmentResponse,
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
    }

    override suspend fun downloadAttachment(
        cipherView: CipherView,
        attachmentId: String,
    ): DownloadAttachmentResult =
        downloadAttachmentForResult(
            cipherView = cipherView,
            attachmentId = attachmentId,
        )
            .fold(
                onSuccess = { DownloadAttachmentResult.Success(file = it) },
                onFailure = { DownloadAttachmentResult.Failure(error = it) },
            )

    private suspend fun downloadAttachmentForResult(
        cipherView: CipherView,
        attachmentId: String,
    ): Result<File> {
        val userId = activeUserId ?: return NoActiveUserException().asFailure()

        val cipher = cipherView
            .encryptCipherAndCheckForMigration(
                userId = userId,
                cipherId = requireNotNull(cipherView.id),
            )
            .fold(
                onSuccess = { it.cipher },
                onFailure = { return it.asFailure() },
            )
        val attachmentView = cipherView.attachments?.find { it.id == attachmentId }
            ?: return IllegalStateException("No attachment to download").asFailure()

        val attachmentData = ciphersService
            .getCipherAttachment(
                cipherId = requireNotNull(cipher.id),
                attachmentId = attachmentId,
            )
            .fold(
                onSuccess = { it },
                onFailure = { return it.asFailure() },
            )

        val url = attachmentData.url
            ?: return IllegalStateException("Attachment does not have a url").asFailure()

        val encryptedFile = when (val result = fileManager.downloadFileToCache(url)) {
            is DownloadResult.Failure -> {
                return IllegalStateException("Download failed", result.error).asFailure()
            }

            is DownloadResult.Success -> result.file
        }

        val decryptedFile = File(encryptedFile.path + "_decrypted")
        return vaultSdkSource
            .decryptFile(
                userId = userId,
                cipher = cipher,
                attachmentView = attachmentView,
                encryptedFilePath = encryptedFile.path,
                decryptedFilePath = decryptedFile.path,
            )
            .onSuccess { fileManager.delete(encryptedFile) }
            .onFailure { fileManager.delete(encryptedFile) }
            .map { decryptedFile }
    }

    /**
     * A helper method to check if the [CipherView] needs to be migrated when you encrypt it.
     */
    private suspend fun CipherView.encryptCipherAndCheckForMigration(
        userId: String,
        cipherId: String,
    ): Result<EncryptionContext> =
        vaultSdkSource
            .encryptCipher(userId = userId, cipherView = this)
            .flatMap { encryptionContext ->
                // We only migrate the cipher if the original cipher did not have a key and the
                // new cipher does. This means the SDK created the key and migration is required.
                if (encryptionContext.cipher.key != null && this.key == null) {
                    ciphersService
                        .updateCipher(
                            cipherId = cipherId,
                            body = encryptionContext.toEncryptedNetworkCipher(),
                        )
                        .flatMap { response ->
                            when (response) {
                                is UpdateCipherResponseJson.Invalid -> {
                                    IllegalStateException(response.firstValidationErrorMessage)
                                        .asFailure()
                                }

                                is UpdateCipherResponseJson.Success -> {
                                    vaultDiskSource.saveCipher(
                                        userId = userId,
                                        cipher = response.cipher,
                                    )
                                    encryptionContext
                                        .copy(cipher = response.cipher.toEncryptedSdkCipher())
                                        .asSuccess()
                                }
                            }
                        }
                } else {
                    encryptionContext.asSuccess()
                }
            }

    private suspend fun migrateAttachments(
        userId: String,
        cipherView: CipherView,
    ): Result<CipherView> {
        // Only run the migrations if we have attachments that do not have their own 'key'
        val attachmentViewsToMigrate = cipherView.attachments.orEmpty().filter { it.key == null }
        if (attachmentViewsToMigrate.none()) return cipherView.asSuccess()

        val cipherViewId = cipherView.id
            ?: return IllegalStateException("CipherView must have an ID").asFailure()
        var migratedCipherView = cipherView
            .encryptCipherAndCheckForMigration(userId = userId, cipherId = cipherViewId)
            .flatMap { vaultSdkSource.decryptCipher(userId = userId, cipher = it.cipher) }
            .getOrElse { return it.asFailure() }

        attachmentViewsToMigrate
            .map { attachmentView ->
                attachmentView
                    .id
                    ?.let { attachmentId ->
                        // This process downloads the attachment file and creates an entirely
                        // new attachment before deleting the original one
                        this@CipherManagerImpl
                            .downloadAttachmentForResult(
                                cipherView = migratedCipherView,
                                attachmentId = attachmentId,
                            )
                            .flatMap { file ->
                                this@CipherManagerImpl
                                    .createAttachmentForResult(
                                        cipherId = cipherViewId,
                                        cipherView = migratedCipherView,
                                        fileSizeBytes = attachmentView.size,
                                        fileName = attachmentView.fileName,
                                        fileUri = file.toUri(),
                                    )
                                    .onSuccess { fileManager.delete(file) }
                            }
                            .flatMap { cipherView ->
                                deleteCipherAttachmentForResult(
                                    cipherView = cipherView,
                                    attachmentId = attachmentId,
                                    cipherId = cipherViewId,
                                )
                            }
                            .flatMap {
                                vaultSdkSource.decryptCipher(
                                    userId = userId,
                                    cipher = it.cipher,
                                )
                            }
                            .onSuccess { migratedCipherView = it }
                    }
                    ?: IllegalStateException("AttachmentView must have an ID").asFailure()
            }
            .onEach { result ->
                // If anything fails, we consider the entire process to be a failure
                // The attachments will be partially migrated and that is OK
                result.onFailure { return it.asFailure() }
            }
        return migratedCipherView.asSuccess()
    }

    /**
     * Deletes the cipher specified by [syncCipherDeleteData] from disk.
     */
    private suspend fun deleteCipher(syncCipherDeleteData: SyncCipherDeleteData) {
        vaultDiskSource.deleteCipher(
            userId = syncCipherDeleteData.userId,
            cipherId = syncCipherDeleteData.cipherId,
        )
    }

    /**
     * Syncs an individual cipher contained in [syncCipherUpsertData] to disk if certain criteria
     * are met. If the resource cannot be found cloud-side, and it was updated, delete it from disk
     * for now.
     */
    private suspend fun syncCipherIfNecessary(syncCipherUpsertData: SyncCipherUpsertData) {
        val userId = syncCipherUpsertData.userId
        val cipherId = syncCipherUpsertData.cipherId
        val organizationId = syncCipherUpsertData.organizationId
        val collectionIds = syncCipherUpsertData.collectionIds
        val revisionDate = syncCipherUpsertData.revisionDate
        val isUpdate = syncCipherUpsertData.isUpdate

        // Return if local cipher is more recent
        val localCipher = vaultDiskSource.getCipher(userId = userId, cipherId = cipherId)
        if (localCipher != null &&
            localCipher.revisionDate.toEpochSecond() > revisionDate.toEpochSecond()
        ) {
            return
        }

        var shouldUpdate: Boolean
        val shouldCheckCollections: Boolean
        when {
            isUpdate -> {
                shouldUpdate = localCipher != null
                shouldCheckCollections = true
            }

            collectionIds == null || organizationId == null -> {
                shouldUpdate = localCipher == null
                shouldCheckCollections = false
            }

            else -> {
                shouldUpdate = false
                shouldCheckCollections = true
            }
        }

        if (!shouldUpdate && shouldCheckCollections && organizationId != null) {
            // Check if there are any collections in common
            shouldUpdate = vaultDiskSource
                .getCollections(userId = userId)
                .first()
                .any { collectionIds?.contains(it.id) == true }
        }

        if (!shouldUpdate) return
        if (activeUserId != userId) {
            // We cannot update right now since the accounts do not match, so we will
            // do a full-sync on the next check.
            settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = null)
            return
        }

        ciphersService
            .getCipher(cipherId = cipherId)
            .fold(
                onSuccess = { vaultDiskSource.saveCipher(userId = userId, cipher = it) },
                onFailure = {
                    // Delete any updates if it's missing from the server
                    val httpException = it as? HttpException
                    @Suppress("MagicNumber")
                    if (httpException?.code() == 404 && isUpdate) {
                        vaultDiskSource.deleteCipher(userId = userId, cipherId = cipherId)
                    }
                },
            )
    }
}
