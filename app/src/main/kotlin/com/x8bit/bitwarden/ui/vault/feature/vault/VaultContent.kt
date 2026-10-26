package com.x8bit.bitwarden.ui.vault.feature.vault

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.bitwarden.ui.platform.base.util.standardHorizontalMargin
import com.bitwarden.ui.platform.base.util.toListItemCardStyle
import com.bitwarden.ui.platform.components.dialog.BitwardenTwoButtonDialog
import com.bitwarden.ui.platform.components.header.BitwardenExpandingListHeader
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.components.model.CardStyle
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.platform.repository.model.VaultSection
import com.x8bit.bitwarden.ui.platform.components.dialog.BitwardenMasterPasswordDialog
import com.x8bit.bitwarden.ui.platform.components.listitem.BitwardenGroupItem
import com.x8bit.bitwarden.ui.platform.composition.util.vfo1Foundation
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.handlers.VaultHandlers
import kotlinx.collections.immutable.ImmutableSet

private const val TOTP_TYPES_COUNT: Int = 1
private const val HIDDEN_TYPES_COUNT: Int = 2

/**
 * Content view for the [VaultScreen].
 */
@Composable
@Suppress("LongMethod")
fun VaultContent(
    state: VaultState.ViewState.Content,
    actionCardState: VaultState.ActionCardState?,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
    modifier: Modifier = Modifier,
) {
    // Handles the master password prompt for the row click
    var masterPasswordRepromptItem by rememberSaveable {
        mutableStateOf<VaultState.ViewState.VaultItem?>(value = null)
    }
    masterPasswordRepromptItem?.let { action ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                masterPasswordRepromptItem = null
                vaultHandlers.masterPasswordRepromptSubmit(action, password)
            },
            onDismissRequest = { masterPasswordRepromptItem = null },
        )
    }
    // Handles the master password prompt for the overflow clicks
    var overflowMasterPasswordRepromptAction by rememberSaveable {
        mutableStateOf<ListingItemOverflowAction.VaultAction?>(value = null)
    }
    overflowMasterPasswordRepromptAction?.let { action ->
        BitwardenMasterPasswordDialog(
            onConfirmClick = { password ->
                overflowMasterPasswordRepromptAction = null
                vaultHandlers.overflowMasterPasswordRepromptSubmit(action, password)
            },
            onDismissRequest = { overflowMasterPasswordRepromptAction = null },
        )
    }

    var overflowSpeedBumpAction: ListingItemOverflowAction.VaultAction? by rememberSaveable {
        mutableStateOf(value = null)
    }
    overflowSpeedBumpAction?.let { action ->
        action
            .speedBump
            ?.let { speedBump ->
                BitwardenTwoButtonDialog(
                    twoButtonDialogData = speedBump,
                    onConfirmClick = {
                        overflowSpeedBumpAction = null
                        vaultHandlers.overflowOptionClick(action)
                    },
                    onDismissClick = { overflowSpeedBumpAction = null },
                    onDismissRequest = { overflowSpeedBumpAction = null },
                )
            }
            ?: run {
                // If we somehow get here and there is no speed bump, then we should keep on going.
                overflowSpeedBumpAction = null
                vaultHandlers.overflowOptionClick(action)
            }
    }

    val onVaultItemClick: (VaultState.ViewState.VaultItem) -> Unit = { item ->
        if (item.shouldShowMasterPasswordReprompt) {
            masterPasswordRepromptItem = item
        } else {
            vaultHandlers.vaultItemClick(item)
        }
    }
    val onVaultItemOverflowOptionClick: (
        VaultState.ViewState.VaultItem,
        ListingItemOverflowAction.VaultAction,
    ) -> Unit = { item, action ->
        if (item.shouldShowMasterPasswordReprompt && action.requiresPasswordReprompt) {
            overflowMasterPasswordRepromptAction = action
        } else if (action.speedBump != null) {
            overflowSpeedBumpAction = action
        } else {
            vaultHandlers.overflowOptionClick(action)
        }
    }
    val foldersLabelRes = vfo1Foundation(
        new = BitwardenString.my_folders,
        old = BitwardenString.folders,
    )
    val collectionsLabelRes = vfo1Foundation(
        new = BitwardenString.shared_folders,
        old = BitwardenString.collections,
    )
    LazyColumn(
        modifier = modifier,
    ) {
        actionCardState?.let {
            item(key = "action_card") {
                Spacer(modifier = Modifier.height(height = 12.dp))
                VaultActionCard(
                    actionCardState = it,
                    vaultHandlers = vaultHandlers,
                    modifier = Modifier
                        .animateItem()
                        .fillMaxWidth()
                        .standardHorizontalMargin(),
                )
            }
        }

        totpSection(
            state = state,
            collapsedSections = collapsedSections,
            vaultHandlers = vaultHandlers,
        )

        favoritesSection(
            state = state,
            collapsedSections = collapsedSections,
            vaultHandlers = vaultHandlers,
            onVaultItemClick = onVaultItemClick,
            onVaultItemOverflowOptionClick = onVaultItemOverflowOptionClick,
        )

        typesSection(
            state = state,
            collapsedSections = collapsedSections,
            vaultHandlers = vaultHandlers,
        )

        foldersSection(
            state = state,
            collapsedSections = collapsedSections,
            vaultHandlers = vaultHandlers,
            labelRes = foldersLabelRes,
        )

        noFolderSection(
            state = state,
            collapsedSections = collapsedSections,
            vaultHandlers = vaultHandlers,
            onVaultItemClick = onVaultItemClick,
            onVaultItemOverflowOptionClick = onVaultItemOverflowOptionClick,
        )

        collectionsSection(
            state = state,
            collapsedSections = collapsedSections,
            vaultHandlers = vaultHandlers,
            labelRes = collectionsLabelRes,
        )

        hiddenItemsSection(
            state = state,
            collapsedSections = collapsedSections,
            vaultHandlers = vaultHandlers,
        )

        item(key = "bottom_padding") {
            Spacer(modifier = Modifier.height(height = 88.dp))
            Spacer(modifier = Modifier.navigationBarsPadding())
        }
    }
}

