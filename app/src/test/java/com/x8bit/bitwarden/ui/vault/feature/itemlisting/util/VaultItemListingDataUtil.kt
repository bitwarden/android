package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.bitwarden.core.CipherType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState

/**
 * Create a mock [VaultItemListingState.DisplayItem] with a given [number].
 */
fun createMockItemListingDisplayItem(
    number: Int,
    cipherType: CipherType = CipherType.LOGIN,
): VaultItemListingState.DisplayItem =
    when (cipherType) {
        CipherType.LOGIN -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "mockUsername-$number",
                iconData = IconData.Network(
                    "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                    fallbackIconRes = R.drawable.ic_login_item,
                ),
            )
        }

        CipherType.SECURE_NOTE -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = null,
                iconData = IconData.Local(R.drawable.ic_secure_note_item),
            )
        }

        CipherType.CARD -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "er-$number",
                iconData = IconData.Local(R.drawable.ic_card_item),
            )
        }

        CipherType.IDENTITY -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "mockFirstName-${number}mockLastName-$number",
                iconData = IconData.Local(R.drawable.ic_identity_item),
            )
        }
    }
