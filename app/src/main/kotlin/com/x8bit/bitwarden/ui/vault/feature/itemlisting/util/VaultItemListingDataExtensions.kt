@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.DrawableRes
import com.bitwarden.collections.CollectionView
import com.bitwarden.core.data.util.toFormattedDateTimeStyle
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.credentials.model.CreateCredentialRequest
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.util.toFailureCipherListView
import com.x8bit.bitwarden.ui.tools.feature.send.util.toLabelIcons
import com.x8bit.bitwarden.ui.tools.feature.send.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import com.x8bit.bitwarden.ui.vault.feature.util.getCollections
import com.x8bit.bitwarden.ui.vault.feature.util.getFolders
import com.x8bit.bitwarden.ui.vault.feature.util.toCollectionDisplayName
import com.x8bit.bitwarden.ui.vault.feature.util.toFolderDisplayName
import com.x8bit.bitwarden.ui.vault.feature.util.toLabelIcons
import com.x8bit.bitwarden.ui.vault.feature.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import com.x8bit.bitwarden.ui.vault.feature.vault.util.applyRestrictItemTypesPolicy
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import com.x8bit.bitwarden.ui.vault.model.TotpData
import com.x8bit.bitwarden.ui.vault.util.toSdkCipherType
import java.time.Clock
import java.time.format.FormatStyle

/**
 * Determines a predicate to filter a list of [CipherView] based on the
 * [VaultItemListingState.ItemListingType].
 */
