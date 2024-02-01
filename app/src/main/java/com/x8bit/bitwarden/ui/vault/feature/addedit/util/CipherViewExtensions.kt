@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.CollectionView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.bitwarden.core.FolderView
import com.bitwarden.core.LoginUriView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType.Companion.fromId
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull
import java.util.UUID

/**
 * Transforms [CipherView] into [VaultAddEditState.ViewState].
 */
fun CipherView.toViewState(
    isClone: Boolean,
    isIndividualVaultDisabled: Boolean,
    resourceManager: ResourceManager,
): VaultAddEditState.ViewState =
    VaultAddEditState.ViewState.Content(
        type = when (type) {
            CipherType.LOGIN -> {
                VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = login?.username.orEmpty(),
                    password = login?.password.orEmpty(),
                    uriList = login?.uris.toUriItems(),
                    totp = login?.totp,
                    canViewPassword = this.viewPassword,
                )
            }

            CipherType.SECURE_NOTE -> VaultAddEditState.ViewState.Content.ItemType.SecureNotes
            CipherType.CARD -> VaultAddEditState.ViewState.Content.ItemType.Card(
                cardHolderName = card?.cardholderName.orEmpty(),
                number = card?.number.orEmpty(),
                brand = card?.brand.toBrandOrDefault(),
                expirationMonth = card?.expMonth.toExpirationMonthOrDefault(),
                expirationYear = card?.expYear.orEmpty(),
                securityCode = card?.code.orEmpty(),
            )

            CipherType.IDENTITY -> VaultAddEditState.ViewState.Content.ItemType.Identity(
                selectedTitle = identity?.title.toTitleOrDefault(),
                firstName = identity?.firstName.orEmpty(),
                middleName = identity?.middleName.orEmpty(),
                lastName = identity?.lastName.orEmpty(),
                username = identity?.username.orEmpty(),
                company = identity?.company.orEmpty(),
                ssn = identity?.ssn.orEmpty(),
                passportNumber = identity?.passportNumber.orEmpty(),
                licenseNumber = identity?.licenseNumber.orEmpty(),
                email = identity?.email.orEmpty(),
                phone = identity?.phone.orEmpty(),
                address1 = identity?.address1.orEmpty(),
                address2 = identity?.address2.orEmpty(),
                address3 = identity?.address3.orEmpty(),
                city = identity?.city.orEmpty(),
                zip = identity?.postalCode.orEmpty(),
                country = identity?.country.orEmpty(),
            )
        },
        common = VaultAddEditState.ViewState.Content.Common(
            originalCipher = this,
            name = name.appendCloneTextIfRequired(
                isClone = isClone,
                resourceManager = resourceManager,
            ),
            favorite = this.favorite,
            masterPasswordReprompt = this.reprompt == CipherRepromptType.PASSWORD,
            notes = this.notes.orEmpty(),
            availableOwners = emptyList(),
            customFieldData = this.fields.orEmpty().map { it.toCustomField() },
        ),
        isIndividualVaultDisabled = isIndividualVaultDisabled,
    )

/**
 * Adds Folder and Owner data to [VaultAddEditState.ViewState].
 */
fun VaultAddEditState.ViewState.appendFolderAndOwnerData(
    folderViewList: List<FolderView>,
    collectionViewList: List<CollectionView>,
    activeAccount: UserState.Account,
    isIndividualVaultDisabled: Boolean,
    resourceManager: ResourceManager,
): VaultAddEditState.ViewState {
    return (this as? VaultAddEditState.ViewState.Content)?.let { currentContentState ->
        currentContentState.copy(
            common = currentContentState.common.copy(
                selectedFolderId = folderViewList.toSelectedFolderId(
                    cipherView = currentContentState.common.originalCipher,
                ),
                availableFolders = folderViewList.toAvailableFolders(
                    resourceManager = resourceManager,
                ),
                selectedOwnerId = activeAccount.toSelectedOwnerId(
                    cipherView = currentContentState.common.originalCipher,
                ),
                availableOwners = activeAccount.toAvailableOwners(
                    collectionViewList = collectionViewList,
                    isIndividualVaultDisabled = isIndividualVaultDisabled,
                ),
            ),
        )
    } ?: this
}

