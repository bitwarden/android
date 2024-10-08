package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.TotpData

/**
 * Returns pre-filled content that may be used for an "add" type
 * [VaultAddEditState.ViewState.Content] during a TOTP creation event.
 */
fun TotpData.toDefaultAddTypeContent(
    isIndividualVaultDisabled: Boolean,
): VaultAddEditState.ViewState.Content = VaultAddEditState.ViewState.Content(
    common = VaultAddEditState.ViewState.Content.Common(
        name = (this.issuer ?: this.accountName).orEmpty(),
    ),
    isIndividualVaultDisabled = isIndividualVaultDisabled,
    type = VaultAddEditState.ViewState.Content.ItemType.Login(totp = this.uri),
)
