package com.x8bit.bitwarden.data.vault.datasource.network.model

/**
 * Create a mock [CipherJsonRequest] with a given [number].
 */
fun createMockAttachmentJsonRequest(number: Int): AttachmentJsonRequest =
    AttachmentJsonRequest(
        fileName = "mockFileName-$number",
        key = "mockKey-$number",
        fileSize = "1",
    )
