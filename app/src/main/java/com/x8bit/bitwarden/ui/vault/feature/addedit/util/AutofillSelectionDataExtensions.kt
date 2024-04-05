package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import java.util.UUID

/**
 * Returns pre-filled content that may be used for an "add" type
 * [VaultAddEditState.ViewState.Content].
 */
fun AutofillSelectionData.toDefaultAddTypeContent(
    isIndividualVaultDisabled: Boolean,
): VaultAddEditState.ViewState.Content {
    val uri = this.uri
    val simpleUri = uri?.toHostOrPathOrNull()
    val defaultAddType = when (this.type) {
        AutofillSelectionData.Type.CARD -> {
            VaultAddEditState.ViewState.Content.ItemType.Card()
        }

        AutofillSelectionData.Type.LOGIN -> {
            VaultAddEditState.ViewState.Content.ItemType.Login(
                uriList = listOf(
                    UriItem(
                        id = UUID.randomUUID().toString(),
                        uri = uri,
                        match = null,
                        checksum = null,
                    ),
                ),
            )
        }
    }
    return VaultAddEditState.ViewState.Content(
        common = VaultAddEditState.ViewState.Content.Common(
            name = simpleUri.orEmpty(),
        ),
        isIndividualVaultDisabled = isIndividualVaultDisabled,
        type = defaultAddType,
    )
}
