package com.x8bit.bitwarden.data.vault.datasource.network.service

import androidx.core.net.toUri
import com.x8bit.bitwarden.data.platform.datasource.network.model.toBitwardenError
import com.x8bit.bitwarden.data.platform.datasource.network.util.parseErrorBodyOrNull
import com.x8bit.bitwarden.data.vault.datasource.network.api.AzureApi
import com.x8bit.bitwarden.data.vault.datasource.network.api.CiphersApi
import com.x8bit.bitwarden.data.vault.datasource.network.model.AttachmentJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.AttachmentJsonResponse
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.CreateCipherInOrganizationJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.FileUploadType
import com.x8bit.bitwarden.data.vault.datasource.network.model.ShareCipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherCollectionsJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.UpdateCipherResponseJson
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.time.Clock
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Suppress("TooManyFunctions")
class CiphersServiceImpl(
    private val azureApi: AzureApi,
    private val ciphersApi: CiphersApi,
    private val json: Json,
    private val clock: Clock,
) : CiphersService {
    override suspend fun createCipher(body: CipherJsonRequest): Result<SyncResponseJson.Cipher> =
        ciphersApi.createCipher(body = body)

    override suspend fun createCipherInOrganization(
        body: CreateCipherInOrganizationJsonRequest,
    ): Result<SyncResponseJson.Cipher> = ciphersApi.createCipherInOrganization(body = body)

    override suspend fun createAttachment(
        cipherId: String,
        body: AttachmentJsonRequest,
    ): Result<AttachmentJsonResponse> =
        ciphersApi.createAttachment(
            cipherId = cipherId,
            body = body,
        )

    override suspend fun uploadAttachment(
        attachmentJsonResponse: AttachmentJsonResponse,
        encryptedFile: File,
    ): Result<SyncResponseJson.Cipher> {
        val cipher = attachmentJsonResponse.cipherResponse
        return when (attachmentJsonResponse.fileUploadType) {
            FileUploadType.DIRECT -> {
                ciphersApi.uploadAttachment(
                    cipherId = requireNotNull(cipher.id),
                    attachmentId = attachmentJsonResponse.attachmentId,
                    body = MultipartBody
                        .Builder(
                            boundary = "--BWMobileFormBoundary${clock.instant().toEpochMilli()}",
                        )
                        .addPart(
                            part = MultipartBody.Part.createFormData(
                                body = encryptedFile.asRequestBody(
                                    contentType = "application/octet-stream".toMediaType(),
                                ),
                                name = "data",
                                filename = cipher
                                    .attachments
                                    ?.find { it.id == attachmentJsonResponse.attachmentId }
                                    ?.fileName,
                            ),
                        )
                        .build(),
                )
            }

            FileUploadType.AZURE -> {
                azureApi.uploadAzureBlob(
                    url = attachmentJsonResponse.url,
                    date = DateTimeFormatter
                        .RFC_1123_DATE_TIME
                        .format(ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)),
                    version = attachmentJsonResponse.url.toUri().getQueryParameter("sv"),
                    body = encryptedFile.asRequestBody(),
                )
            }
        }
            .map { cipher }
    }

    override suspend fun updateCipher(
        cipherId: String,
        body: CipherJsonRequest,
    ): Result<UpdateCipherResponseJson> =
        ciphersApi
            .updateCipher(
                cipherId = cipherId,
                body = body,
            )
            .map { UpdateCipherResponseJson.Success(cipher = it) }
            .recoverCatching { throwable ->
                throwable
                    .toBitwardenError()
                    .parseErrorBodyOrNull<UpdateCipherResponseJson.Invalid>(
                        code = 400,
                        json = json,
                    )
                    ?: throw throwable
            }

    override suspend fun shareCipher(
        cipherId: String,
        body: ShareCipherJsonRequest,
    ): Result<SyncResponseJson.Cipher> =
        ciphersApi.shareCipher(
            cipherId = cipherId,
            body = body,
        )

    override suspend fun updateCipherCollections(
        cipherId: String,
        body: UpdateCipherCollectionsJsonRequest,
    ): Result<Unit> =
        ciphersApi.updateCipherCollections(
            cipherId = cipherId,
            body = body,
        )

    override suspend fun hardDeleteCipher(cipherId: String): Result<Unit> =
        ciphersApi.hardDeleteCipher(cipherId = cipherId)

    override suspend fun softDeleteCipher(cipherId: String): Result<Unit> =
        ciphersApi.softDeleteCipher(cipherId = cipherId)

    override suspend fun deleteCipherAttachment(
        cipherId: String,
        attachmentId: String,
    ): Result<Unit> =
        ciphersApi.deleteCipherAttachment(
            cipherId = cipherId,
            attachmentId = attachmentId,
        )

    override suspend fun restoreCipher(cipherId: String): Result<SyncResponseJson.Cipher> =
        ciphersApi.restoreCipher(cipherId = cipherId)

    override suspend fun getCipher(
        cipherId: String,
    ): Result<SyncResponseJson.Cipher> =
        ciphersApi.getCipher(cipherId = cipherId)

    override suspend fun getCipherAttachment(
        cipherId: String,
        attachmentId: String,
    ): Result<SyncResponseJson.Cipher.Attachment> =
        ciphersApi.getCipherAttachment(
            cipherId = cipherId,
            attachmentId = attachmentId,
        )

    override suspend fun hasUnassignedCiphers(): Result<Boolean> =
        ciphersApi.hasUnassignedCiphers()
}
