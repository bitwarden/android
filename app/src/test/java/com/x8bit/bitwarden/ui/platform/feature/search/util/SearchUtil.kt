package com.x8bit.bitwarden.ui.platform.feature.search.util

import com.bitwarden.core.CipherType
import com.bitwarden.core.SendType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.platform.feature.search.SearchState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction

/**
 * Create a mock [SearchState.DisplayItem] with a given [number].
 */
fun createMockDisplayItemForCipher(
    number: Int,
    cipherType: CipherType = CipherType.LOGIN,
): SearchState.DisplayItem =
    when (cipherType) {
        CipherType.LOGIN -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "mockUsername-$number",
                iconData = IconData.Network(
                    uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                    fallbackIconRes = R.drawable.ic_login_item,
                ),
                extraIconList = emptyList(),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = "mockUsername-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = "mockPassword-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.LaunchClick(
                        url = "www.mockuri$number.com",
                    ),
                ),
            )
        }

        CipherType.SECURE_NOTE -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = null,
                iconData = IconData.Local(R.drawable.ic_secure_note_item),
                extraIconList = emptyList(),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        notes = "mockNotes-$number",
                    ),
                ),
            )
        }

        CipherType.CARD -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "er-$number",
                iconData = IconData.Local(R.drawable.ic_card_item),
                extraIconList = emptyList(),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = "mockNumber-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = "mockCode-$number",
                    ),
                ),
            )
        }

        CipherType.IDENTITY -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "mockFirstName-${number}mockLastName-$number",
                iconData = IconData.Local(R.drawable.ic_identity_item),
                extraIconList = emptyList(),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(cipherId = "mockId-$number"),
                ),
            )
        }
    }

/**
 * Create a mock [SearchState.DisplayItem] with a given [number].
 */
@Suppress("MaxLineLength")
fun createMockDisplayItemForSend(
    number: Int,
    sendType: SendType = SendType.FILE,
): SearchState.DisplayItem =
    when (sendType) {
        SendType.FILE -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "Oct 27, 2023, 12:00 PM",
                iconData = IconData.Local(R.drawable.ic_send_file),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_send_password,
                        contentDescription = R.string.password.asText(),
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.EditClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-$number"),
                ),
            )
        }

        SendType.TEXT -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                subtitle = "Oct 27, 2023, 12:00 PM",
                iconData = IconData.Local(R.drawable.ic_send_text),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_send_password,
                        contentDescription = R.string.password.asText(),
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.EditClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-$number"),
                ),
            )
        }
    }
