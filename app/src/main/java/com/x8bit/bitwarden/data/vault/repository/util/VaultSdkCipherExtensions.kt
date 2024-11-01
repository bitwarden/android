@file:Suppress("TooManyFunctions")

package com.x8bit.bitwarden.data.vault.repository.util

import com.bitwarden.vault.Attachment
import com.bitwarden.vault.Card
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.Fido2Credential
import com.bitwarden.vault.Field
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.Identity
import com.bitwarden.vault.Login
import com.bitwarden.vault.LoginUri
import com.bitwarden.vault.PasswordHistory
import com.bitwarden.vault.SecureNote
import com.bitwarden.vault.SecureNoteType
import com.bitwarden.vault.SshKey
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.platform.util.SpecialCharWithPrecedenceComparator
import com.x8bit.bitwarden.data.vault.datasource.network.model.AttachmentJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherJsonRequest
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherRepromptTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.CipherTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.FieldTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.LinkedIdTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SecureNoteTypeJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.SyncResponseJson
import com.x8bit.bitwarden.data.vault.datasource.network.model.UriMatchTypeJson
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Converts a Bitwarden SDK [Cipher] object to a corresponding
 * [SyncResponseJson.Cipher] object.
 */
fun Cipher.toEncryptedNetworkCipher(): CipherJsonRequest =
    CipherJsonRequest(
        notes = notes,
        attachments = attachments
            ?.filter { it.id != null }
            ?.associate { requireNotNull(it.id) to it.toNetworkAttachmentRequest() },
        reprompt = reprompt.toNetworkRepromptType(),
        passwordHistory = passwordHistory?.toEncryptedNetworkPasswordHistoryList(),
        lastKnownRevisionDate = ZonedDateTime.ofInstant(revisionDate, ZoneOffset.UTC),
        type = type.toNetworkCipherType(),
        login = login?.toEncryptedNetworkLogin(),
        secureNote = secureNote?.toEncryptedNetworkSecureNote(),
        folderId = folderId,
        organizationId = organizationId,
        identity = identity?.toEncryptedNetworkIdentity(),
        name = name,
        fields = fields?.toEncryptedNetworkFieldList(),
        isFavorite = favorite,
        card = card?.toEncryptedNetworkCard(),
        key = key,
        sshKey = sshKey?.toEncryptedNetworkSshKey(),
    )

/**
 * Converts a Bitwarden SDK [Cipher] object to a corresponding
 * [SyncResponseJson.Cipher] object.
 */
fun Cipher.toEncryptedNetworkCipherResponse(): SyncResponseJson.Cipher =
    SyncResponseJson.Cipher(
        notes = notes,
        reprompt = reprompt.toNetworkRepromptType(),
        passwordHistory = passwordHistory?.toEncryptedNetworkPasswordHistoryList(),
        type = type.toNetworkCipherType(),
        login = login?.toEncryptedNetworkLogin(),
        secureNote = secureNote?.toEncryptedNetworkSecureNote(),
        folderId = folderId,
        organizationId = organizationId,
        identity = identity?.toEncryptedNetworkIdentity(),
        name = name,
        fields = fields?.toEncryptedNetworkFieldList(),
        isFavorite = favorite,
        card = card?.toEncryptedNetworkCard(),
        attachments = attachments?.toNetworkAttachmentList(),
        sshKey = sshKey?.toEncryptedNetworkSshKey(),
        shouldOrganizationUseTotp = organizationUseTotp,
        shouldEdit = edit,
        revisionDate = ZonedDateTime.ofInstant(revisionDate, ZoneOffset.UTC),
        creationDate = ZonedDateTime.ofInstant(creationDate, ZoneOffset.UTC),
        deletedDate = deletedDate?.let { ZonedDateTime.ofInstant(it, ZoneOffset.UTC) },
        collectionIds = collectionIds,
        id = id.orEmpty(),
        shouldViewPassword = viewPassword,
        key = key,
    )

