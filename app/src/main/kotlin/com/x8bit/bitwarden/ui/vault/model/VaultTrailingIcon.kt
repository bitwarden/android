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
        iconRes = BitwardenDrawable.ic_shared_folder,
        contentDescription = BitwardenString.shared_folders.asText(),
        testTag = "CipherInCollectionIcon",
    ),

    /**
     * Pre-VFO-1 variant of [COLLECTION], selected when the `vfo1-foundation` feature flag is
     * disabled. This entry should be removed, along with all of its call sites, once the flag
     * is fully rolled out.
     */
    COLLECTION_LEGACY(
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
