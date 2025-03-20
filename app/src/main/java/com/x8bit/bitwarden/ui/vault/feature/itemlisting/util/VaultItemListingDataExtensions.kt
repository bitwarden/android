@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import androidx.annotation.DrawableRes
import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.send.SendType
import com.bitwarden.send.SendView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.FolderView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.autofill.fido2.model.Fido2CreateCredentialRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSelectionData
import com.x8bit.bitwarden.data.autofill.util.isActiveWithFido2Credentials
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.base.util.toHostOrPathOrNull
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
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
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toFilteredList
import com.x8bit.bitwarden.ui.vault.feature.vault.util.toLoginIconData
import com.x8bit.bitwarden.ui.vault.model.TotpData
import java.time.Clock

private const val DELETION_DATE_PATTERN: String = "MMM d, uuuu, hh:mm a"

/**
 * Determines a predicate to filter a list of [CipherView] based on the
 * [VaultItemListingState.ItemListingType].
 */
fun CipherView.determineListingPredicate(
    itemListingType: VaultItemListingState.ItemListingType.Vault,
): Boolean =
    when (itemListingType) {
        is VaultItemListingState.ItemListingType.Vault.Card -> {
            type == CipherType.CARD &&
                deletedDate == null &&
                archivedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Collection -> {
            itemListingType.collectionId in this.collectionIds && deletedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Folder -> {
            folderId == itemListingType.folderId &&
                deletedDate == null &&
                archivedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Identity -> {
            type == CipherType.IDENTITY &&
                deletedDate == null &&
                archivedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Login -> {
            type == CipherType.LOGIN &&
                deletedDate == null &&
                archivedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.SecureNote -> {
            type == CipherType.SECURE_NOTE &&
                deletedDate == null &&
                archivedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.SshKey -> {
            type == CipherType.SSH_KEY &&
                deletedDate == null &&
                archivedDate == null
        }

        is VaultItemListingState.ItemListingType.Vault.Trash -> {
            deletedDate != null
        }

        is VaultItemListingState.ItemListingType.Vault.Archive -> {
            archivedDate != null && deletedDate == null
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
 * Transforms a list of [CipherView] into [VaultItemListingState.ViewState].
 */
@Suppress("CyclomaticComplexMethod", "LongMethod", "LongParameterList")
fun VaultData.toViewState(
    itemListingType: VaultItemListingState.ItemListingType.Vault,
    vaultFilterType: VaultFilterType,
    hasMasterPassword: Boolean,
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    autofillSelectionData: AutofillSelectionData?,
    fido2CreationData: Fido2CreateCredentialRequest?,
    fido2CredentialAutofillViews: List<Fido2CredentialAutofillView>?,
    totpData: TotpData?,
    isPremiumUser: Boolean,
): VaultItemListingState.ViewState {
    val filteredCipherViewList = cipherViewList
        .filter { cipherView ->
            cipherView.determineListingPredicate(itemListingType)
        }
        .toFilteredList(vaultFilterType)

    val folderList =
        (itemListingType as? VaultItemListingState.ItemListingType.Vault.Folder)
            ?.folderId
            ?.let { folderViewList.getFolders(it) }
            .orEmpty()

    val collectionList =
        (itemListingType as? VaultItemListingState.ItemListingType.Vault.Collection)
            ?.let { collectionViewList.getCollections(it.collectionId) }
            .orEmpty()

    return if (folderList.isNotEmpty() || filteredCipherViewList.isNotEmpty() ||
        collectionList.isNotEmpty()
    ) {
        VaultItemListingState.ViewState.Content(
            displayItemList = filteredCipherViewList.toDisplayItemList(
                baseIconUrl = baseIconUrl,
                hasMasterPassword = hasMasterPassword,
                isIconLoadingDisabled = isIconLoadingDisabled,
                isAutofill = autofillSelectionData != null,
                isFido2Creation = fido2CreationData != null,
                fido2CredentialAutofillViews = fido2CredentialAutofillViews,
                isPremiumUser = isPremiumUser,
                isTotp = totpData != null,
            ),
            displayFolderList = folderList.map { folderView ->
                VaultItemListingState.FolderDisplayItem(
                    id = requireNotNull(folderView.id),
                    name = folderView.name,
                    count = this.cipherViewList
                        .count {
                            it.deletedDate == null &&
                                it.archivedDate == null &&
                                !it.id.isNullOrBlank() &&
                                folderView.id == it.folderId
                        },
                )
            },
            displayCollectionList = collectionList.map { collectionView ->
                VaultItemListingState.CollectionDisplayItem(
                    id = requireNotNull(collectionView.id),
                    name = collectionView.name,
                    count = this.cipherViewList
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
            ?.let { R.string.no_items_for_uri.asText(it) }
            ?: fido2CreationData
                ?.origin
                ?.toHostOrPathOrNull()
                ?.let { R.string.no_items_for_uri.asText(it) }
            ?: totpData?.let { R.string.search_for_a_login_or_add_a_new_login.asText() }
            ?: run {
                when (itemListingType) {
                    is VaultItemListingState.ItemListingType.Vault.Folder -> {
                        R.string.no_items_folder
                    }

                    is VaultItemListingState.ItemListingType.Vault.Collection -> {
                        R.string.no_items_collection
                    }

                    VaultItemListingState.ItemListingType.Vault.Trash -> {
                        R.string.no_items_trash
                    }

                    VaultItemListingState.ItemListingType.Vault.Card -> {
                        R.string.no_cards
                    }

                    VaultItemListingState.ItemListingType.Vault.Identity -> {
                        R.string.no_identities
                    }

                    VaultItemListingState.ItemListingType.Vault.Login -> {
                        R.string.no_logins
                    }

                    VaultItemListingState.ItemListingType.Vault.SecureNote -> {
                        R.string.no_notes
                    }

                    VaultItemListingState.ItemListingType.Vault.SshKey -> {
                        R.string.no_ssh_keys
                    }

                    VaultItemListingState.ItemListingType.Vault.Archive -> {
                        R.string.no_items_archive_description
                    }
                }
                    .asText()
            }
        val shouldShowAddButton = when (itemListingType) {
            VaultItemListingState.ItemListingType.Vault.Trash,
            VaultItemListingState.ItemListingType.Vault.SshKey,
            VaultItemListingState.ItemListingType.Vault.Archive,
                -> false

            else -> true
        }
        VaultItemListingState.ViewState.NoItems(
            header = totpData
                ?.let { R.string.no_items_for_vault.asText(it.issuer ?: it.accountName ?: "") }
                ?: run {
                    when (itemListingType) {
                        VaultItemListingState.ItemListingType.Vault.Archive -> {
                            R.string.no_items_archive.asText()
                        }

                        else -> null
                    }
                },
            message = message,
            shouldShowAddButton = shouldShowAddButton,
            buttonText = fido2CreationData
                ?.let { R.string.save_passkey_as_new_login.asText() }
                ?: run {
                    when (itemListingType) {
                        VaultItemListingState.ItemListingType.Vault.Card -> {
                            R.string.new_card
                        }

                        VaultItemListingState.ItemListingType.Vault.Identity -> {
                            R.string.new_identity
                        }

                        VaultItemListingState.ItemListingType.Vault.Login -> {
                            R.string.new_login
                        }

                        VaultItemListingState.ItemListingType.Vault.SecureNote -> {
                            R.string.new_note
                        }

                        VaultItemListingState.ItemListingType.Vault.SshKey -> {
                            R.string.new_ssh_key
                        }

                        else -> R.string.new_item
                    }
                        .asText()
                },
            vectorRes = totpData
                ?.let { R.drawable.img_folder_question }
                ?: when (itemListingType) {
                    VaultItemListingState.ItemListingType.Vault.Archive ->
                        R.drawable.no_archives_icon

                    else -> null
                },
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
                VaultItemListingState.ItemListingType.Send.SendFile -> R.string.no_file_sends
                VaultItemListingState.ItemListingType.Send.SendText -> R.string.no_text_sends
            }
                .asText(),
            shouldShowAddButton = true,
            buttonText = when (itemListingType) {
                VaultItemListingState.ItemListingType.Send.SendFile -> R.string.new_file_send
                VaultItemListingState.ItemListingType.Send.SendText -> R.string.new_text_send
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
        is VaultItemListingState.ItemListingType.Vault.Archive -> this
    }

@Suppress("LongParameterList")
private fun List<CipherView>.toDisplayItemList(
    baseIconUrl: String,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
    isFido2Creation: Boolean,
    fido2CredentialAutofillViews: List<Fido2CredentialAutofillView>?,
    isPremiumUser: Boolean,
    isTotp: Boolean,
): List<VaultItemListingState.DisplayItem> =
    this.map {
        it.toDisplayItem(
            baseIconUrl = baseIconUrl,
            hasMasterPassword = hasMasterPassword,
            isIconLoadingDisabled = isIconLoadingDisabled,
            isAutofill = isAutofill,
            isFido2Creation = isFido2Creation,
            fido2CredentialAutofillView = fido2CredentialAutofillViews
                ?.firstOrNull { fido2CredentialAutofillView ->
                    fido2CredentialAutofillView.cipherId == it.id
                },
            isPremiumUser = isPremiumUser,
            isTotp = isTotp,
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
private fun CipherView.toDisplayItem(
    baseIconUrl: String,
    hasMasterPassword: Boolean,
    isIconLoadingDisabled: Boolean,
    isAutofill: Boolean,
    isFido2Creation: Boolean,
    fido2CredentialAutofillView: Fido2CredentialAutofillView?,
    isPremiumUser: Boolean,
    isTotp: Boolean,
): VaultItemListingState.DisplayItem =
    VaultItemListingState.DisplayItem(
        id = id.orEmpty(),
        title = name,
        titleTestTag = "CipherNameLabel",
        secondSubtitle = this.toSecondSubtitle(fido2CredentialAutofillView?.rpId),
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
        isFido2Creation = isFido2Creation,
        isTotp = isTotp,
        shouldShowMasterPasswordReprompt = (reprompt == CipherRepromptType.PASSWORD) &&
            hasMasterPassword,
        type = this.type,
    )

private fun CipherView.toSecondSubtitle(fido2CredentialRpId: String?): String? =
    fido2CredentialRpId
        ?.takeIf { this.type == CipherType.LOGIN && it.isNotEmpty() && it != this.name }

private fun CipherView.toSubtitleTestTag(
    isAutofill: Boolean,
    isFido2Creation: Boolean,
): String =
    if ((isAutofill || isFido2Creation)) {
        if (this.isActiveWithFido2Credentials) "PasskeyName" else "PasswordName"
    } else {
        "CipherSubTitleLabel"
    }

private fun CipherView.toIconTestTag(): String =
    when (type) {
        CipherType.LOGIN -> "LoginCipherIcon"
        CipherType.SECURE_NOTE -> "SecureNoteCipherIcon"
        CipherType.CARD -> "CardCipherIcon"
        CipherType.IDENTITY -> "IdentityCipherIcon"
        CipherType.SSH_KEY -> "SshKeyCipherIcon"
    }

private fun CipherView.toIconData(
    baseIconUrl: String,
    isIconLoadingDisabled: Boolean,
    usePasskeyDefaultIcon: Boolean,
): IconData {
    return when (this.type) {
        CipherType.LOGIN -> {
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
        title = name,
        titleTestTag = "SendNameLabel",
        secondSubtitle = null,
        secondSubtitleTestTag = null,
        subtitle = deletionDate.toFormattedPattern(DELETION_DATE_PATTERN, clock),
        subtitleTestTag = "SendDateLabel",
        iconData = IconData.Local(
            iconRes = when (type) {
                SendType.TEXT -> R.drawable.ic_file_text
                SendType.FILE -> R.drawable.ic_file
            },
        ),
        iconTestTag = null,
        extraIconList = toLabelIcons(clock = clock),
        overflowOptions = toOverflowActions(baseWebSendUrl = baseWebSendUrl),
        optionsTestTag = "SendOptionsButton",
        isAutofill = false,
        shouldShowMasterPasswordReprompt = false,
        isFido2Creation = false,
        isTotp = false,
        type = null,
    )

@get:DrawableRes
private val CipherType.iconRes: Int
    get() = when (this) {
        CipherType.LOGIN -> R.drawable.ic_globe
        CipherType.SECURE_NOTE -> R.drawable.ic_note
        CipherType.CARD -> R.drawable.ic_payment_card
        CipherType.IDENTITY -> R.drawable.ic_id_card
        CipherType.SSH_KEY -> R.drawable.ic_ssh_key
    }
