package com.x8bit.bitwarden.data.vault.datasource.network.model

import com.bitwarden.core.AttachmentEncryptResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkAttachment

/**
 * Create a mock [AttachmentEncryptResult] with a given [number].
 */
fun createMockAttachmentEncryptResult(number: Int): AttachmentEncryptResult =
    AttachmentEncryptResult(
        attachment = createMockSdkAttachment(number = 1),
        contents = byteArrayOf(number.toByte()),
    )
