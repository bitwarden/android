package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull
import java.util.UUID

/**
 * Returns pre-filled content that may be used for an "add" type
 * [VaultAddEditState.ViewState.Content].
 */
fun AutofillSaveItem.toDefaultAddTypeContent(
    isIndividualVaultDisabled: Boolean,
): VaultAddEditState.ViewState.Content =
    when (this) {
        is AutofillSaveItem.Card -> {
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(),
                isIndividualVaultDisabled = isIndividualVaultDisabled,
                type = VaultAddEditState.ViewState.Content.ItemType.Card(
                    cardHolderName = this.cardholderName.orEmpty(),
                    number = this.number.orEmpty(),
                    expirationMonth = VaultCardExpirationMonth
                        .entries
                        .find { it.number == this.expirationMonth }
                        ?: VaultCardExpirationMonth.SELECT,
                    expirationYear = this.expirationYear.orEmpty(),
                    securityCode = this.securityCode.orEmpty(),
                    brand = this.brand
                        ?.findVaultCardBrandWithNameOrNull()
                        ?: VaultCardBrand.SELECT,
                ),
            )
        }

        is AutofillSaveItem.Login -> {
            val uri = this.uri
            val simpleUri = uri?.toHostOrPathOrNull()
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    name = simpleUri.orEmpty(),
                ),
                isIndividualVaultDisabled = isIndividualVaultDisabled,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = this.username.orEmpty(),
                    password = this.password.orEmpty(),
                    uriList = listOf(
                        UriItem(
                            id = UUID.randomUUID().toString(),
                            uri = uri,
                            match = null,
                            checksum = null,
                        ),
                    ),
                ),
            )
        }
    }

/**
 * Converts an [AutofillSaveItem] to a [VaultItemCipherType].
 */
fun AutofillSaveItem.toVaultItemCipherType(): VaultItemCipherType = when (this) {
    is AutofillSaveItem.Card -> VaultItemCipherType.CARD
    is AutofillSaveItem.Login -> VaultItemCipherType.LOGIN
}
