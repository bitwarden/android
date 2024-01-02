package com.x8bit.bitwarden.ui.vault.feature.additem.util

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType.Companion.fromId
import java.util.UUID

/**
 * Transforms [CipherView] into [VaultAddItemState.ViewState].
 */
fun CipherView.toViewState(): VaultAddItemState.ViewState =
    VaultAddItemState.ViewState.Content(
        type = when (type) {
            CipherType.LOGIN -> {
                VaultAddItemState.ViewState.Content.ItemType.Login(
                    username = login?.username.orEmpty(),
                    password = login?.password.orEmpty(),
                    uri = login?.uris?.firstOrNull()?.uri.orEmpty(),
                )
            }

            CipherType.SECURE_NOTE -> VaultAddItemState.ViewState.Content.ItemType.SecureNotes
            CipherType.CARD -> VaultAddItemState.ViewState.Content.ItemType.Card
            CipherType.IDENTITY -> VaultAddItemState.ViewState.Content.ItemType.Identity(
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
        common = VaultAddItemState.ViewState.Content.Common(
            originalCipher = this,
            name = this.name,
            favorite = this.favorite,
            masterPasswordReprompt = this.reprompt == CipherRepromptType.PASSWORD,
            notes = this.notes.orEmpty(),
            // TODO: Update these properties to pull folder from data layer (BIT-501)
            folderName = this.folderId?.asText() ?: R.string.folder_none.asText(),
            availableFolders = emptyList(),
            // TODO: Update this property to pull owner from data layer (BIT-501)
            ownership = "",
            // TODO: Update this property to pull available owners from data layer (BIT-501)
            availableOwners = emptyList(),
            customFieldData = this.fields.orEmpty().map { it.toCustomField() },
        ),
    )

private fun FieldView.toCustomField() =
    when (this.type) {
        FieldType.TEXT -> VaultAddItemState.Custom.TextField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            value = this.value.orEmpty(),
        )

        FieldType.HIDDEN -> VaultAddItemState.Custom.HiddenField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            value = this.value.orEmpty(),
        )

        FieldType.BOOLEAN -> VaultAddItemState.Custom.BooleanField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            value = this.value.toBoolean(),
        )

        FieldType.LINKED -> VaultAddItemState.Custom.LinkedField(
            itemId = UUID.randomUUID().toString(),
            name = this.name.orEmpty(),
            vaultLinkedFieldType = fromId(requireNotNull(this.linkedId)),
        )
    }

@Suppress("MaxLineLength")
private fun String?.toTitleOrDefault(): VaultAddItemState.ViewState.Content.ItemType.Identity.Title =
    VaultAddItemState.ViewState.Content.ItemType.Identity.Title
        .entries
        .find { it.name == this }
        ?: VaultAddItemState.ViewState.Content.ItemType.Identity.Title.MR
