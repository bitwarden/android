package com.x8bit.bitwarden.ui.platform.feature.search.util

import androidx.annotation.DrawableRes
import com.bitwarden.send.SendType
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.ui.platform.feature.search.SearchState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.persistentListOf

/**
 * Create a mock [SearchState.DisplayItem] with a given [number].
 */
fun createMockDisplayItemForCipher(
    number: Int,
    cipherType: CipherType = CipherType.LOGIN,
    @DrawableRes fallbackIconRes: Int = BitwardenDrawable.ic_globe,
): SearchState.DisplayItem =
    when (cipherType) {
        CipherType.LOGIN -> {
            SearchState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                subtitle = "mockSubtitle-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Network(
                    uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                    fallbackIconRes = fallbackIconRes,
                ),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_collections,
                        contentDescription = BitwardenString.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_paperclip,
                        contentDescription = BitwardenString.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = "mockUsername-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        requiresPasswordReprompt = true,
                        cipherId = "mockId-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.ViewClick(
                        cipherId = "mockId-$number",
                        cipherType = cipherType,
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = cipherType,
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
                iconData = IconData.Local(BitwardenDrawable.ic_note),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_collections,
                        contentDescription = BitwardenString.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_paperclip,
                        contentDescription = BitwardenString.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
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
                subtitle = "mockSubtitle-$number",
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(BitwardenDrawable.ic_payment_card),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_collections,
                        contentDescription = BitwardenString.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_paperclip,
                        contentDescription = BitwardenString.attachments.asText(),
                        testTag = "CipherWithAttachmentsIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = true,
                    ),
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
                iconData = IconData.Local(BitwardenDrawable.ic_id_card),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_collections,
                        contentDescription = BitwardenString.collections.asText(),
                        testTag = "CipherInCollectionIcon",
                    ),
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_paperclip,
                        contentDescription = BitwardenString.attachments.asText(),
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
                iconData = IconData.Local(BitwardenDrawable.ic_ssh_key),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_collections,
                        contentDescription = BitwardenString.collections.asText(),
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
                iconData = IconData.Local(BitwardenDrawable.ic_file),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_key,
                        contentDescription = BitwardenString.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_send_max_access_count_reached,
                        contentDescription = BitwardenString.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ViewClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
                    ),
                    ListingItemOverflowAction.SendAction.EditClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
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
                iconData = IconData.Local(BitwardenDrawable.ic_file_text),
                extraIconList = persistentListOf(
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_key,
                        contentDescription = BitwardenString.password.asText(),
                        testTag = "PasswordProtectedSendIcon",
                    ),
                    IconData.Local(
                        iconRes = BitwardenDrawable.ic_send_max_access_count_reached,
                        contentDescription = BitwardenString.maximum_access_count_reached.asText(),
                        testTag = "MaxAccessSendIcon",
                    ),
                ),
                overflowOptions = listOf(
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://vault.bitwarden.com/#/send/mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ViewClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
                    ),
                    ListingItemOverflowAction.SendAction.EditClick(
                        sendId = "mockId-$number",
                        sendType = sendType,
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
