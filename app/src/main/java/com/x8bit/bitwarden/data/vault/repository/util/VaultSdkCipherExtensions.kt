@file:Suppress("TooManyFunctions")
package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.core.Attachment
import com.bitwarden.core.Card
import com.bitwarden.core.Cipher
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.Field
import com.bitwarden.core.FieldType
import com.bitwarden.core.Identity
import com.bitwarden.core.Login
import com.bitwarden.core.LoginUri
import com.bitwarden.core.PasswordHistory
import com.bitwarden.core.SecureNote
import com.bitwarden.core.SecureNoteType
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherRepromptTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.FieldTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SecureNoteTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UriMatchTypeJson
import java.time.ZoneOffset

/**
 * Converts a list of [SyncResponseJson.Cipher] objects to a list of corresponding
 * Bitwarden SDK [Cipher] objects.
 */
fun List<SyncResponseJson.Cipher>.toEncryptedSdkCipherList(): List<Cipher> =
    map { it.toEncryptedSdkCipher() }

/**
 * Converts a of [SyncResponseJson.Cipher] object to a corresponding
 * Bitwarden SDK [Cipher] object.
 */
fun SyncResponseJson.Cipher.toEncryptedSdkCipher(): Cipher =
    Cipher(
        id = id,
        organizationId = organizationId,
        folderId = folderId,
        collectionIds = collectionIds.orEmpty(),
        key = key,
        name = name.orEmpty(),
        notes = notes,
        type = type.toSdkCipherType(),
        login = login?.toSdkLogin(),
        identity = identity?.toSdkIdentity(),
        card = card?.toSdkCard(),
        secureNote = secureNote?.toSdkSecureNote(),
        favorite = isFavorite,
        reprompt = reprompt.toSdkRepromptType(),
        organizationUseTotp = shouldOrganizationUseTotp,
        edit = shouldEdit,
        viewPassword = shouldViewPassword,
        localData = null,
        attachments = attachments?.toSdkAttachmentList(),
        fields = fields?.toSdkFieldList(),
        passwordHistory = passwordHistory?.toSdkPasswordHistoryList(),
        creationDate = creationDate.toInstant(ZoneOffset.UTC),
        deletedDate = deletedDate?.toInstant(ZoneOffset.UTC),
        revisionDate = revisionDate.toInstant(ZoneOffset.UTC),
    )

/**
 * Transforms a [SyncResponseJson.Cipher.Login] into the corresponding Bitwarden SDK [Login].
 */
fun SyncResponseJson.Cipher.Login.toSdkLogin(): Login =
    Login(
        username = username,
        password = password,
        passwordRevisionDate = passwordRevisionDate?.toInstant(ZoneOffset.UTC),
        uris = uris?.toSdkLoginUriList(),
        totp = totp,
        autofillOnPageLoad = shouldAutofillOnPageLoad,
    )

/**
 * Transforms a [SyncResponseJson.Cipher.Identity] into the corresponding Bitwarden SDK [Identity].
 */
fun SyncResponseJson.Cipher.Identity.toSdkIdentity(): Identity =
    Identity(
        title = title,
        middleName = middleName,
        firstName = firstName,
        lastName = lastName,
        address1 = address1,
        address2 = address2,
        address3 = address3,
        city = city,
        state = state,
        postalCode = postalCode,
        country = country,
        company = company,
        email = email,
        phone = phone,
        ssn = ssn,
        username = username,
        passportNumber = passportNumber,
        licenseNumber = licenseNumber,
    )

/**
 * Transforms a [SyncResponseJson.Cipher.Card] into the corresponding Bitwarden SDK [Card].
 */
fun SyncResponseJson.Cipher.Card.toSdkCard(): Card =
    Card(
        cardholderName = cardholderName,
        expMonth = expMonth,
        expYear = expirationYear,
        code = code,
        brand = brand,
        number = number,
    )

/**
 * Transforms a [SyncResponseJson.Cipher.SecureNote] into
 * the corresponding Bitwarden SDK [SecureNote].
 */
fun SyncResponseJson.Cipher.SecureNote.toSdkSecureNote(): SecureNote =
    SecureNote(
        type = when (type) {
            SecureNoteTypeJson.GENERIC -> SecureNoteType.GENERIC
        },
    )

/**
 * Transforms a list of [SyncResponseJson.Cipher.Login.Uri] into
 * a corresponding list of  Bitwarden SDK [LoginUri].
 */
fun List<SyncResponseJson.Cipher.Login.Uri>.toSdkLoginUriList(): List<LoginUri> =
    map { it.toSdkLoginUri() }

