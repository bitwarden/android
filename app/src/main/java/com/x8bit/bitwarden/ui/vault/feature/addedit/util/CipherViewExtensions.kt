@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.vault.feature.addedit.util

import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.CollectionView
import com.bitwarden.vault.Fido2Credential
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.FieldView
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.LoginUriView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.platform.util.toFormattedPattern
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.TotpData
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType.Companion.fromId
import com.x8bit.bitwarden.ui.vault.model.findVaultCardBrandWithNameOrNull
import java.time.Clock
import java.util.UUID

private const val PASSKEY_CREATION_DATE_PATTERN: String = "M/d/yy"
private const val PASSKEY_CREATION_TIME_PATTERN: String = "hh:mm a"

/**
 * Transforms [CipherView] into [VaultAddEditState.ViewState].
 */
@Suppress("LongMethod", "LongParameterList")
fun CipherView.toViewState(
    isClone: Boolean,
    isIndividualVaultDisabled: Boolean,
    totpData: TotpData?,
    resourceManager: ResourceManager,
    clock: Clock,
    canDelete: Boolean,
    canAssignToCollections: Boolean,
): VaultAddEditState.ViewState =
    VaultAddEditState.ViewState.Content(
        type = when (type) {
            CipherType.LOGIN -> {
                VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = login?.username.orEmpty(),
                    password = login?.password.orEmpty(),
                    totp = totpData?.uri ?: login?.totp,
                    canViewPassword = this.viewPassword,
                    canEditItem = this.edit,
                    uriList = login?.uris.toUriItems(),
                    fido2CredentialCreationDateTime = login
                        ?.fido2Credentials
                        .getPrimaryFido2CredentialOrNull(isClone)
                        ?.getCreationDateTime(clock),
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

            CipherType.SSH_KEY -> VaultAddEditState.ViewState.Content.ItemType.SshKey(
                publicKey = sshKey?.publicKey.orEmpty(),
                privateKey = sshKey?.privateKey.orEmpty(),
                fingerprint = sshKey?.fingerprint.orEmpty(),
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
            hasOrganizations = false,
            customFieldData = this.fields.orEmpty().map { it.toCustomField() },
            canDelete = canDelete,
            canAssignToCollections = canAssignToCollections,
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
                )
                    ?: currentContentState.common.selectedFolderId,
                availableFolders = folderViewList.toAvailableFolders(
                    resourceManager = resourceManager,
                ),
                selectedOwnerId = activeAccount
                    .toSelectedOwnerId(cipherView = currentContentState.common.originalCipher)
                    ?: collectionViewList
                        .firstOrNull { it.id == currentContentState.common.selectedCollectionId }
                        ?.organizationId,
                availableOwners = activeAccount.toAvailableOwners(
                    collectionViewList = collectionViewList,
                    cipherView = currentContentState.common.originalCipher,
                    isIndividualVaultDisabled = isIndividualVaultDisabled,
                    selectedCollectionId = currentContentState.common.selectedCollectionId,
                ),
                isUnlockWithPasswordEnabled = activeAccount.hasMasterPassword,
                hasOrganizations = activeAccount.organizations.isNotEmpty(),
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
    cipherView: CipherView?,
    isIndividualVaultDisabled: Boolean,
    selectedCollectionId: String? = null,
): List<VaultAddEditState.Owner> =
    listOfNotNull(
        VaultAddEditState
            .Owner(
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
                                isSelected = (cipherView
                                    ?.collectionIds
                                    ?.contains(collection.id))
                                    ?: (selectedCollectionId != null &&
                                        collection.id == selectedCollectionId),
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
                checksum = null,
            ),
        )
    } else {
        this.map { loginUriView ->
            UriItem(
                id = UUID.randomUUID().toString(),
                uri = loginUriView.uri,
                match = loginUriView.match,
                checksum = loginUriView.uriChecksum,
            )
        }
    }

/**
 * Retrieves the cipher's primary (first) FIDO2 credential, or null if there is no FIDO2 credential
 * assigned.
 */
private fun List<Fido2Credential>?.getPrimaryFido2CredentialOrNull(
    isClone: Boolean,
): Fido2Credential? {
    if (isNullOrEmpty() || isClone) return null

    return first()
}

/**
 * Return the creation date and time of the primary FIDO2 credential, formatted as
 * "M/d/yy, hh:mm a".
 */
private fun Fido2Credential.getCreationDateTime(clock: Clock) = R.string.created_xy.asText(
    creationDate.toFormattedPattern(pattern = PASSKEY_CREATION_DATE_PATTERN, clock = clock),
    creationDate.toFormattedPattern(pattern = PASSKEY_CREATION_TIME_PATTERN, clock = clock),
)
