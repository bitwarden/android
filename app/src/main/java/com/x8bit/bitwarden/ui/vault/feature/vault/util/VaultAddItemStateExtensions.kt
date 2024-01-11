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
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import java.time.Instant

/**
 * Transforms [VaultAddEditState.ViewState.Content] into [CipherView].
 */
fun VaultAddEditState.ViewState.Content.toCipherView(): CipherView =
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
        notes = common.notes.orNullIfBlank(),
        favorite = common.favorite,
        // TODO Use real folder ID (BIT-528)
        folderId = common.originalCipher?.folderId,
        // TODO Use real organization ID (BIT-780)
        organizationId = common.originalCipher?.organizationId,
        reprompt = common.toCipherRepromptType(),
        fields = common.customFieldData.map { it.toFieldView() },
    )

private fun VaultAddEditState.ViewState.Content.ItemType.toCipherType(): CipherType =
    when (this) {
        is VaultAddEditState.ViewState.Content.ItemType.Card -> CipherType.CARD
        is VaultAddEditState.ViewState.Content.ItemType.Identity -> CipherType.IDENTITY
        is VaultAddEditState.ViewState.Content.ItemType.Login -> CipherType.LOGIN
        is VaultAddEditState.ViewState.Content.ItemType.SecureNotes -> CipherType.SECURE_NOTE
    }

private fun VaultAddEditState.ViewState.Content.ItemType.toCardView(): CardView? =
    (this as? VaultAddEditState.ViewState.Content.ItemType.Card)?.let {
        CardView(
            cardholderName = it.cardHolderName.orNullIfBlank(),
            expMonth = it
                .expirationMonth
                .takeUnless { month ->
                    month == VaultCardExpirationMonth.SELECT
                }
                ?.number,
            expYear = it.expirationYear.orNullIfBlank(),
            code = it.securityCode.orNullIfBlank(),
            brand = it
                .brand
                .takeUnless { brand ->
                    brand == VaultCardBrand.SELECT
                }
                ?.name,
            number = it.number.orNullIfBlank(),
        )
    }

private fun VaultAddEditState.ViewState.Content.ItemType.toIdentityView(): IdentityView? =
    (this as? VaultAddEditState.ViewState.Content.ItemType.Identity)?.let {
        IdentityView(
            title = it
                .selectedTitle
                .takeUnless { title ->
                    title == VaultIdentityTitle.SELECT
                }
                ?.name,
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

private fun VaultAddEditState.ViewState.Content.ItemType.toLoginView(
    common: VaultAddEditState.ViewState.Content.Common,
): LoginView? =
    (this as? VaultAddEditState.ViewState.Content.ItemType.Login)?.let {
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
            totp = it.totp,
            autofillOnPageLoad = common.originalCipher?.login?.autofillOnPageLoad,
        )
    }

private fun VaultAddEditState.ViewState.Content.ItemType.toSecureNotesView(): SecureNoteView? =
    (this as? VaultAddEditState.ViewState.Content.ItemType.SecureNotes)?.let {
        SecureNoteView(type = SecureNoteType.GENERIC)
    }

private fun VaultAddEditState.ViewState.Content.Common.toCipherRepromptType(): CipherRepromptType =
    if (masterPasswordReprompt) {
        CipherRepromptType.PASSWORD
    } else {
        CipherRepromptType.NONE
    }

/**
 * Transforms [VaultAddItemState.Custom into [FieldView].
 */
private fun VaultAddEditState.Custom.toFieldView(): FieldView =
    when (val item = this) {
        is VaultAddEditState.Custom.BooleanField -> {
            FieldView(
                name = item.name,
                value = item.value.toString(),
                type = FieldType.BOOLEAN,
                linkedId = null,
            )
        }

        is VaultAddEditState.Custom.HiddenField -> {
            FieldView(
                name = item.name,
                value = item.value,
                type = FieldType.HIDDEN,
                linkedId = null,
            )
        }

        is VaultAddEditState.Custom.LinkedField -> {
            FieldView(
                name = item.name,
                value = null,
                type = FieldType.LINKED,
                linkedId = item.vaultLinkedFieldType?.id,
            )
        }

        is VaultAddEditState.Custom.TextField -> {
            FieldView(
                name = item.name,
                value = item.value,
                type = FieldType.TEXT,
                linkedId = null,
            )
        }
    }
