package com.bitwarden.network.model

/**
 * Creates a mock [AttachmentInfo] with the given [number].
 */
fun createMockAttachmentInfo(number: Int = 1): AttachmentInfo = AttachmentInfo(
    id = "mockId-$number",
    key = "mockKey-$number",
    fileName = "mockFileName-$number",
)
