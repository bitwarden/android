package com.bitwarden.network.model

/**
 * Create a mock [AttachmentJsonResponse.Success] with a given [number].
 */
fun createMockAttachmentResponse(
    number: Int,
    attachmentId: String = "mockAttachmentId-$number",
    url: String = "mockUrl-$number",
    fileUploadType: FileUploadType = FileUploadType.AZURE,
    cipherResponse: SyncResponseJson.Cipher = createMockCipher(number = number),
): AttachmentJsonResponse.Success =
    AttachmentJsonResponse.Success(
        attachmentId = attachmentId,
        url = url,
        fileUploadType = fileUploadType,
        cipherResponse = cipherResponse,
    )
