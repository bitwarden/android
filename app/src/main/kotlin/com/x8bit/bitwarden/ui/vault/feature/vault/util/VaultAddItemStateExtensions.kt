@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.ui.platform.base.util.orNullIfBlank
import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.FieldView
import com.bitwarden.vault.IdentityView
import com.bitwarden.vault.LoginUriView
import com.bitwarden.vault.LoginView
import com.bitwarden.vault.PasswordHistoryView
import com.bitwarden.vault.SecureNoteType
import com.bitwarden.vault.SecureNoteView
import com.bitwarden.vault.SshKeyView
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.util.stringLongNameOrNull
import java.time.Clock
import java.time.Instant

/**
 * Transforms [VaultAddEditState.ViewState.Content] into [CipherView].
 */
fun VaultAddEditState.ViewState.Content.toCipherView(clock: Clock): CipherView =
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
        passwordHistory = toPasswordHistory(clock = clock),
        permissions = common.originalCipher?.permissions,
        creationDate = common.originalCipher?.creationDate ?: clock.instant(),
        deletedDate = common.originalCipher?.deletedDate,
        revisionDate = common.originalCipher?.revisionDate ?: clock.instant(),
        archivedDate = common.originalCipher?.archivedDate,

        // Type specific section
        type = type.toCipherType(),
        identity = type.toIdentityView(),
        secureNote = type.toSecureNotesView(),
        login = type.toLoginView(common = common, clock = clock),
        card = type.toCardView(),
        sshKey = type.toSshKeyView(),

        // Fields we always grab from the UI
        name = common.name,
        notes = common.notes.orNullIfBlank(),
        favorite = common.favorite,
        folderId = common.selectedFolderId,
        organizationId = common.selectedOwnerId,
        reprompt = common.toCipherRepromptType(),
        fields = common.customFieldData.map { it.toFieldView() },
    )

private fun VaultAddEditState.ViewState.Content.ItemType.toCipherType(): CipherType =
    when (this) {
        is VaultAddEditState.ViewState.Content.ItemType.Card -> CipherType.CARD
        is VaultAddEditState.ViewState.Content.ItemType.Identity -> CipherType.IDENTITY
        is VaultAddEditState.ViewState.Content.ItemType.Login -> CipherType.LOGIN
        is VaultAddEditState.ViewState.Content.ItemType.SecureNotes -> CipherType.SECURE_NOTE
        is VaultAddEditState.ViewState.Content.ItemType.SshKey -> CipherType.SSH_KEY
    }

private fun VaultAddEditState.ViewState.Content.ItemType.toSshKeyView(): SshKeyView? =
    (this as? VaultAddEditState.ViewState.Content.ItemType.SshKey)?.let {
        SshKeyView(
            publicKey = it.publicKey,
            privateKey = it.privateKey,
            fingerprint = it.fingerprint,
        )
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
                ?.stringLongNameOrNull,
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

@Suppress("MagicNumber")
private fun VaultAddEditState.ViewState.Content.toPasswordHistory(
    clock: Clock,
): List<PasswordHistoryView>? {
    val loginCipher = type as? VaultAddEditState.ViewState.Content.ItemType.Login
    val oldPassword = common.originalCipher?.login?.password
    val timestamp = clock.instant()

    val newPasswordHistory = getPasswordHistory(
        loginCipher = loginCipher,
        oldPassword = oldPassword,
        timestamp = timestamp,
    )

    val newHiddenFieldHistory = getHiddenFieldHistory(
        timestamp = timestamp,
        oldFields = common.originalCipher?.fields,
        newFields = common.customFieldData,
    )

    return listOf(
        common.originalCipher?.passwordHistory.orEmpty(),
        newPasswordHistory,
        newHiddenFieldHistory,
    )
        .flatten()
        .ifEmpty { null }
        ?.takeLast(5)
}

private fun getPasswordHistory(
    loginCipher: VaultAddEditState.ViewState.Content.ItemType.Login?,
    oldPassword: String?,
    timestamp: Instant,
): List<PasswordHistoryView> {
    return oldPassword
        .takeIf {
            loginCipher != null && !it.isNullOrEmpty() && it != loginCipher.password
        }
        ?.let {
            listOf(PasswordHistoryView(password = it, lastUsedDate = timestamp))
        }
        ?: emptyList()
}

private fun getHiddenFieldHistory(
    timestamp: Instant,
    oldFields: List<FieldView>?,
    newFields: List<VaultAddEditState.Custom>,
): List<PasswordHistoryView> {
    return oldFields
        ?.filter { oldField ->
            oldField.type == FieldType.HIDDEN &&
                !oldField.value.isNullOrEmpty() &&
                !oldField.name.isNullOrEmpty() &&
                newFields
                    .none { newField ->
                        newField is VaultAddEditState.Custom.HiddenField &&
                            newField.name == oldField.name &&
                            newField.value == oldField.value
                    }
        }
        ?.map { field ->
            PasswordHistoryView(
                password = "${field.name}: ${field.value}",
                lastUsedDate = timestamp,
            )
        }
        .orEmpty()
}

private fun VaultAddEditState.ViewState.Content.ItemType.toLoginView(
    common: VaultAddEditState.ViewState.Content.Common,
    clock: Clock,
): LoginView? =
    (this as? VaultAddEditState.ViewState.Content.ItemType.Login)?.let {
        LoginView(
            username = it.username.orNullIfBlank(),
            password = it.password.orNullIfBlank(),
            passwordRevisionDate = it.getRevisionDate(
                originalCipher = common.originalCipher,
                clock = clock,
            ),
            uris = it.uriList.toLoginUriView(),
            totp = it.totp,
            autofillOnPageLoad = common.originalCipher?.login?.autofillOnPageLoad,
            fido2Credentials = common.originalCipher?.login?.fido2Credentials
                .takeIf { _ -> it.fido2CredentialCreationDateTime != null },
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

private fun List<UriItem>?.toLoginUriView(): List<LoginUriView>? =
    this
        ?.filter { it.uri?.isNotBlank() == true }
        ?.map { LoginUriView(uri = it.uri.orEmpty(), match = it.match, uriChecksum = null) }
        .takeUnless { it.isNullOrEmpty() }

private fun VaultAddEditState.ViewState.Content.ItemType.Login.getRevisionDate(
    originalCipher: CipherView?,
    clock: Clock,
): Instant? {
    val isOriginalPasswordNull = originalCipher?.login?.password.isNullOrEmpty()
    val hasPasswordHistory = originalCipher?.passwordHistory?.any() ?: false
    val isOriginalAndNewPasswordEqual = originalCipher?.login?.password == this.password
    return if ((!isOriginalPasswordNull || hasPasswordHistory) && !isOriginalAndNewPasswordEqual) {
        clock.instant()
    } else {
        originalCipher?.login?.passwordRevisionDate
    }
}
