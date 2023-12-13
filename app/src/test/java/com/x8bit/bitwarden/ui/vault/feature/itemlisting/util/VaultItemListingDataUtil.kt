package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.bitwarden.core.CipherType
import com.x8bit.bitwarden.R
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
                iconRes = R.drawable.ic_login_item,
                uri = "mockUri-$number",
            )
        }

        CipherType.SECURE_NOTE -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = null,
                iconRes = R.drawable.ic_secure_note_item,
                uri = null,
            )
        }

        CipherType.CARD -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "er-$number",
                iconRes = R.drawable.ic_card_item,
                uri = null,
            )
        }

        CipherType.IDENTITY -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "mockFirstName-${number}mockLastName-$number",
                iconRes = R.drawable.ic_identity_item,
                uri = null,
            )
        }
    }
