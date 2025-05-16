package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.bitwarden.send.SendType
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import kotlinx.collections.immutable.persistentListOf

/**
 * Create a mock [VaultItemListingState.DisplayItem] with a given [number].
 */
@Suppress("LongParameterList")
fun createMockDisplayItemForCipher(
    number: Int,
    cipherType: CipherType = CipherType.LOGIN,
    subtitle: String? = "mockUsername-$number",
    secondSubtitleTestTag: String? = null,
    requiresPasswordReprompt: Boolean = true,
    isTotp: Boolean = false,
): VaultItemListingState.DisplayItem =
    when (cipherType) {
        CipherType.LOGIN -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = secondSubtitleTestTag,
                subtitle = subtitle,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Network(
                    uri = "https://vault.bitwarden.com/icons/www.mockuri.com/icon.png",
                    fallbackIconRes = R.drawable.ic_globe,
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
                        cipherType = cipherType,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = cipherType,
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(
                        username = "mockUsername-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                        password = "mockPassword-$number",
                        requiresPasswordReprompt = requiresPasswordReprompt,
                        cipherId = "mockId-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.CopyTotpClick(
                        totpCode = "mockTotp-$number",
                    ),
                    ListingItemOverflowAction.VaultAction.LaunchClick(
                        url = "www.mockuri$number.com",
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isCredentialCreation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "LoginCipherIcon",
                isTotp = isTotp,
                itemType = VaultItemListingState.DisplayItem.ItemType.Vault(type = cipherType),
            )
        }

        CipherType.SECURE_NOTE -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = secondSubtitleTestTag,
                subtitle = subtitle,
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
                        cipherType = cipherType,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = cipherType,
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        notes = "mockNotes-$number",
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isCredentialCreation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "SecureNoteCipherIcon",
                isTotp = false,
                itemType = VaultItemListingState.DisplayItem.ItemType.Vault(type = cipherType),
            )
        }

        CipherType.CARD -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = secondSubtitleTestTag,
                subtitle = subtitle,
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
                        cipherType = cipherType,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = cipherType,
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = "mockNumber-$number",
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = "mockCode-$number",
                        cipherId = "mockId-$number",
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isCredentialCreation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "CardCipherIcon",
                isTotp = false,
                itemType = VaultItemListingState.DisplayItem.ItemType.Vault(type = cipherType),
            )
        }

        CipherType.IDENTITY -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = secondSubtitleTestTag,
                subtitle = subtitle,
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
                        cipherType = cipherType,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = cipherType,
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isCredentialCreation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "IdentityCipherIcon",
                isTotp = false,
                itemType = VaultItemListingState.DisplayItem.ItemType.Vault(type = cipherType),
            )
        }

        CipherType.SSH_KEY -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "CipherNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = secondSubtitleTestTag,
                subtitle = subtitle,
                subtitleTestTag = "CipherSubTitleLabel",
                iconData = IconData.Local(R.drawable.ic_ssh_key),
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
                        cipherType = cipherType,
                    ),
                    ListingItemOverflowAction.VaultAction.EditClick(
                        cipherId = "mockId-$number",
                        cipherType = cipherType,
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isCredentialCreation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "SshKeyCipherIcon",
                isTotp = false,
                itemType = VaultItemListingState.DisplayItem.ItemType.Vault(type = cipherType),
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
                titleTestTag = "SendNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
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
                    ListingItemOverflowAction.SendAction.EditClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://send.bitwarden.com/#mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://send.bitwarden.com/#mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-$number"),
                ),
                optionsTestTag = "SendOptionsButton",
                isAutofill = false,
                isCredentialCreation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = null,
                isTotp = false,
                itemType = VaultItemListingState.DisplayItem.ItemType.Sends(type = sendType),
            )
        }

        SendType.TEXT -> {
            VaultItemListingState.DisplayItem(
                id = "mockId-$number",
                title = "mockName-$number",
                titleTestTag = "SendNameLabel",
                secondSubtitle = null,
                secondSubtitleTestTag = null,
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
                    ListingItemOverflowAction.SendAction.EditClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.CopyUrlClick(
                        sendUrl = "https://send.bitwarden.com/#mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.ShareUrlClick(
                        sendUrl = "https://send.bitwarden.com/#mockAccessId-$number/mockKey-$number",
                    ),
                    ListingItemOverflowAction.SendAction.RemovePasswordClick(sendId = "mockId-$number"),
                    ListingItemOverflowAction.SendAction.DeleteClick(sendId = "mockId-$number"),
                ),
                optionsTestTag = "SendOptionsButton",
                isAutofill = false,
                isCredentialCreation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = null,
                isTotp = false,
                itemType = VaultItemListingState.DisplayItem.ItemType.Sends(type = sendType),
            )
        }
    }