/**
 * Converts a Bitwarden SDK [Card] object to a corresponding
 * [SyncResponseJson.Cipher.Card] object.
 */
private fun Card.toEncryptedNetworkCard(): SyncResponseJson.Cipher.Card =
    SyncResponseJson.Cipher.Card(
        number = number,
        expMonth = expMonth,
        code = code,
        expirationYear = expYear,
        cardholderName = cardholderName,
        brand = brand,
    )

private fun SshKey.toEncryptedNetworkSshKey(): SyncResponseJson.Cipher.SshKey =
    SyncResponseJson.Cipher.SshKey(
        publicKey = publicKey,
        privateKey = privateKey,
        keyFingerprint = fingerprint,
    )

/**
 * Converts a list of Bitwarden SDK [Field] objects to a corresponding
 * list of [SyncResponseJson.Cipher.Field] objects.
 */
private fun List<Field>.toEncryptedNetworkFieldList(): List<SyncResponseJson.Cipher.Field> =
    this.map { it.toEncryptedNetworkField() }

/**
 * Converts a Bitwarden SDK [Field] object to a corresponding
 * [SyncResponseJson.Cipher.Field] object.
 */
private fun Field.toEncryptedNetworkField(): SyncResponseJson.Cipher.Field =
    SyncResponseJson.Cipher.Field(
        linkedIdType = linkedId?.toNetworkLinkedIdType(),
        name = name,
        type = type.toNetworkFieldType(),
        value = value,
    )

private fun UInt.toNetworkLinkedIdType(): LinkedIdTypeJson =
    LinkedIdTypeJson.entries.first { this == it.value }

/**
 * Converts a Bitwarden SDK [FieldType] object to a corresponding
 * [FieldTypeJson] object.
 */
private fun FieldType.toNetworkFieldType(): FieldTypeJson =
    when (this) {
        FieldType.TEXT -> FieldTypeJson.TEXT
        FieldType.HIDDEN -> FieldTypeJson.HIDDEN
        FieldType.BOOLEAN -> FieldTypeJson.BOOLEAN
        FieldType.LINKED -> FieldTypeJson.LINKED
    }

/**
 * Converts a Bitwarden SDK [Identity] object to a corresponding
 * [SyncResponseJson.Cipher.Identity] object.
 */
