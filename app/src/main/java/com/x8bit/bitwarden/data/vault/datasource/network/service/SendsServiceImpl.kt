package com.x8bit.bitwarden.data.vault.datasource.network.service

import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.vault.datasource.network.api.AzureApi
import com.x8bit.bitwarden.data.vault.datasource.network.api.SendsApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendFileResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SendJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateSendResponseJson
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
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
    override suspend fun createSend(body: SendJsonRequest): Result<SyncResponseJson.Send> =
        sendsApi.createSend(body = body)

    override suspend fun createFileSend(body: SendJsonRequest): Result<SendFileResponseJson> =
        sendsApi.createFileSend(body = body)

    override suspend fun updateSend(
        sendId: String,
        body: SendJsonRequest,
    ): Result<UpdateSendResponseJson> =
        sendsApi
            .updateSend(
                sendId = sendId,
                body = body,
            )
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
        sendFileResponse: SendFileResponseJson,
        encryptedFile: ByteArray,
    ): Result<SyncResponseJson.Send> {
        val send = sendFileResponse.sendResponse
        return when (sendFileResponse.fileUploadType) {
            SendFileResponseJson.FileUploadType.DIRECT -> {
                sendsApi.uploadFile(
                    sendId = requireNotNull(send.id),
                    fileId = requireNotNull(send.file?.id),
                    body = MultipartBody
                        .Builder(
                            boundary = "--BWMobileFormBoundary${clock.instant().toEpochMilli()}",
                        )
                        .addPart(
                            part = MultipartBody.Part.createFormData(
                                body = encryptedFile.toRequestBody(
                                    contentType = "application/octet-stream".toMediaType(),
                                ),
                                name = "data",
                                filename = send.file?.fileName,
                            ),
                        )
                        .build(),
                )
            }

            SendFileResponseJson.FileUploadType.AZURE -> {
                azureApi.uploadAzureBlob(
                    url = sendFileResponse.url,
                    date = DateTimeFormatter
                        .RFC_1123_DATE_TIME
                        .format(ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)),
                    version = sendFileResponse.url.toUri().getQueryParameter("sv"),
                    body = encryptedFile.toRequestBody(),
                )
            }
        }
            .onFailure { sendsApi.deleteSend(send.id) }
            .map { send }
    }

    override suspend fun deleteSend(sendId: String): Result<Unit> =
        sendsApi.deleteSend(sendId = sendId)

    override suspend fun removeSendPassword(sendId: String): Result<UpdateSendResponseJson> =
        sendsApi
            .removeSendPassword(sendId = sendId)
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
}