fun CipherListView.determineListingPredicate(
    itemListingType: VaultItemListingState.ItemListingType.Vault,
): Boolean =
    when (itemListingType) {
        is VaultItemListingState.ItemListingType.Vault.Card -> {
            type is CipherListViewType.Card && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Collection -> {
            itemListingType.collectionId in this.collectionIds && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Folder -> {
            folderId == itemListingType.folderId && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Identity -> {
            type is CipherListViewType.Identity && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Login -> {
            type is CipherListViewType.Login && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.SecureNote -> {
            type is CipherListViewType.SecureNote && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.SshKey -> {
            type is CipherListViewType.SshKey && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Trash -> {
            deletedDate != null
        }
    }

/**
 * Determines a predicate to filter a list of [CipherView] based on the
 * [VaultItemListingState.ItemListingType].
 */
fun SendView.determineListingPredicate(
    itemListingType: VaultItemListingState.ItemListingType.Send,
): Boolean =
    when (itemListingType) {
        is VaultItemListingState.ItemListingType.Send.SendFile -> {
            type == SendType.FILE
        }

        is VaultItemListingState.ItemListingType.Send.SendText -> {
            type == SendType.TEXT
        }
    }

/**
 * Transforms a list of [CipherListView] into [VaultItemListingState.ViewState].
 */
@Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList")
fun VaultData.toViewState(
    itemListingType: VaultItemListingState.ItemListingType.Vault,
    vaultFilterType: VaultFilterType,
    hasMasterPassword: Boolean,
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    autofillSelectionData: AutofillSelectionData?,
    createCredentialRequestData: CreateCredentialRequest?,
    totpData: TotpData?,
    isPremiumUser: Boolean,
    restrictItemTypesPolicyOrgIds: List<String>,
): VaultItemListingState.ViewState {
    val filteredCipherViewList = decryptCipherListResult
        .successes
        .applyFilters(
            itemListingType = itemListingType,
            vaultFilterType = vaultFilterType,
            restrictItemTypesPolicyOrgIds = restrictItemTypesPolicyOrgIds,
        )

    val filteredFailuresCipherViewList = decryptCipherListResult
        .failures
        .map { cipher ->
            cipher.toFailureCipherListView()
        }
        .applyFilters(
            itemListingType = itemListingType,
            vaultFilterType = vaultFilterType,
            restrictItemTypesPolicyOrgIds = restrictItemTypesPolicyOrgIds,
        )

    val allFilteredCipherViewList = filteredFailuresCipherViewList
        .plus(filteredCipherViewList)

    val folderList =
        (itemListingType as? VaultItemListingState.ItemListingType.Vault.Folder)
            ?.folderId
            ?.let { folderViewList.getFolders(it) }
            .orEmpty()

    val collectionList =
        (itemListingType as? VaultItemListingState.ItemListingType.Vault.Collection)
            ?.let { collectionViewList.getCollections(it.collectionId) }
            .orEmpty()

    return if (folderList.isNotEmpty() || allFilteredCipherViewList.isNotEmpty() ||
        collectionList.isNotEmpty()
    ) {
        VaultItemListingState.ViewState.Content(
            displayItemList = filteredFailuresCipherViewList
                .toDisplayItemListDecryptionError()
                .plus(
                    filteredCipherViewList.toDisplayItemList(
                        baseIconUrl = baseIconUrl,
                        hasMasterPassword = hasMasterPassword,
                        isIconLoadingDisabled = isIconLoadingDisabled,
                        isAutofill = autofillSelectionData != null,
                        isFido2Creation = createCredentialRequestData != null,
                        isPremiumUser = isPremiumUser,
                    ),
                ),
            displayFolderList = folderList.map { folderView ->
                VaultItemListingState.FolderDisplayItem(
                    id = requireNotNull(folderView.id),
                    name = folderView.name,
                    count = allFilteredCipherViewList
                        .count {
                            it.deletedDate == null &&
                                !it.id.isNullOrBlank() &&
                                folderView.id == it.folderId
                        },
                )
            },
            displayCollectionList = collectionList.map { collectionView ->
                VaultItemListingState.CollectionDisplayItem(
                    id = requireNotNull(collectionView.id),
                    name = collectionView.name,
                    count = allFilteredCipherViewList
                        .count {
                            !it.id.isNullOrBlank() &&
                                it.deletedDate == null &&
                                collectionView.id in it.collectionIds
                        },
                )
            },
        )
    } else {
        // Use the autofill empty message if necessary, otherwise use normal type-specific message
        val message = autofillSelectionData
            ?.uri
            ?.toHostOrPathOrNull()
            ?.let { BitwardenString.no_items_for_uri.asText(it) }
            ?: createCredentialRequestData
                ?.relyingPartyIdOrNull
                ?.toHostOrPathOrNull()
                ?.let { BitwardenString.no_items_for_uri.asText(it) }
            ?: totpData?.let { BitwardenString.search_for_a_login_or_add_a_new_login.asText() }
            ?: run {
                when (itemListingType) {
                    is VaultItemListingState.ItemListingType.Vault.Folder -> {
                        BitwardenString.no_items_folder
                    }

                    is VaultItemListingState.ItemListingType.Vault.Collection -> {
                        BitwardenString.no_items_collection
                    }

                    VaultItemListingState.ItemListingType.Vault.Trash -> {
                        BitwardenString.no_items_trash
                    }

                    VaultItemListingState.ItemListingType.Vault.Card -> {
                        BitwardenString.no_cards
                    }

                    VaultItemListingState.ItemListingType.Vault.Identity -> {
                        BitwardenString.no_identities
                    }

                    VaultItemListingState.ItemListingType.Vault.Login -> {
                        BitwardenString.no_logins
                    }

                    VaultItemListingState.ItemListingType.Vault.SecureNote -> {
                        BitwardenString.no_notes
                    }

                    VaultItemListingState.ItemListingType.Vault.SshKey -> {
                        BitwardenString.no_ssh_keys
                    }
                }
                    .asText()
            }

        val restrictItemTypePolicyEnabled = restrictItemTypesPolicyOrgIds.isNotEmpty() &&
            itemListingType == VaultItemListingState.ItemListingType.Vault.Card

        val shouldShowAddButton = !restrictItemTypePolicyEnabled && itemListingType.hasFab

        VaultItemListingState.ViewState.NoItems(
            header = totpData
                ?.let {
                    BitwardenString.no_items_for_vault
                        .asText(it.issuer ?: it.accountName ?: "--")
                },
            message = message,
            shouldShowAddButton = shouldShowAddButton,
            buttonText = createCredentialRequestData
                ?.let { BitwardenString.save_passkey_as_new_login.asText() }
                ?: run {
                    when (itemListingType) {
                        VaultItemListingState.ItemListingType.Vault.Card -> {
                            BitwardenString.new_card
                        }

                        VaultItemListingState.ItemListingType.Vault.Identity -> {
                            BitwardenString.new_identity
                        }

                        VaultItemListingState.ItemListingType.Vault.Login -> {
                            BitwardenString.new_login
                        }

                        VaultItemListingState.ItemListingType.Vault.SecureNote -> {
                            BitwardenString.new_note
                        }

                        VaultItemListingState.ItemListingType.Vault.SshKey -> {
                            BitwardenString.new_ssh_key
                        }

                        else -> BitwardenString.new_item
                    }
                        .asText()
                },
            vectorRes = totpData
                ?.let { BitwardenDrawable.ill_folder_question },
        )
    }
}

/**
 * Transforms a list of [CipherView] into [VaultItemListingState.ViewState].
 */
fun List<SendView>.toViewState(
    itemListingType: VaultItemListingState.ItemListingType.Send,
    baseWebSendUrl: String,
    clock: Clock,
): VaultItemListingState.ViewState =
    if (isNotEmpty()) {
        VaultItemListingState.ViewState.Content(
            displayItemList = toDisplayItemList(
                baseWebSendUrl = baseWebSendUrl,
                clock = clock,
            ),
            displayFolderList = emptyList(),
            displayCollectionList = emptyList(),
        )
    } else {
        VaultItemListingState.ViewState.NoItems(
            message = when (itemListingType) {
                VaultItemListingState.ItemListingType.Send.SendFile -> BitwardenString.no_file_sends
                VaultItemListingState.ItemListingType.Send.SendText -> BitwardenString.no_text_sends
            }
                .asText(),
            shouldShowAddButton = true,
            buttonText = when (itemListingType) {
                VaultItemListingState.ItemListingType.Send.SendFile -> BitwardenString.new_file_send
                VaultItemListingState.ItemListingType.Send.SendText -> BitwardenString.new_text_send
            }
                .asText(),
        )
    }

/**
 * Updates a [VaultItemListingState.ItemListingType] with the given data if necessary.
 */
fun VaultItemListingState.ItemListingType.updateWithAdditionalDataIfNecessary(
    folderList: List<FolderView>,
    collectionList: List<CollectionView>,
): VaultItemListingState.ItemListingType =
    when (this) {
        is VaultItemListingState.ItemListingType.Vault.Card -> this
        is VaultItemListingState.ItemListingType.Vault.Collection -> copy(
            collectionName = collectionList
                .find { it.id == collectionId }
                ?.name
                ?.toCollectionDisplayName(collectionList)
                .orEmpty(),
        )

        is VaultItemListingState.ItemListingType.Vault.Folder -> {
            val fullyQualifiedName = folderList
                .find { it.id == folderId }
                ?.name
            copy(
                folderName = fullyQualifiedName
                    ?.toFolderDisplayName()
                    .orEmpty(),
                fullyQualifiedName = fullyQualifiedName.orEmpty(),
            )
        }

        is VaultItemListingState.ItemListingType.Vault.Identity -> this
        is VaultItemListingState.ItemListingType.Vault.Login -> this
        is VaultItemListingState.ItemListingType.Vault.SecureNote -> this
        is VaultItemListingState.ItemListingType.Vault.Trash -> this
        is VaultItemListingState.ItemListingType.Send.SendFile -> this
        is VaultItemListingState.ItemListingType.Send.SendText -> this
        is VaultItemListingState.ItemListingType.Vault.SshKey -> this
    }

@Suppress("LongParameterList")
private fun List<CipherListView>.toDisplayItemList(
    baseIconUrl: String,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
    isFido2Creation: Boolean,
    isPremiumUser: Boolean,
): List<VaultItemListingState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseIconUrl = baseIconUrl,
            hasMasterPassword = hasMasterPassword,
            isIconLoadingDisabled = isIconLoadingDisabled,
            isAutofill = isAutofill,
            isFido2Creation = isFido2Creation,
            isPremiumUser = isPremiumUser,
        )
    }

private fun List<SendView>.toDisplayItemList(
    baseWebSendUrl: String,
    clock: Clock,
): List<VaultItemListingState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseWebSendUrl = baseWebSendUrl,
            clock = clock,
        )
    }

@Suppress("LongParameterList")
private fun CipherListView.toDisplayItem(
    baseIconUrl: String,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
    isFido2Creation: Boolean,
    isPremiumUser: Boolean,
): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = name.asText(),
        titleTestTag = "CipherNameLabel",
        secondSubtitle = this.toSecondSubtitle(
            fido2CredentialRpId = login?.fido2Credentials?.firstOrNull()?.rpId,
        ),
        secondSubtitleTestTag = "PasskeySite",
        subtitle = this.subtitle,
        subtitleTestTag = this.toSubtitleTestTag(
            isAutofill = isAutofill,
            isFido2Creation = isFido2Creation,
        ),
        iconData = this.toIconData(
            baseIconUrl = baseIconUrl,
            isIconLoadingDisabled = isIconLoadingDisabled,
            usePasskeyDefaultIcon = (isAutofill || isFido2Creation) &&
                this.isActiveWithFido2Credentials,
        ),
        iconTestTag = this.toIconTestTag(),
        extraIconList = this.toLabelIcons(),
        overflowOptions = this.toOverflowActions(
            hasMasterPassword = hasMasterPassword,
            isPremiumUser = isPremiumUser,
        ),
        optionsTestTag = "CipherOptionsButton",
        isAutofill = isAutofill,
        isCredentialCreation = isFido2Creation,
        shouldShowMasterPasswordReprompt = (reprompt == CipherRepromptType.PASSWORD) &&
            hasMasterPassword,
        itemType = VaultItemListingState
            .DisplayItem
            .ItemType
            .Vault(
                type = this.type.toSdkCipherType(),
            ),
    )

@Suppress("MaxLineLength")
private fun List<CipherListView>.toDisplayItemListDecryptionError(): List<VaultItemListingState.DisplayItem> =
    this.map {
        it.toDisplayItemDecryptionError()
    }

private fun CipherListView.toDisplayItemDecryptionError(): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = BitwardenString.error_cannot_decrypt.asText(),
        titleTestTag = "CipherNameLabel",
        secondSubtitle = null,
        secondSubtitleTestTag = null,
        subtitle = null,
        subtitleTestTag = "",
        iconData = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
        iconTestTag = this.toIconTestTag(),
        extraIconList = this.toLabelIcons(),
        overflowOptions = emptyList(),
        optionsTestTag = "CipherOptionsButton",
        isAutofill = false,
        isCredentialCreation = false,
        shouldShowMasterPasswordReprompt = false,
        itemType = VaultItemListingState.DisplayItem.ItemType.DecryptionError,
    )

private fun CipherListView.toSecondSubtitle(fido2CredentialRpId: String?): String? =
    fido2CredentialRpId
        ?.takeIf { this.type is CipherListViewType.Login && it.isNotEmpty() && it != this.name }

private fun CipherListView.toSubtitleTestTag(
    isAutofill: Boolean,
    isFido2Creation: Boolean,
): String =
    if ((isAutofill || isFido2Creation)) {
        if (this.isActiveWithFido2Credentials) "PasskeyName" else "PasswordName"
    } else {
        "CipherSubTitleLabel"
    }

private fun CipherListView.toIconTestTag(): String =
    when (type) {
        is CipherListViewType.Login -> "LoginCipherIcon"
        CipherListViewType.SecureNote -> "SecureNoteCipherIcon"
        is CipherListViewType.Card -> "CardCipherIcon"
        CipherListViewType.Identity -> "IdentityCipherIcon"
        CipherListViewType.SshKey -> "SshKeyCipherIcon"
    }

private fun CipherListView.toIconData(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    usePasskeyDefaultIcon: Boolean,
): IconData {
    return when (this.type) {
        is CipherListViewType.Login -> {
            login?.uris.toLoginIconData(
                baseIconUrl = baseIconUrl,
                isIconLoadingDisabled = isIconLoadingDisabled,
                usePasskeyDefaultIcon = usePasskeyDefaultIcon,
            )
        }

        else -> {
            IconData.Local(iconRes = this.type.iconRes)
        }
    }
}

private fun SendView.toDisplayItem(
    baseWebSendUrl: String,
    clock: Clock,
): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = name.asText(),
        titleTestTag = "SendNameLabel",
        secondSubtitle = null,
        secondSubtitleTestTag = null,
        subtitle = deletionDate.toFormattedDateTimeStyle(
            dateStyle = FormatStyle.MEDIUM,
            timeStyle = FormatStyle.SHORT,
            clock = clock,
        ),
        subtitleTestTag = "SendDateLabel",
        iconData = IconData.Local(
            iconRes = when (type) {
                SendType.TEXT -> BitwardenDrawable.ic_file_text
                SendType.FILE -> BitwardenDrawable.ic_file
            },
        ),
        iconTestTag = null,
        extraIconList = toLabelIcons(clock = clock),
        overflowOptions = toOverflowActions(baseWebSendUrl = baseWebSendUrl),
        optionsTestTag = "SendOptionsButton",
        isAutofill = false,
        shouldShowMasterPasswordReprompt = false,
        isCredentialCreation = false,
        itemType = VaultItemListingState.DisplayItem.ItemType.Sends(type = this.type),
    )

@get:DrawableRes
private val CipherListViewType.iconRes: Int
    get() = when (this) {
        is CipherListViewType.Login -> BitwardenDrawable.ic_globe
        CipherListViewType.SecureNote -> BitwardenDrawable.ic_note
        is CipherListViewType.Card -> BitwardenDrawable.ic_payment_card
        CipherListViewType.Identity -> BitwardenDrawable.ic_id_card
        CipherListViewType.SshKey -> BitwardenDrawable.ic_ssh_key
    }

private fun List<CipherListView>.applyFilters(
    itemListingType: VaultItemListingState.ItemListingType.Vault,
    vaultFilterType: VaultFilterType,
    restrictItemTypesPolicyOrgIds: List<String>,
): List<CipherListView> = this
    .filter { it.determineListingPredicate(itemListingType) }
    .applyRestrictItemTypesPolicy(restrictItemTypesPolicyOrgIds)
    .toFilteredList(vaultFilterType)
