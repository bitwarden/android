package com.x8bit.bitwarden.ui.vault.model

import androidx.annotation.DrawableRes
import com.bitwarden.ui.util.Text
import com.bitwarden.ui.util.asText
import com.x8bit.bitwarden.R

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
