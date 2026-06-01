package com.bitwarden.network.model

/**
 * Create a mock [CipherJsonRequest] with a given [number].
 */
fun createMockAttachmentJsonRequest(
    number: Int,
    fileName: String? = "mockFileName-$number",
    key: String? = "mockKey-$number",
    fileSize: String? = "1",
): AttachmentJsonRequest =
    AttachmentJsonRequest(
        fileName = fileName,
        key = key,
        fileSize = fileSize,
    )
