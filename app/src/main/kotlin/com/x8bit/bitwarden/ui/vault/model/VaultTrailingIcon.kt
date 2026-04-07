package com.x8bit.bitwarden.ui.vault.model

import androidx.annotation.DrawableRes
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText

/**
 * Represents the icons displayed after the cipher name.
 */
enum class VaultTrailingIcon(
    @field:DrawableRes val iconRes: Int,
    val contentDescription: Text,
    val testTag: String,
) {
    COLLECTION(
        iconRes = BitwardenDrawable.ic_collections,
        contentDescription = BitwardenString.collections.asText(),
        testTag = "CipherInCollectionIcon",
    ),
    ATTACHMENT(
        iconRes = BitwardenDrawable.ic_paperclip,
        contentDescription = BitwardenString.attachments.asText(),
        testTag = "CipherWithAttachmentsIcon",
    ),
}
