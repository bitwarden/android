package com.x8bit.bitwarden.data.vault.manager

import android.net.Uri
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.core.data.util.flatMap
import com.bitwarden.data.manager.file.FileManager
import com.bitwarden.network.model.CreateFileSendResponse
import com.bitwarden.network.model.CreateSendJsonResponse
import com.bitwarden.network.model.UpdateSendResponseJson
import com.bitwarden.network.service.SendsService
import com.bitwarden.send.Send
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.error.NoActiveUserException
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.ReviewPromptManager
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendUpsertData
import com.x8bit.bitwarden.data.vault.datasource.disk.VaultDiskSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.repository.model.CreateSendResult
import com.x8bit.bitwarden.data.vault.repository.model.DeleteSendResult
import com.x8bit.bitwarden.data.vault.repository.model.RemovePasswordSendResult
import com.x8bit.bitwarden.data.vault.repository.model.UpdateSendResult
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedNetworkSend
import com.x8bit.bitwarden.data.vault.repository.util.toEncryptedSdkSend
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import retrofit2.HttpException

/**
 * The default implementation of the [SendManager].
 */
@Suppress("LongParameterList")
class SendManagerImpl(
    private val authDiskSource: AuthDiskSource,
    private val settingsDiskSource: SettingsDiskSource,
    private val vaultDiskSource: VaultDiskSource,
    private val vaultSdkSource: VaultSdkSource,
    private val sendsService: SendsService,
    private val fileManager: FileManager,
    private val reviewPromptManager: ReviewPromptManager,
    pushManager: PushManager,
    dispatcherManager: DispatcherManager,
) : SendManager {
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)
    private val ioScope = CoroutineScope(dispatcherManager.io)

    private val activeUserId: String? get() = authDiskSource.userState?.activeUserId

    init {
        pushManager
            .syncSendDeleteFlow
            .onEach(::deleteSend)
            .launchIn(unconfinedScope)
        pushManager
            .syncSendUpsertFlow
            .onEach(::syncSendIfNecessary)
            .launchIn(ioScope)
    }

    override suspend fun createSend(
        sendView: SendView,
        fileUri: Uri?,
    ): CreateSendResult {
        val userId = activeUserId
            ?: return CreateSendResult.Error(message = null, error = NoActiveUserException())
        return vaultSdkSource
            .encryptSend(userId = userId, sendView = sendView)
            .flatMap { send ->
                when (send.type) {
                    SendType.TEXT -> sendsService.createTextSend(send.toEncryptedNetworkSend())
                    SendType.FILE -> createFileSend(uri = fileUri, userId = userId, send = send)
                }
            }
            .map { createSendResponse ->
                when (createSendResponse) {
                    is CreateSendJsonResponse.Invalid -> {
                        return CreateSendResult.Error(
                            message = createSendResponse.firstValidationErrorMessage,
                            error = null,
                        )
                    }

                    is CreateSendJsonResponse.Success -> {
                        // Save the send immediately, regardless of whether the decrypt succeeds
                        vaultDiskSource.saveSend(userId = userId, send = createSendResponse.send)
                        createSendResponse
                    }
                }
            }
            .flatMap { createSendSuccessResponse ->
                vaultSdkSource.decryptSend(
                    userId = userId,
                    send = createSendSuccessResponse.send.toEncryptedSdkSend(),
                )
            }
            .fold(
                onFailure = { CreateSendResult.Error(message = null, error = it) },
                onSuccess = {
                    reviewPromptManager.registerCreateSendAction()
                    CreateSendResult.Success(sendView = it)
                },
            )
    }

    override suspend fun deleteSend(sendId: String): DeleteSendResult {
        val userId = activeUserId ?: return DeleteSendResult.Error(error = NoActiveUserException())
        return sendsService
            .deleteSend(sendId)
            .onSuccess { vaultDiskSource.deleteSend(userId = userId, sendId = sendId) }
            .fold(
                onSuccess = { DeleteSendResult.Success },
                onFailure = { DeleteSendResult.Error(error = it) },
            )
    }

    override suspend fun removePasswordSend(sendId: String): RemovePasswordSendResult {
        val userId = activeUserId ?: return RemovePasswordSendResult.Error(
            errorMessage = null,
            error = NoActiveUserException(),
        )
        return sendsService
            .removeSendPassword(sendId = sendId)
            .fold(
                onSuccess = { response ->
                    when (response) {
                        is UpdateSendResponseJson.Invalid -> {
                            RemovePasswordSendResult.Error(
                                errorMessage = response.message,
                                error = null,
                            )
                        }

                        is UpdateSendResponseJson.Success -> {
                            vaultDiskSource.saveSend(userId = userId, send = response.send)
                            vaultSdkSource
                                .decryptSend(
                                    userId = userId,
                                    send = response.send.toEncryptedSdkSend(),
                                )
                                .fold(
                                    onSuccess = { RemovePasswordSendResult.Success(sendView = it) },
                                    onFailure = {
                                        RemovePasswordSendResult.Error(
                                            errorMessage = null,
                                            error = it,
                                        )
                                    },
                                )
                        }
                    }
                },
                onFailure = { RemovePasswordSendResult.Error(errorMessage = null, error = it) },
            )
    }

    override suspend fun updateSend(
        sendId: String,
        sendView: SendView,
    ): UpdateSendResult {
        val userId = activeUserId ?: return UpdateSendResult.Error(
            errorMessage = null,
            error = NoActiveUserException(),
        )
        return vaultSdkSource
            .encryptSend(userId = userId, sendView = sendView)
            .flatMap { send ->
                sendsService.updateSend(sendId = sendId, body = send.toEncryptedNetworkSend())
            }
            .fold(
                onFailure = { UpdateSendResult.Error(errorMessage = null, error = it) },
                onSuccess = { response ->
                    when (response) {
                        is UpdateSendResponseJson.Invalid -> {
                            UpdateSendResult.Error(errorMessage = response.message, error = null)
                        }

                        is UpdateSendResponseJson.Success -> {
                            vaultDiskSource.saveSend(userId = userId, send = response.send)
                            vaultSdkSource
                                .decryptSend(
                                    userId = userId,
                                    send = response.send.toEncryptedSdkSend(),
                                )
                                .fold(
                                    onSuccess = { UpdateSendResult.Success(sendView = it) },
                                    onFailure = {
                                        UpdateSendResult.Error(errorMessage = null, error = it)
                                    },
                                )
                        }
                    }
                },
            )
    }

    private suspend fun createFileSend(
        uri: Uri?,
        userId: String,
        send: Send,
    ): Result<CreateSendJsonResponse> {
        uri ?: return IllegalArgumentException("File URI must be present to create a File Send.")
            .asFailure()

        return fileManager
            .writeUriToCache(uri)
            .flatMap { file ->
                vaultSdkSource.encryptFile(
                    userId = userId,
                    send = send,
                    path = file.absolutePath,
                    destinationFilePath = file.absolutePath,
                )
            }
            .flatMap { encryptedFile ->
                sendsService
                    .createFileSend(
                        body = send.toEncryptedNetworkSend(fileLength = encryptedFile.length()),
                    )
                    .flatMap { sendFileResponse ->
                        when (sendFileResponse) {
                            is CreateFileSendResponse.Invalid -> {
                                CreateSendJsonResponse
                                    .Invalid(
                                        message = sendFileResponse.message,
                                        validationErrors = sendFileResponse.validationErrors,
                                    )
                                    .asSuccess()
                            }

                            is CreateFileSendResponse.Success -> {
                                sendsService
                                    .uploadFile(
                                        sendFileResponse = sendFileResponse.createFileJsonResponse,
                                        encryptedFile = encryptedFile,
                                    )
                                    .also {
                                        // Delete encrypted file once it has been uploaded.
                                        fileManager.delete(encryptedFile)
                                    }
                                    .map { CreateSendJsonResponse.Success(it) }
                            }
                        }
                    }
            }
    }

    /**
     * Deletes the send specified by [syncSendDeleteData] from disk.
     */
    private suspend fun deleteSend(syncSendDeleteData: SyncSendDeleteData) {
        vaultDiskSource.deleteSend(
            userId = syncSendDeleteData.userId,
            sendId = syncSendDeleteData.sendId,
        )
    }

    /**
     * Syncs an individual send contained in [syncSendUpsertData] to disk if certain criteria are
     * met. If the resource cannot be found cloud-side, and it was updated, delete it from disk for
     * now.
     */
    private suspend fun syncSendIfNecessary(syncSendUpsertData: SyncSendUpsertData) {
        val userId = syncSendUpsertData.userId
        val sendId = syncSendUpsertData.sendId
        val isUpdate = syncSendUpsertData.isUpdate
        val revisionDate = syncSendUpsertData.revisionDate
        val localSend = vaultDiskSource
            .getSends(userId = userId)
            .first()
            .find { it.id == sendId }
        val isValidCreate = !isUpdate && localSend == null
        val isValidUpdate = isUpdate &&
            localSend != null &&
            localSend.revisionDate.toEpochSecond() < revisionDate.toEpochSecond()
        if (!isValidCreate && !isValidUpdate) return
        if (activeUserId != userId) {
            // We cannot update right now since the accounts do not match, so we will
            // do a full-sync on the next check.
            settingsDiskSource.storeLastSyncTime(userId = userId, lastSyncTime = null)
            return
        }

        sendsService
            .getSend(sendId = sendId)
            .fold(
                onSuccess = { vaultDiskSource.saveSend(userId = userId, send = it) },
                onFailure = {
                    // Delete any updates if it's missing from the server
                    val httpException = it as? HttpException
                    @Suppress("MagicNumber")
                    if (httpException?.code() == 404 && isUpdate) {
                        vaultDiskSource.deleteSend(userId = userId, sendId = sendId)
                    }
                },
            )
    }
}
