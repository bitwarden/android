package com.x8bit.bitwarden.ui.platform.feature.search.util

import androidx.annotation.DrawableRes
import com.bitwarden.send.SendType
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.feature.search.SearchState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.persistentListOf

/**
 * Create a mock [SearchState.DisplayItem] with a given [number].
 */
fun createMockDisplayItemForCipher(
    number: Int,
    cipherType: CipherType = CipherType.LOGIN,
    @DrawableRes fallbackIconRes: Int = R.drawable.ic_globe,
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
                    fallbackIconRes = fallbackIconRes,
                ),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.LOGIN,
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.LOGIN,
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
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.LaunchClick(
                        url = "www.mockuri$number.com",
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = "mockTotp-$number",
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                itemType = SearchState.DisplayItem.ItemType.Vault(type = cipherType),
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
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.SECURE_NOTE,
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.SECURE_NOTE,
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        notes = "mockNotes-$number",
                        requiresPasswordReprompt = true,
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                itemType = SearchState.DisplayItem.ItemType.Vault(type = cipherType),
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
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.CARD,
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.CARD,
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
                itemType = SearchState.DisplayItem.ItemType.Vault(type = cipherType),
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
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = R.drawable.ic_paperclip,
                        contentDescription = R.string.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.IDENTITY,
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.IDENTITY,
                        requiresPasswordReprompt = true,
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                itemType = SearchState.DisplayItem.ItemType.Vault(type = cipherType),
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
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = R.drawable.ic_collections,
                        contentDescription = R.string.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.SSH_KEY,
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = CipherType.SSH_KEY,
                        requiresPasswordReprompt = true,
                    ),
                ),
                overflowTestTag = "CipherOptionsButton",
                totpCode = null,
                autofillSelectionOptions = emptyList(),
                shouldDisplayMasterPasswordReprompt = false,
                itemType = SearchState.DisplayItem.ItemType.Vault(type = cipherType),
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
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = R.drawable.ic_key,
                        contentDescription = R.string.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconData.Local(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.ViewClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
                    ),
                    ListingItemOverflowAction.SendAction.EditClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
                    ),
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
                itemType = SearchState.DisplayItem.ItemType.Sends(type = sendType),
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
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = R.drawable.ic_key,
                        contentDescription = R.string.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconData.Local(
                        iconRes = R.drawable.ic_send_max_access_count_reached,
                        contentDescription = R.string.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.ViewClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
                    ),
                    ListingItemOverflowAction.SendAction.EditClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
                    ),
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
                itemType = SearchState.DisplayItem.ItemType.Sends(type = sendType),
            )
        }
    }
