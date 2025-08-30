package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CopyableCipherFields
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.model.VaultTrailingIcon
import com.x8bit.bitwarden.ui.vault.util.toSdkCipherType
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Creates the list of overflow actions to be displayed for a [CipherView].
 */
@Suppress("LongMethod")
fun CipherListView.toOverflowActions(
    hasMasterPassword: Boolean,
    isPremiumUser: Boolean,
): List<ListingItemOverflowAction.VaultAction> =
    this
        .id
        ?.let { cipherId ->
            listOfNotNull(
                this.login?.username
                    ?.let { ListingItemOverflowAction.VaultAction.CopyUsernameClick(username = it) }
                    .takeIf { this.copyableFields.contains(CopyableCipherFields.LOGIN_USERNAME) },
                ListingItemOverflowAction.VaultAction
                    .CopyPasswordClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = hasMasterPassword,
                    )
                    .takeIf {
                        this.viewPassword &&
                            this.copyableFields.contains(CopyableCipherFields.LOGIN_PASSWORD)
                    },
                this.login?.totp
                    ?.let {
                        ListingItemOverflowAction.VaultAction.CopyTotpClick(
                            cipherId = cipherId,
                            requiresPasswordReprompt = hasMasterPassword,
                        )
                    }
                    .takeIf {
                        this.type is CipherListViewType.Login &&
                            (this.organizationUseTotp || isPremiumUser) &&
                            this.copyableFields.contains(CopyableCipherFields.LOGIN_TOTP)
                    },
                ListingItemOverflowAction.VaultAction
                    .CopyNumberClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = hasMasterPassword,
                    )
                    .takeIf { this.copyableFields.contains(CopyableCipherFields.CARD_NUMBER) },
                ListingItemOverflowAction.VaultAction
                    .CopySecurityCodeClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = hasMasterPassword,
                    )
                    .takeIf {
                        this.type is CipherListViewType.Card &&
                            this.copyableFields.contains(CopyableCipherFields.CARD_SECURITY_CODE)
                    },
                ListingItemOverflowAction.VaultAction
                    .CopyNoteClick(
                        cipherId = cipherId,
                        requiresPasswordReprompt = hasMasterPassword,
                    )
                    .takeIf {
                        this.type is CipherListViewType.SecureNote &&
                            this.copyableFields.contains(CopyableCipherFields.SECURE_NOTES)
                    },
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = cipherId,
                    cipherType = this.type.toSdkCipherType(),
                    requiresPasswordReprompt = hasMasterPassword,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = cipherId,
                    cipherType = this.type.toSdkCipherType(),
                    requiresPasswordReprompt = hasMasterPassword,
                )
                    .takeUnless { this.deletedDate != null || !this.edit },
                this.login?.uris?.firstOrNull { it.uri != null }?.uri?.let {
                    ListingItemOverflowAction.VaultAction.LaunchClick(url = it)
                },
            )
        }
        .orEmpty()

/**
 * Checks if the list is empty and if not returns an icon in a list.
 */
fun CipherListView.toLabelIcons(): ImmutableList<IconData> {
    return listOfNotNull(
        VaultTrailingIcon.COLLECTION.takeIf {
            this.collectionIds.isNotEmpty() || this.organizationId?.isNotEmpty() == true
        },
        VaultTrailingIcon.ATTACHMENT.takeIf { this.attachments > 0U },
    )
        .map {
            IconData.Local(
                iconRes = it.iconRes,
                contentDescription = it.contentDescription,
                testTag = it.testTag,
            )
        }
        .toImmutableList()
}