private fun Identity.toEncryptedNetworkIdentity(): SyncResponseJson.Cipher.Identity =
    SyncResponseJson.Cipher.Identity(
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
 * Converts a Bitwarden SDK [SecureNote] object to a corresponding
 * [SyncResponseJson.Cipher.SecureNote] object.
 */
private fun SecureNote.toEncryptedNetworkSecureNote(): SyncResponseJson.Cipher.SecureNote =
    SyncResponseJson.Cipher.SecureNote(
        type = when (type) {
            SecureNoteType.GENERIC -> SecureNoteTypeJson.GENERIC
        },
    )

/**
 * Converts a list of Bitwarden SDK [LoginUri] objects to a corresponding
 * list of [SyncResponseJson.Cipher.Login.Uri] objects.
 */
private fun List<LoginUri>.toEncryptedNetworkUriList(): List<SyncResponseJson.Cipher.Login.Uri> =
    this.map { it.toEncryptedNetworkUri() }

/**
 * Converts a Bitwarden SDK [LoginUri] object to a corresponding
 * [SyncResponseJson.Cipher.Login.Uri] object.
 */
private fun LoginUri.toEncryptedNetworkUri(): SyncResponseJson.Cipher.Login.Uri =
    SyncResponseJson.Cipher.Login.Uri(
        uriMatchType = match?.toNetworkMatchType(),
        uri = uri,
        uriChecksum = uriChecksum,
    )

private fun UriMatchType.toNetworkMatchType(): UriMatchTypeJson =
    when (this) {
        UriMatchType.DOMAIN -> UriMatchTypeJson.DOMAIN
        UriMatchType.HOST -> UriMatchTypeJson.HOST
        UriMatchType.STARTS_WITH -> UriMatchTypeJson.STARTS_WITH
        UriMatchType.EXACT -> UriMatchTypeJson.EXACT
        UriMatchType.REGULAR_EXPRESSION -> UriMatchTypeJson.REGULAR_EXPRESSION
        UriMatchType.NEVER -> UriMatchTypeJson.NEVER
    }

/**
 * Converts a list of Bitwarden SDK [Attachment] objects to a corresponding
 * [SyncResponseJson.Cipher.Attachment] list.
 */
private fun List<Attachment>.toNetworkAttachmentList(): List<SyncResponseJson.Cipher.Attachment> =
    map { it.toNetworkAttachment() }

/**
 * Converts a Bitwarden SDK [Attachment] object to a corresponding
 * [SyncResponseJson.Cipher.Attachment] object.
 */
private fun Attachment.toNetworkAttachment(): SyncResponseJson.Cipher.Attachment =
    SyncResponseJson.Cipher.Attachment(
        fileName = fileName,
        size = size?.toInt() ?: 0,
        sizeName = sizeName,
        id = id,
        url = url,
        key = key,
    )

/**
 * Converts a Bitwarden SDK [Attachment] object to a corresponding [AttachmentJsonRequest] object.
 */
fun Attachment.toNetworkAttachmentRequest(): AttachmentJsonRequest =
    AttachmentJsonRequest(
        fileName = fileName,
        fileSize = size,
        key = key,
    )

/**
 * Converts a Bitwarden SDK [Login] object to a corresponding
 * [SyncResponseJson.Cipher.Login] object.
 */
private fun Login.toEncryptedNetworkLogin(): SyncResponseJson.Cipher.Login =
    SyncResponseJson.Cipher.Login(
        uris = uris?.toEncryptedNetworkUriList(),
        totp = totp,
        password = password,
        passwordRevisionDate = passwordRevisionDate?.let {
            ZonedDateTime.ofInstant(it, ZoneOffset.UTC)
        },
        shouldAutofillOnPageLoad = autofillOnPageLoad,
        // uri needs to be null to avoid duplicating the first url entry for a login item.
        uri = null,
        username = username,
        fido2Credentials = fido2Credentials?.toNetworkFido2Credentials(),
    )

private fun List<Fido2Credential>.toNetworkFido2Credentials() =
    this.map { it.toNetworkFido2Credential() }

private fun Fido2Credential.toNetworkFido2Credential() = SyncResponseJson.Cipher.Fido2Credential(
    credentialId = credentialId,
    keyType = keyType,
    keyAlgorithm = keyAlgorithm,
    keyCurve = keyCurve,
    keyValue = keyValue,
    rpId = rpId,
    rpName = rpName,
    userHandle = userHandle,
    userName = userName,
    userDisplayName = userDisplayName,
    counter = counter,
    discoverable = discoverable,
    creationDate = ZonedDateTime.ofInstant(creationDate, ZoneOffset.UTC),
)

/**
 * Converts a list of Bitwarden SDK [PasswordHistory] objects to a corresponding
 * list of [SyncResponseJson.Cipher.PasswordHistory] objects.
 */
@Suppress("MaxLineLength")
private fun List<PasswordHistory>.toEncryptedNetworkPasswordHistoryList(): List<SyncResponseJson.Cipher.PasswordHistory> =
    this.map { it.toEncryptedNetworkPasswordHistory() }

/**
 * Converts a Bitwarden SDK [PasswordHistory] object to a corresponding
 * [SyncResponseJson.Cipher.PasswordHistory] object.
 */
@Suppress("MaxLineLength")
private fun PasswordHistory.toEncryptedNetworkPasswordHistory(): SyncResponseJson.Cipher.PasswordHistory =
    SyncResponseJson.Cipher.PasswordHistory(
        password = password,
        lastUsedDate = ZonedDateTime.ofInstant(lastUsedDate, ZoneOffset.UTC),
    )

/**
 * Converts a Bitwarden SDK [CipherRepromptType] object to a corresponding
 * [CipherRepromptTypeJson] object.
 */
private fun CipherRepromptType.toNetworkRepromptType(): CipherRepromptTypeJson =
    when (this) {
        CipherRepromptType.NONE -> CipherRepromptTypeJson.NONE
        CipherRepromptType.PASSWORD -> CipherRepromptTypeJson.PASSWORD
    }

/**
 * Converts a Bitwarden SDK [CipherType] object to a corresponding
 * [CipherTypeJson] object.
 */
private fun CipherType.toNetworkCipherType(): CipherTypeJson =
    when (this) {
        CipherType.LOGIN -> CipherTypeJson.LOGIN
        CipherType.SECURE_NOTE -> CipherTypeJson.SECURE_NOTE
        CipherType.CARD -> CipherTypeJson.CARD
        CipherType.IDENTITY -> CipherTypeJson.IDENTITY
        CipherType.SSH_KEY -> CipherTypeJson.SSH_KEY
    }

/**
 * Converts a list of [SyncResponseJson.Cipher] objects to a list of corresponding
 * Bitwarden SDK [Cipher] objects.
 */
fun List<SyncResponseJson.Cipher>.toEncryptedSdkCipherList(): List<Cipher> =
    map { it.toEncryptedSdkCipher() }

/**
 * Converts a [SyncResponseJson.Cipher] object to a corresponding
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
        sshKey = sshKey?.toSdkSshKey(),
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
        creationDate = creationDate.toInstant(),
        deletedDate = deletedDate?.toInstant(),
        revisionDate = revisionDate.toInstant(),
    )

/**
 * Transforms a [SyncResponseJson.Cipher.Login] into the corresponding Bitwarden SDK [Login].
 */
fun SyncResponseJson.Cipher.Login.toSdkLogin(): Login =
    Login(
        username = username,
        password = password,
        passwordRevisionDate = passwordRevisionDate?.toInstant(),
        uris = uris?.toSdkLoginUriList(),
        totp = totp,
        autofillOnPageLoad = shouldAutofillOnPageLoad,
        fido2Credentials = fido2Credentials?.toSdkFido2Credentials(),
    )

private fun List<SyncResponseJson.Cipher.Fido2Credential>.toSdkFido2Credentials() =
    this.map { it.toSdkFido2Credential() }

private fun SyncResponseJson.Cipher.Fido2Credential.toSdkFido2Credential() = Fido2Credential(
    credentialId = credentialId,
    keyType = keyType,
    keyAlgorithm = keyAlgorithm,
    keyCurve = keyCurve,
    keyValue = keyValue,
    rpId = rpId,
    rpName = rpName,
    userHandle = userHandle,
    userName = userName,
    userDisplayName = userDisplayName,
    counter = counter,
    discoverable = discoverable,
    creationDate = creationDate.toInstant(),
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
 * Transforms a [SyncResponseJson.Cipher.SshKey] into
 * the corresponding Bitwarden SDK [SshKey].
 */
fun SyncResponseJson.Cipher.SshKey.toSdkSshKey(): SshKey =
    SshKey(
        publicKey = publicKey,
        privateKey = privateKey,
        fingerprint = keyFingerprint,
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
        uriChecksum = uriChecksum,
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
        lastUsedDate = lastUsedDate.toInstant(),
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
        CipherTypeJson.SSH_KEY -> CipherType.SSH_KEY
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

/**
 * Sorts the data in alphabetical order by name. Using lexicographical sorting but giving
 * precedence to special characters over letters and digits.
 */
@JvmName("toAlphabeticallySortedCipherList")
fun List<CipherView>.sortAlphabetically(): List<CipherView> {
    return this.sortedWith(
        comparator = { cipher1, cipher2 ->
            SpecialCharWithPrecedenceComparator.compare(cipher1.name, cipher2.name)
        },
    )
}
