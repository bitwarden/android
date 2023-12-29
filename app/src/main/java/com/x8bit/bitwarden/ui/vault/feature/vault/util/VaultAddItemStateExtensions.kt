package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.CardView
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.bitwarden.core.IdentityView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.LoginView
import com.bitwarden.core.SecureNoteType
import com.bitwarden.core.SecureNoteView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.ui.platform.base.util.orNullIfBlank
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState
import java.time.Instant

/**
 * Transforms a [VaultAddItemState.ViewState.ItemType] into [CipherView].
 */
fun VaultAddItemState.ViewState.Content.toCipherView(): CipherView =
    CipherView(
        // Pulled from original cipher when editing, otherwise uses defaults
        id = common.originalCipher?.id,
        collectionIds = common.originalCipher?.collectionIds.orEmpty(),
        key = common.originalCipher?.key,
        edit = common.originalCipher?.edit ?: true,
        viewPassword = common.originalCipher?.viewPassword ?: true,
        localData = common.originalCipher?.localData,
        attachments = common.originalCipher?.attachments,
        organizationUseTotp = common.originalCipher?.organizationUseTotp ?: false,
        passwordHistory = common.originalCipher?.passwordHistory,
        creationDate = common.originalCipher?.creationDate ?: Instant.now(),
        deletedDate = common.originalCipher?.deletedDate,
        revisionDate = common.originalCipher?.revisionDate ?: Instant.now(),

        // Type specific section
        type = type.toCipherType(),
        identity = type.toIdentityView(),
        secureNote = type.toSecureNotesView(),
        login = type.toLoginView(common = common),
        card = type.toCardView(),

        // Fields we always grab from the UI
        name = common.name,
        notes = common.notes,
        favorite = common.favorite,
        // TODO Use real folder ID (BIT-528)
        folderId = common.originalCipher?.folderId,
        // TODO Use real organization ID (BIT-780)
        organizationId = common.originalCipher?.organizationId,
        reprompt = common.toCipherRepromptType(),
        fields = common.customFieldData.map { it.toFieldView() },
    )

private fun VaultAddItemState.ViewState.Content.ItemType.toCipherType(): CipherType =
    when (this) {
        is VaultAddItemState.ViewState.Content.ItemType.Card -> CipherType.CARD
        is VaultAddItemState.ViewState.Content.ItemType.Identity -> CipherType.IDENTITY
        is VaultAddItemState.ViewState.Content.ItemType.Login -> CipherType.LOGIN
        is VaultAddItemState.ViewState.Content.ItemType.SecureNotes -> CipherType.SECURE_NOTE
    }

private fun VaultAddItemState.ViewState.Content.ItemType.toCardView(): CardView? =
    (this as? VaultAddItemState.ViewState.Content.ItemType.Card)?.let {
        // TODO Create real CardView from Content (BIT-668)
        CardView(
            cardholderName = null,
            expMonth = null,
            expYear = null,
            code = null,
            brand = null,
            number = null,
        )
    }

private fun VaultAddItemState.ViewState.Content.ItemType.toIdentityView(): IdentityView? =
    (this as? VaultAddItemState.ViewState.Content.ItemType.Identity)?.let {
        IdentityView(
            title = it.selectedTitle.name,
            firstName = it.firstName.orNullIfBlank(),
            lastName = it.lastName.orNullIfBlank(),
            middleName = it.middleName.orNullIfBlank(),
            address1 = it.address1.orNullIfBlank(),
            address2 = it.address2.orNullIfBlank(),
            address3 = it.address3.orNullIfBlank(),
            city = it.city.orNullIfBlank(),
            state = it.state.orNullIfBlank(),
            postalCode = it.zip.orNullIfBlank(),
            country = it.country.orNullIfBlank(),
            company = it.company.orNullIfBlank(),
            email = it.email.orNullIfBlank(),
            phone = it.phone.orNullIfBlank(),
            ssn = it.ssn.orNullIfBlank(),
            username = it.username.orNullIfBlank(),
            passportNumber = it.passportNumber.orNullIfBlank(),
            licenseNumber = it.licenseNumber.orNullIfBlank(),
        )
    }

private fun VaultAddItemState.ViewState.Content.ItemType.toLoginView(
    common: VaultAddItemState.ViewState.Content.Common,
): LoginView? =
    (this as? VaultAddItemState.ViewState.Content.ItemType.Login)?.let {
        LoginView(
            username = it.username,
            password = it.password,
            passwordRevisionDate = common.originalCipher?.login?.passwordRevisionDate,
            uris = listOf(
                // TODO Implement URI list (BIT-1094)
                LoginUriView(
                    uri = it.uri,
                    // TODO Implement URI settings in (BIT-1094)
                    match = UriMatchType.DOMAIN,
                ),
            ),
            // TODO Implement TOTP (BIT-1066)
            totp = common.originalCipher?.login?.totp,
            autofillOnPageLoad = common.originalCipher?.login?.autofillOnPageLoad,
        )
    }

private fun VaultAddItemState.ViewState.Content.ItemType.toSecureNotesView(): SecureNoteView? =
    (this as? VaultAddItemState.ViewState.Content.ItemType.SecureNotes)?.let {
        SecureNoteView(type = SecureNoteType.GENERIC)
    }

private fun VaultAddItemState.ViewState.Content.Common.toCipherRepromptType(): CipherRepromptType =
    if (masterPasswordReprompt) {
        CipherRepromptType.PASSWORD
    } else {
        CipherRepromptType.NONE
    }

/**
 * Transforms [VaultAddItemState.Custom into [FieldView].
 */
private fun VaultAddItemState.Custom.toFieldView(): FieldView =
    when (val item = this) {
        is VaultAddItemState.Custom.BooleanField -> {
            FieldView(
                name = item.name,
                value = item.value.toString(),
                type = FieldType.BOOLEAN,
                linkedId = null,
            )
        }

        is VaultAddItemState.Custom.HiddenField -> {
            FieldView(
                name = item.name,
                value = item.value,
                type = FieldType.HIDDEN,
                linkedId = null,
            )
        }

        is VaultAddItemState.Custom.LinkedField -> {
            FieldView(
                name = item.name,
                value = null,
                type = FieldType.LINKED,
                linkedId = item.vaultLinkedFieldType.id,
            )
        }

        is VaultAddItemState.Custom.TextField -> {
            FieldView(
                name = item.name,
                value = item.value,
                type = FieldType.TEXT,
                linkedId = null,
            )
        }
    }