/**
 * Transforms a [SyncResponseJson.Cipher.Login.Uri] into
 * a corresponding Bitwarden SDK [LoginUri].
 */
fun SyncResponseJson.Cipher.Login.Uri.toSdkLoginUri(): LoginUri =
    LoginUri(
        uri = uri,
        match = uriMatchType?.toSdkMatchType(),
    )

/**
 * Transforms a list of [SyncResponseJson.Cipher.Attachment] into
 * a corresponding list of  Bitwarden SDK [Attachment].
 */
fun List<SyncResponseJson.Cipher.Attachment>.toSdkAttachmentList(): List<Attachment> =
    map { it.toSdkAttachment() }

/**
 * Transforms a [SyncResponseJson.Cipher.Attachment] into
 * a corresponding Bitwarden SDK [Attachment].
 */
fun SyncResponseJson.Cipher.Attachment.toSdkAttachment(): Attachment =
    Attachment(
        id = id,
        url = url,
        size = size.toString(),
        sizeName = sizeName,
        fileName = fileName,
        key = key,
    )

/**
 * Transforms a list of [SyncResponseJson.Cipher.Field] into
 * a corresponding list of  Bitwarden SDK [Field].
 */
fun List<SyncResponseJson.Cipher.Field>.toSdkFieldList(): List<Field> =
    map { it.toSdkField() }

/**
 * Transforms a [SyncResponseJson.Cipher.Field] into
 * a corresponding Bitwarden SDK [Field].
 */
fun SyncResponseJson.Cipher.Field.toSdkField(): Field =
    Field(
        name = name,
        value = value,
        type = type.toSdkFieldType(),
        linkedId = linkedIdType?.value,
    )

/**
 * Transforms a list of [SyncResponseJson.Cipher.PasswordHistory] into
 * a corresponding list of  Bitwarden SDK [PasswordHistory].
 */
@Suppress("MaxLineLength")
fun List<SyncResponseJson.Cipher.PasswordHistory>.toSdkPasswordHistoryList(): List<PasswordHistory> =
    map { it.toSdkPasswordHistory() }

/**
 * Transforms a [SyncResponseJson.Cipher.PasswordHistory] into
 * a corresponding Bitwarden SDK [PasswordHistory].
 */
fun SyncResponseJson.Cipher.PasswordHistory.toSdkPasswordHistory(): PasswordHistory =
    PasswordHistory(
        password = password,
        lastUsedDate = lastUsedDate.toInstant(ZoneOffset.UTC),
    )

/**
 * Transforms a [CipherTypeJson] to the corresponding Bitwarden SDK [CipherType].
 */
fun CipherTypeJson.toSdkCipherType(): CipherType =
    when (this) {
        CipherTypeJson.LOGIN -> CipherType.LOGIN
        CipherTypeJson.SECURE_NOTE -> CipherType.SECURE_NOTE
        CipherTypeJson.CARD -> CipherType.CARD
        CipherTypeJson.IDENTITY -> CipherType.IDENTITY
    }

/**
 * Transforms a [UriMatchTypeJson] to the corresponding Bitwarden SDK [UriMatchType].
 */
fun UriMatchTypeJson.toSdkMatchType(): UriMatchType =
    when (this) {
        UriMatchTypeJson.DOMAIN -> UriMatchType.DOMAIN
        UriMatchTypeJson.HOST -> UriMatchType.HOST
        UriMatchTypeJson.STARTS_WITH -> UriMatchType.STARTS_WITH
        UriMatchTypeJson.EXACT -> UriMatchType.EXACT
        UriMatchTypeJson.REGULAR_EXPRESSION -> UriMatchType.REGULAR_EXPRESSION
        UriMatchTypeJson.NEVER -> UriMatchType.NEVER
    }

/**
 * Transforms a [CipherRepromptTypeJson] to the corresponding Bitwarden SDK [CipherRepromptType].
 */
fun CipherRepromptTypeJson.toSdkRepromptType(): CipherRepromptType =
    when (this) {
        CipherRepromptTypeJson.NONE -> CipherRepromptType.NONE
        CipherRepromptTypeJson.PASSWORD -> CipherRepromptType.PASSWORD
    }

/**
 * Transforms a [FieldTypeJson] to the corresponding Bitwarden SDK [FieldType].
 */
fun FieldTypeJson.toSdkFieldType(): FieldType =
    when (this) {
        FieldTypeJson.TEXT -> FieldType.TEXT
        FieldTypeJson.HIDDEN -> FieldType.HIDDEN
        FieldTypeJson.BOOLEAN -> FieldType.BOOLEAN
        FieldTypeJson.LINKED -> FieldType.LINKED
    }
