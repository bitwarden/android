package com.x8bit.bitwarden.data.vault.datasource.network.model

/**
 * Create a mock [AttachmentJsonResponse] with a given [number].
 */
fun createMockAttachmentJsonResponse(
    number: Int,
    fileUploadType: FileUploadType = FileUploadType.AZURE,
): AttachmentJsonResponse =
    AttachmentJsonResponse(
        attachmentId = "mockAttachmentId-$number",
        url = "mockUrl-$number",
        fileUploadType = fileUploadType,
        cipherResponse = createMockCipher(number = number),
    )
