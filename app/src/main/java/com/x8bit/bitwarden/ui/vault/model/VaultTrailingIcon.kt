package com.x8bit.bitwarden.ui.vault.model

import androidx.annotation.DrawableRes
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.Text
import com.x8bit.bitwarden.ui.platform.base.util.asText

/**
 * Represents the icons displayed after the cipher name.
 */
enum class VaultTrailingIcon(
    @DrawableRes val iconRes: Int,
    val contentDescription: Text,
    val testTag: String,
) {
    COLLECTION(
        iconRes = R.drawable.ic_collections,
        contentDescription = R.string.collections.asText(),
        testTag = "CipherInCollectionIcon",
    ),
    ATTACHMENT(
        iconRes = R.drawable.ic_paperclip,
        contentDescription = R.string.attachments.asText(),
        testTag = "CipherWithAttachmentsIcon",
    ),
}
