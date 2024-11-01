package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import com.bitwarden.send.SendType
import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction

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
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "LoginCipherIcon",
                isTotp = isTotp,
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
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                    ListingItemOverflowAction.VaultAction.CopyNoteClick(
                        notes = "mockNotes-$number",
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "SecureNoteCipherIcon",
                isTotp = false,
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
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "CardCipherIcon",
                isTotp = false,
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
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "IdentityCipherIcon",
                isTotp = false,
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
                        requiresPasswordReprompt = requiresPasswordReprompt,
                    ),
                ),
                optionsTestTag = "CipherOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = "SshKeyCipherIcon",
                isTotp = false,
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
                optionsTestTag = "SendOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = null,
                isTotp = false,
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
                optionsTestTag = "SendOptionsButton",
                isAutofill = false,
                isFido2Creation = false,
                shouldShowMasterPasswordReprompt = false,
                iconTestTag = null,
                isTotp = false,
            )
        }
    }
