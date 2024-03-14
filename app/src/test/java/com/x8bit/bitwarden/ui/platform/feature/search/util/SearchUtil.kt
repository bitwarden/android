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
                titleTestTag = "CipherNameLabel",
                subtitle = "mockUsername-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Network(
                    uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                    fallbackIconRes = R.drawable.ic_login_item,
                ),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = "mockUsername-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = "mockPassword-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(
                        totpCode = "mockTotp-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.LaunchClick(
                        url = "www.mockuri$number.com",
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = "mockTotp-$number",
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
            )
        }

        CipherType.SECURE_NOTE -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = null,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_secure_note_item),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        notes = "mockNotes-$number",
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
            )
        }

        CipherType.CARD -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = "mockBrand-$number, *er-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_card_item),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
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
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
            )
        }

        CipherType.IDENTITY -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = "mockFirstName-${number}mockLastName-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_identity_item),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collection,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_attachment,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(cipherId = "mockId-$number"),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
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
                titleTestTag = "SendNameLabel",
                subtitle = "Oct 27, 2023, 12:00 PM",
                subtitleTestTag = "SendDateLabel",
                iconData = IconData.Local(R.drawable.ic_send_file),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_send_password,
                        contentDescription = R.string.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
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
                overflowTestTag = "SendOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
            )
        }

        SendType.TEXT -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "SendNameLabel",
                subtitle = "Oct 27, 2023, 12:00 PM",
                subtitleTestTag = "SendDateLabel",
                iconData = IconData.Local(R.drawable.ic_send_text),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_send_password,
                        contentDescription = R.string.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
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
                overflowTestTag = "SendOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
            )
        }
    }
