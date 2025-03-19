package com.x8bit.bitwarden.ui.vault.feature.item.model

import com.bitwarden.vault.CipherView
import kotlinx.collections.immutable.ImmutableList

/**
 * The state containing totp code item information and the cipher for the item.
 *
 * @property cipher The cipher view for the item.
 * @property totpCodeItemData The data for the totp code.
 * @property canDelete Whether the item can be deleted.
 * @property canAssociateToCollections Whether the item can be associated to a collection.
 * @property canEdit Whether the item can be edited.
 * @property relatedLocations The locations the item is assigned to.
 */
data class VaultItemStateData(
    val cipher: CipherView?,
    val totpCodeItemData: TotpCodeItemData?,
    val canDelete: Boolean,
    val canAssociateToCollections: Boolean,
    val canEdit: Boolean,
    val relatedLocations: ImmutableList<VaultItemLocation>,
)