/**
 * Adds the verification codes section to the list.
 */
private fun LazyListScope.totpSection(
    state: VaultState.ViewState.Content,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
) {
    if (state.totpItemsCount <= 0) return
    expandingSectionHeader(
        section = VaultSection.TOTP,
        labelRes = BitwardenString.totp,
        supportingLabel = TOTP_TYPES_COUNT.toString(),
        collapsedSections = collapsedSections,
        onClick = vaultHandlers.sectionHeaderClick,
    )
    if (VaultSection.TOTP in collapsedSections) return
    item(key = "verification_codes_group") {
        BitwardenGroupItem(
            startIcon = IconData.Local(iconRes = BitwardenDrawable.ic_clock),
            label = stringResource(id = BitwardenString.verification_codes),
            supportingLabel = state.totpItemsCount.toString(),
            onClick = vaultHandlers.verificationCodesClick,
            cardStyle = CardStyle.Full,
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("VerificationCodesFilter")
                .standardHorizontalMargin(),
        )
    }
}

/**
 * Adds the favorite items section to the list.
 */
private fun LazyListScope.favoritesSection(
    state: VaultState.ViewState.Content,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
    onVaultItemClick: (VaultState.ViewState.VaultItem) -> Unit,
    onVaultItemOverflowOptionClick: (
        VaultState.ViewState.VaultItem,
        ListingItemOverflowAction.VaultAction,
    ) -> Unit,
) {
    if (state.favoriteItems.isEmpty()) return
    expandingSectionHeader(
        section = VaultSection.FAVORITES,
        labelRes = BitwardenString.favorites,
        supportingLabel = state.favoriteItems.count().toString(),
        collapsedSections = collapsedSections,
        onClick = vaultHandlers.sectionHeaderClick,
    )
    if (VaultSection.FAVORITES in collapsedSections) return
    itemsIndexed(
        items = state.favoriteItems,
        key = { _, favorite -> "favorite_${favorite.id}" },
    ) { index, favoriteItem ->
        VaultEntryListItem(
            startIcon = favoriteItem.startIcon,
            startIconTestTag = favoriteItem.startIconTestTag,
            trailingLabelIcons = favoriteItem.extraIconList,
            label = favoriteItem.name(),
            supportingLabel = favoriteItem.supportingLabel?.invoke(),
            onClick = { onVaultItemClick(favoriteItem) },
            overflowOptions = favoriteItem.overflowOptions,
            onOverflowOptionClick = { action ->
                onVaultItemOverflowOptionClick(favoriteItem, action)
            },
            cardStyle = state
                .favoriteItems
                .toListItemCardStyle(index = index, dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("CipherCell")
                .standardHorizontalMargin(),
        )
    }
}

/**
 * Adds the item types section to the list.
 */
@Suppress("LongMethod")
private fun LazyListScope.typesSection(
    state: VaultState.ViewState.Content,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
) {
    expandingSectionHeader(
        section = VaultSection.TYPES,
        labelRes = BitwardenString.types,
        supportingLabel = state.itemTypesCount.toString(),
        collapsedSections = collapsedSections,
        onClick = vaultHandlers.sectionHeaderClick,
    )
    if (VaultSection.TYPES in collapsedSections) return
    item(key = "logins_group") {
        BitwardenGroupItem(
            startIcon = IconData.Local(
                iconRes = BitwardenDrawable.ic_globe,
                testTag = "LoginCipherIcon",
            ),
            label = stringResource(id = BitwardenString.type_login),
            supportingLabel = state.loginItemsCount.toString(),
            onClick = vaultHandlers.loginGroupClick,
            cardStyle = CardStyle.Top(dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("LoginFilter")
                .standardHorizontalMargin(),
        )
    }

    if (state.showCardGroup) {
        item(key = "cards_group") {
            BitwardenGroupItem(
                startIcon = IconData.Local(
                    iconRes = BitwardenDrawable.ic_payment_card,
                    testTag = "CardCipherIcon",
                ),
                label = stringResource(id = BitwardenString.type_card),
                supportingLabel = state.cardItemsCount.toString(),
                onClick = vaultHandlers.cardGroupClick,
                cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .testTag("CardFilter")
                    .standardHorizontalMargin(),
            )
        }
    }

    if (state.showBankAccountGroup) {
        item(key = "bank_accounts_group") {
            BitwardenGroupItem(
                startIcon = IconData.Local(
                    iconRes = BitwardenDrawable.ic_payment_card,
                    testTag = "BankAccountCipherIcon",
                ),
                label = stringResource(id = BitwardenString.type_bank_account),
                supportingLabel = state.bankAccountItemsCount.toString(),
                onClick = vaultHandlers.bankAccountGroupClick,
                cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .testTag("BankAccountFilter")
                    .standardHorizontalMargin(),
            )
        }
    }

    item(key = "identities_group") {
        BitwardenGroupItem(
            startIcon = IconData.Local(
                iconRes = BitwardenDrawable.ic_id_card,
                testTag = "IdentityCipherIcon",
            ),
            label = stringResource(id = BitwardenString.type_identity),
            supportingLabel = state.identityItemsCount.toString(),
            onClick = vaultHandlers.identityGroupClick,
            cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("IdentityFilter")
                .standardHorizontalMargin(),
        )
    }

    if (state.showLicenseGroup) {
        item(key = "licenses_group") {
            BitwardenGroupItem(
                startIcon = IconData.Local(
                    iconRes = BitwardenDrawable.ic_id_card,
                    testTag = "LicenseCipherIcon",
                ),
                label = stringResource(id = BitwardenString.type_license),
                supportingLabel = state.licenseItemsCount.toString(),
                onClick = vaultHandlers.licenseGroupClick,
                cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .testTag("LicenseFilter")
                    .standardHorizontalMargin(),
            )
        }
    }

    if (state.showPassportGroup) {
        item(key = "passports_group") {
            BitwardenGroupItem(
                startIcon = IconData.Local(
                    iconRes = BitwardenDrawable.ic_passport,
                    testTag = "PassportCipherIcon",
                ),
                label = stringResource(id = BitwardenString.type_passport),
                supportingLabel = state.passportItemsCount.toString(),
                onClick = vaultHandlers.passportGroupClick,
                cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
                modifier = Modifier
                    .animateItem()
                    .fillMaxWidth()
                    .testTag("PassportFilter")
                    .standardHorizontalMargin(),
            )
        }
    }

    item(key = "notes_group") {
        BitwardenGroupItem(
            startIcon = IconData.Local(
                iconRes = BitwardenDrawable.ic_note,
                testTag = "SecureNoteCipherIcon",
            ),
            label = stringResource(id = BitwardenString.type_secure_note),
            supportingLabel = state.secureNoteItemsCount.toString(),
            onClick = vaultHandlers.secureNoteGroupClick,
            cardStyle = CardStyle.Middle(dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("SecureNoteFilter")
                .standardHorizontalMargin(),
        )
    }

    item(key = "ssh_keys_group") {
        BitwardenGroupItem(
            startIcon = IconData.Local(
                iconRes = BitwardenDrawable.ic_ssh_key,
                testTag = "SshKeyCipherIcon",
            ),
            label = stringResource(id = BitwardenString.type_ssh_key),
            supportingLabel = state.sshKeyItemsCount.toString(),
            onClick = vaultHandlers.sshKeyGroupClick,
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("SshKeyFilter")
                .standardHorizontalMargin(),
        )
    }
}

/**
 * Adds the folders section to the list.
 */
private fun LazyListScope.foldersSection(
    state: VaultState.ViewState.Content,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
    @StringRes labelRes: Int,
) {
    if (state.folderItems.isEmpty()) return
    expandingSectionHeader(
        section = VaultSection.FOLDERS,
        labelRes = labelRes,
        supportingLabel = state.folderItems.count().toString(),
        collapsedSections = collapsedSections,
        onClick = vaultHandlers.sectionHeaderClick,
    )
    if (VaultSection.FOLDERS in collapsedSections) return
    itemsIndexed(
        items = state.folderItems,
        key = { _, folder -> "folder_${folder.id}" },
    ) { index, folder ->
        BitwardenGroupItem(
            startIcon = IconData.Local(iconRes = BitwardenDrawable.ic_folder),
            label = folder.name(),
            supportingLabel = folder.itemCount.toString(),
            onClick = { vaultHandlers.folderClick(folder) },
            cardStyle = state
                .folderItems
                .toListItemCardStyle(index = index, dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("FolderFilter")
                .standardHorizontalMargin(),
        )
    }
}

/**
 * Adds the items without a folder section to the list.
 */
private fun LazyListScope.noFolderSection(
    state: VaultState.ViewState.Content,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
    onVaultItemClick: (VaultState.ViewState.VaultItem) -> Unit,
    onVaultItemOverflowOptionClick: (
        VaultState.ViewState.VaultItem,
        ListingItemOverflowAction.VaultAction,
    ) -> Unit,
) {
    if (state.noFolderItems.isEmpty()) return
    expandingSectionHeader(
        section = VaultSection.NO_FOLDER,
        labelRes = BitwardenString.folder_none,
        supportingLabel = state.noFolderItems.count().toString(),
        collapsedSections = collapsedSections,
        onClick = vaultHandlers.sectionHeaderClick,
    )
    if (VaultSection.NO_FOLDER in collapsedSections) return
    itemsIndexed(
        items = state.noFolderItems,
        key = { _, noFolderItem -> "no_folder_${noFolderItem.id}" },
    ) { index, noFolderItem ->
        VaultEntryListItem(
            startIcon = noFolderItem.startIcon,
            startIconTestTag = noFolderItem.startIconTestTag,
            trailingLabelIcons = noFolderItem.extraIconList,
            label = noFolderItem.name(),
            supportingLabel = noFolderItem.supportingLabel?.invoke(),
            onClick = { onVaultItemClick(noFolderItem) },
            overflowOptions = noFolderItem.overflowOptions,
            onOverflowOptionClick = { action ->
                onVaultItemOverflowOptionClick(noFolderItem, action)
            },
            cardStyle = state
                .noFolderItems
                .toListItemCardStyle(index = index, dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("CipherCell")
                .standardHorizontalMargin(),
        )
    }
}

/**
 * Adds the collections section to the list.
 */
private fun LazyListScope.collectionsSection(
    state: VaultState.ViewState.Content,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
    @StringRes labelRes: Int,
) {
    if (state.collectionItems.isEmpty()) return
    expandingSectionHeader(
        section = VaultSection.COLLECTIONS,
        labelRes = labelRes,
        supportingLabel = state.collectionItems.count().toString(),
        collapsedSections = collapsedSections,
        onClick = vaultHandlers.sectionHeaderClick,
    )
    if (VaultSection.COLLECTIONS in collapsedSections) return
    itemsIndexed(
        items = state.collectionItems,
        key = { _, collection -> "collection_${collection.id}" },
    ) { index, collection ->
        BitwardenGroupItem(
            startIcon = IconData.Local(
                iconRes = vfo1Foundation(
                    new = BitwardenDrawable.ic_shared_folder,
                    old = BitwardenDrawable.ic_collections,
                ),
            ),
            label = collection.name,
            supportingLabel = collection.itemCount.toString(),
            onClick = { vaultHandlers.collectionClick(collection) },
            cardStyle = state
                .collectionItems
                .toListItemCardStyle(index = index, dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("CollectionFilter")
                .standardHorizontalMargin(),
        )
    }
}

/**
 * Adds the hidden items section to the list.
 */
private fun LazyListScope.hiddenItemsSection(
    state: VaultState.ViewState.Content,
    collapsedSections: ImmutableSet<VaultSection>,
    vaultHandlers: VaultHandlers,
) {
    expandingSectionHeader(
        section = VaultSection.HIDDEN_ITEMS,
        labelRes = BitwardenString.hidden_items,
        supportingLabel = HIDDEN_TYPES_COUNT.toString(),
        collapsedSections = collapsedSections,
        onClick = vaultHandlers.sectionHeaderClick,
    )
    if (VaultSection.HIDDEN_ITEMS in collapsedSections) return
    item(key = "archive_group") {
        BitwardenGroupItem(
            startIcon = IconData.Local(iconRes = BitwardenDrawable.ic_archive),
            endIcon = state.archiveEndIcon?.let { IconData.Local(iconRes = it) },
            label = stringResource(id = BitwardenString.archive_noun),
            subLabel = state.archiveSubText?.invoke(),
            supportingLabel = state.archivedItemsCount?.toString().orEmpty(),
            onClick = vaultHandlers.archiveClick,
            cardStyle = CardStyle.Top(dividerPadding = 56.dp),
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag(tag = "ArchiveFilter")
                .standardHorizontalMargin(),
        )
    }

    item(key = "trash_group") {
        BitwardenGroupItem(
            startIcon = IconData.Local(iconRes = BitwardenDrawable.ic_trash),
            label = stringResource(id = BitwardenString.trash),
            supportingLabel = state.trashItemsCount.toString(),
            onClick = vaultHandlers.trashClick,
            cardStyle = CardStyle.Bottom,
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .testTag("TrashFilter")
                .standardHorizontalMargin(),
        )
    }
}

/**
 * Adds an expandable header for the given vault [section] to the list.
 */
private fun LazyListScope.expandingSectionHeader(
    section: VaultSection,
    @StringRes labelRes: Int,
    supportingLabel: String,
    collapsedSections: ImmutableSet<VaultSection>,
    onClick: (VaultSection) -> Unit,
) {
    item(key = "${section.name.lowercase()}_header") {
        BitwardenExpandingListHeader(
            label = stringResource(id = labelRes),
            supportingLabel = supportingLabel,
            isExpanded = section !in collapsedSections,
            onClick = { onClick(section) },
            modifier = Modifier
                .animateItem()
                .fillMaxWidth()
                .standardHorizontalMargin(),
        )
    }
}
