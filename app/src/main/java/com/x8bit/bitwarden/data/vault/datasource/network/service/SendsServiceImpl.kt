package com.x8bit.bitwarden.data.vault.datasource.network.service

import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.platform.datasource.network.util.toResult
import com.x8bit.bitwarden.data.vault.datasource.network.api.AzureApi
import com.x8bit.bitwarden.data.vault.datasource.network.api.SendsApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateFileSendResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateFileSendResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateSendJsonResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.FileUploadType
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateSendResponseJson
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Clock
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Default implementation of the [SendsService].
 */
class SendsServiceImpl(
    private val azureApi: AzureApi,
    private val sendsApi: SendsApi,
    private val clock: Clock,
    private val json: Json,
) : SendsService {
    override suspend fun createTextSend(
        body: SendJsonRequest,
    ): Result<CreateSendJsonResponse> =
        sendsApi
            .createTextSend(body = body)
            .toResult()
            .map { CreateSendJsonResponse.Success(send = it) }
            .recoverCatching { throwable ->
                throwable.toBitwardenError()
                    .parseErrorBodyOrNull<CreateSendJsonResponse.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun createFileSend(
        body: SendJsonRequest,
    ): Result<CreateFileSendResponse> =
        sendsApi
            .createFileSend(body = body)
            .toResult()
            .map { CreateFileSendResponse.Success(it) }
            .recoverCatching { throwable ->
                throwable.toBitwardenError()
                    .parseErrorBodyOrNull<CreateFileSendResponse.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun updateSend(
        sendId: String,
        body: SendJsonRequest,
    ): Result<UpdateSendResponseJson> =
        sendsApi
            .updateSend(
                sendId = sendId,
                body = body,
            )
            .toResult()
            .map { UpdateSendResponseJson.Success(send = it) }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<UpdateSendResponseJson.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun uploadFile(
        sendFileResponse: CreateFileSendResponseJson,
        encryptedFile: File,
    ): Result<SyncResponseJson.Send> {
        val send = sendFileResponse.sendResponse
        return when (sendFileResponse.fileUploadType) {
            FileUploadType.DIRECT -> {
                sendsApi.uploadFile(
                    sendId = requireNotNull(send.id),
                    fileId = requireNotNull(send.file?.id),
                    body = MultipartBody
                        .Builder(
                            boundary = "--BWMobileFormBoundary${clock.instant().toEpochMilli()}",
                        )
                        .setType(type = MultipartBody.FORM)
                        .addPart(
                            part = MultipartBody.Part.createFormData(
                                body = encryptedFile.asRequestBody(
                                    contentType = "application/octet-stream".toMediaType(),
                                ),
                                name = "data",
                                filename = send.file?.fileName,
                            ),
                        )
                        .build(),
                )
            }

            FileUploadType.AZURE -> {
                azureApi.uploadAzureBlob(
                    url = sendFileResponse.url,
                    date = DateTimeFormatter
                        .RFC_1123_DATE_TIME
                        .format(ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)),
                    version = sendFileResponse.url.toUri().getQueryParameter("sv"),
                    body = encryptedFile.asRequestBody(),
                )
            }
        }
            .toResult()
            .onFailure { sendsApi.deleteSend(send.id) }
            .map { send }
    }

    override suspend fun deleteSend(sendId: String): Result<Unit> =
        sendsApi
            .deleteSend(sendId = sendId)
            .toResult()

    override suspend fun removeSendPassword(sendId: String): Result<UpdateSendResponseJson> =
        sendsApi
            .removeSendPassword(sendId = sendId)
            .toResult()
            .map { UpdateSendResponseJson.Success(send = it) }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<UpdateSendResponseJson.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun getSend(
        sendId: String,
    ): Result<SyncResponseJson.Send> =
        sendsApi
            .getSend(sendId = sendId)
            .toResult()
}
