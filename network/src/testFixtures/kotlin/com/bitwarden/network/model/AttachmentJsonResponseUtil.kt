package com.bitwarden.network.model

/**
 * Create a mock [AttachmentJsonResponse] with a given [number].
 */
fun createMockAttachmentJsonResponse(
    number: Int,
    fileUploadType: FileUploadType = FileUploadType.AZURE,
): AttachmentJsonResponse =
    AttachmentJsonResponse.Success(
        attachmentId = "mockAttachmentId-$number",
        url = "mockUrl-$number",
        fileUploadType = fileUploadType,
        cipherResponse = createMockCipher(number = number),
    )

/**
 * Create a mock [AttachmentJsonResponse.Success] with a given [number].
 */
fun createMockAttachmentResponse(
    number: Int,
    fileUploadType: FileUploadType = FileUploadType.AZURE,
): AttachmentJsonResponse.Success =
    AttachmentJsonResponse.Success(
        attachmentId = "mockAttachmentId-$number",
        url = "mockUrl-$number",
        fileUploadType = fileUploadType,
        cipherResponse = createMockCipher(number = number),
    )
