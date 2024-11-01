package com.x8bit.bitwarden.ui.platform.feature.search.util

import com.bitwarden.send.SendType
import com.bitwarden.vault.CipherType
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
    isTotp: Boolean = false,
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
                    fallbackIconRes = R.drawable.ic_globe,
                ),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = "mockUsername-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = "mockPassword-$number",
                        requiresPasswordReprompt = true,
                        cipherId = "mockId-$number",
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
                isTotp = isTotp,
            )
        }

        CipherType.SECURE_NOTE -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = null,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_note),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        notes = "mockNotes-$number",
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                isTotp = false,
            )
        }

        CipherType.CARD -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = "mockBrand-$number, *er-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_payment_card),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = "mockNumber-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = "mockCode-$number",
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                isTotp = false,
            )
        }

        CipherType.IDENTITY -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = "mockFirstName-${number}mockLastName-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_id_card),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconRes(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                isTotp = false,
            )
        }

        CipherType.SSH_KEY -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = "mockPublicKey-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_ssh_key),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(cipherId = "mockId-$number"),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                isTotp = false,
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
                iconData = IconData.Local(R.drawable.ic_file),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_key,
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
                isTotp = false,
            )
        }

        SendType.TEXT -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "SendNameLabel",
                subtitle = "Oct 27, 2023, 12:00 PM",
                subtitleTestTag = "SendDateLabel",
                iconData = IconData.Local(R.drawable.ic_file_text),
                extraIconList = listOf(
                    IconRes(
                        iconRes = R.drawable.ic_key,
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
                isTotp = true,
            )
        }
    }