/**
 * Validates a [CipherView] otherwise returning a [VaultAddEditState.ViewState.Error].
 */
fun CipherView?.validateCipherOrReturnErrorState(
    currentAccount: UserState.Account?,
    vaultAddEditType: VaultAddEditType,
    lambda: (
        currentAccount: UserState.Account,
        cipherView: CipherView?,
    ) -> VaultAddEditState.ViewState,
): VaultAddEditState.ViewState =
    if (currentAccount == null ||
        (vaultAddEditType is VaultAddEditType.EditItem && this == null)
    ) {
        VaultAddEditState.ViewState.Error(R.string.generic_error_message.asText())
    } else {
        lambda(currentAccount, this)
    }

private fun List<FolderView>.toSelectedFolderId(cipherView: CipherView?): String? =
    cipherView
        ?.folderId
        ?.takeIf { id -> id in map { it.id } }

private fun List<FolderView>.toAvailableFolders(
    resourceManager: ResourceManager,
): List<VaultAddEditState.Folder> =
    listOf(
        VaultAddEditState.Folder(
            id = null,
            name = resourceManager.getString(R.string.folder_none),
        ),
    )
        .plus(
            map { VaultAddEditState.Folder(name = it.name, id = it.id.toString()) },
        )

private fun UserState.Account.toSelectedOwnerId(cipherView: CipherView?): String? =
    cipherView
        ?.organizationId
        ?.takeIf { id -> id in organizations.map { it.id } }

private fun UserState.Account.toAvailableOwners(
    collectionViewList: List<CollectionView>,
    isIndividualVaultDisabled: Boolean,
): List<VaultAddEditState.Owner> =
    listOfNotNull(
        VaultAddEditState.Owner(
            name = email,
            id = null,
            collections = emptyList(),
        )
            .takeUnless { isIndividualVaultDisabled },
        *organizations
            .map {
                VaultAddEditState.Owner(
                    name = it.name.orEmpty(),
                    id = it.id,
                    collections = collectionViewList
                        .filter { collection ->
                            collection.organizationId == it.id &&
                                collection.id != null
                        }
                        .map { collection ->
                            VaultCollection(
                                id = collection.id.orEmpty(),
                                name = collection.name,
                                isSelected = false,
                            )
                        },
                )
            }
            .toTypedArray(),
    )

private fun FieldView.toCustomField() =
    when (this.type) {
        FieldType.TEXT -> VaultAddEditState.Custom.TextField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            value = this.value.orEmpty(),
        )

        FieldType.HIDDEN -> VaultAddEditState.Custom.HiddenField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            value = this.value.orEmpty(),
        )

        FieldType.BOOLEAN -> VaultAddEditState.Custom.BooleanField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            value = this.value.toBoolean(),
        )

        FieldType.LINKED -> VaultAddEditState.Custom.LinkedField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            vaultLinkedFieldType = fromId(requireNotNull(this.linkedId)),
        )
    }

private fun String?.toTitleOrDefault(): VaultIdentityTitle =
    VaultIdentityTitle
        .entries
        .find { it.name == this }
        ?: VaultIdentityTitle.SELECT

private fun String?.toBrandOrDefault(): VaultCardBrand =
    this
        ?.findVaultCardBrandWithNameOrNull()
        ?: VaultCardBrand.SELECT

private fun String?.toExpirationMonthOrDefault(): VaultCardExpirationMonth =
    VaultCardExpirationMonth
        .entries
        .find { it.number == this }
        ?: VaultCardExpirationMonth.SELECT

private fun String.appendCloneTextIfRequired(
    isClone: Boolean,
    resourceManager: ResourceManager,
): String =
    if (isClone) {
        plus(" - ${resourceManager.getString(R.string.clone)}")
    } else {
        this
    }

private fun List<LoginUriView>?.toUriItems(): List<UriItem> =
    if (this.isNullOrEmpty()) {
        listOf(
            UriItem(
                id = UUID.randomUUID().toString(),
                uri = "",
                match = null,
            ),
        )
    } else {
        this.map { loginUriView ->
            UriItem(
                id = UUID.randomUUID().toString(),
                uri = loginUriView.uri,
                match = loginUriView.match,
            )
        }
    }
