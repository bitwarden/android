package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.model.VaultTrailingIcon

/**
 * Creates the list of overflow actions to be displayed for a [CipherView].
 */
fun CipherView.toOverflowActions(
    hasMasterPassword: Boolean,
    isPremiumUser: Boolean,
): List<ListingItemOverflowAction.VaultAction> =
    this
        .id
        ?.let { cipherId ->
            listOfNotNull(
                ListingItemOverflowAction.VaultAction.ViewClick(cipherId = cipherId),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = cipherId,
                    requiresPasswordReprompt = hasMasterPassword,
                )
                    .takeUnless { this.deletedDate != null },
                this.login?.username?.let {
                    ListingItemOverflowAction.VaultAction.CopyUsernameClick(username = it)
                },
                this.login?.password
                    ?.let {
                        ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                            cipherId = cipherId,
                            password = it,
                            requiresPasswordReprompt = hasMasterPassword,
                        )
                    }
                    .takeIf { this.viewPassword },
                this.login?.totp
                    ?.let { ListingItemOverflowAction.VaultAction.CopyTotpClick(totpCode = it) }
                    .takeIf {
                        this.type == CipherType.LOGIN &&
                            (this.organizationUseTotp || isPremiumUser)
                    },
                this.card?.number?.let {
                    ListingItemOverflowAction.VaultAction.CopyNumberClick(
                        number = it,
                        requiresPasswordReprompt = hasMasterPassword,
                    )
                },
                this.card?.code?.let {
                    ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                        securityCode = it,
                        cipherId = cipherId,
                        requiresPasswordReprompt = hasMasterPassword,
                    )
                },
                this.notes
                    ?.let { ListingItemOverflowAction.VaultAction.CopyNoteClick(notes = it) }
                    .takeIf { this.type == CipherType.SECURE_NOTE },
                this.login?.uris?.firstOrNull { it.uri != null }?.uri?.let {
                    ListingItemOverflowAction.VaultAction.LaunchClick(url = it)
                },
            )
        }
        .orEmpty()

/**
 * Checks if the list is empty and if not returns an icon in a list.
 */
fun CipherView.toLabelIcons(): List<IconRes> {
    return listOfNotNull(
        VaultTrailingIcon.COLLECTION.takeIf {
            this.collectionIds.isNotEmpty() || this.organizationId?.isNotEmpty() == true
        },
        VaultTrailingIcon.ATTACHMENT.takeIf { this.attachments?.isNotEmpty() == true },
    )
        .map {
            IconRes(
                iconRes = it.iconRes,
                contentDescription = it.contentDescription,
                testTag = it.testTag,
            )
        }
}
