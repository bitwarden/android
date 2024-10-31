package com.x8bit.bitwarden.ui.vault.feature.item.model

import com.bitwarden.vault.CipherView

/**
 * The state containing totp code item information and the cipher for the item.
 *
 * @property cipher The cipher view for the item.
 * @property totpCodeItemData The data for the totp code.
 */
data class VaultItemStateData(
    val cipher: CipherView?,
    val totpCodeItemData: TotpCodeItemData?,
    val canDelete: Boolean,
)
