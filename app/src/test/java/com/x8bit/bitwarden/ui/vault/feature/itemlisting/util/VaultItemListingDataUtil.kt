package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.bitwarden.core.CipherType
import com.bitwarden.core.SendType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingsAction

/**
 * Create a mock [VaultItemListingState.DisplayItem] with a given [number].
 */
fun createMockDisplayItemForCipher(
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
                overflowOptions = emptyList(),
            )
        }

        CipherType.SECURE_NOTE -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = null,
                iconData = IconData.Local(R.drawable.ic_secure_note_item),
                overflowOptions = emptyList(),
            )
        }

        CipherType.CARD -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "er-$number",
                iconData = IconData.Local(R.drawable.ic_card_item),
                overflowOptions = emptyList(),
            )
        }

        CipherType.IDENTITY -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "mockFirstName-${number}mockLastName-$number",
                iconData = IconData.Local(R.drawable.ic_identity_item),
                overflowOptions = emptyList(),
            )
        }
    }

/**
 * Create a mock [VaultItemListingState.DisplayItem] with a given [number].
 */
@Suppress("MaxLineLength")
fun createMockDisplayItemForSend(
    number: Int,
    sendType: SendType = SendType.FILE,
): VaultItemListingState.DisplayItem =
    when (sendType) {
        SendType.FILE -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "2023-10-27T12:00:00Z",
                iconData = IconData.Local(R.drawable.ic_send_file),
                overflowOptions = listOfNotNull(
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.edit.asText(),
                        action = VaultItemListingsAction.ItemClick(id = "mockId-$number"),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.copy_link.asText(),
                        action = VaultItemListingsAction.CopySendUrlClick(
                            sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                        ),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.share_link.asText(),
                        action = VaultItemListingsAction.ShareSendUrlClick(
                            sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                        ),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.remove_password.asText(),
                        action = VaultItemListingsAction.RemoveSendPasswordClick(
                            sendId = "mockId-$number",
                        ),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.delete.asText(),
                        action = VaultItemListingsAction.DeleteSendClick(sendId = "mockId-$number"),
                    ),
                ),
            )
        }

        SendType.TEXT -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "2023-10-27T12:00:00Z",
                iconData = IconData.Local(R.drawable.ic_send_text),
                overflowOptions = listOfNotNull(
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.edit.asText(),
                        action = VaultItemListingsAction.ItemClick(id = "mockId-$number"),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.copy_link.asText(),
                        action = VaultItemListingsAction.CopySendUrlClick(
                            sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                        ),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.share_link.asText(),
                        action = VaultItemListingsAction.ShareSendUrlClick(
                            sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                        ),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.remove_password.asText(),
                        action = VaultItemListingsAction.RemoveSendPasswordClick(
                            sendId = "mockId-$number",
                        ),
                    ),
                    VaultItemListingState.DisplayItem.OverflowItem(
                        title = R.string.delete.asText(),
                        action = VaultItemListingsAction.DeleteSendClick(sendId = "mockId-$number"),
                    ),
                ),
            )
        }
    }
