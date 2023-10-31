package com.x8bit.bitwarden.data.vault.datasource.sdk

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
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Create a mock [Cipher] with a given [number].
 */
fun createMockSdkCipher(number: Int): Cipher =
    Cipher(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        folderId = "mockFolderId-$number",
        collectionIds = listOf("mockCollectionId-$number"),
        name = "mockName-$number",
        notes = "mockNotes-$number",
        type = CipherType.LOGIN,
        login = createMockSdkLogin(number = number),
        creationDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
        deletedDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
        revisionDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
        attachments = listOf(createMockSdkAttachment(number = number)),
        card = createMockSdkCard(number = number),
        fields = listOf(createMockSdkField(number = number)),
        identity = createMockSdkIdentity(number = number),
        favorite = false,
        passwordHistory = listOf(createMockSdkPasswordHistory(number = number)),
        reprompt = CipherRepromptType.NONE,
        secureNote = createMockSdkSecureNote(),
        edit = false,
        organizationUseTotp = false,
        viewPassword = false,
        localData = null,
    )

/**
 * Create a mock [SecureNote] with a given [number].
 */
fun createMockSdkSecureNote(): SecureNote =
    SecureNote(
        type = SecureNoteType.GENERIC,
    )

/**
 * Create a mock [PasswordHistory] with a given [number].
 */
fun createMockSdkPasswordHistory(number: Int): PasswordHistory =
    PasswordHistory(
        password = "mockPassword-$number",
        lastUsedDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
    )

/**
 * Create a mock [Identity] with a given [number].
 */
fun createMockSdkIdentity(number: Int): Identity =
    Identity(
        firstName = "mockFirstName-$number",
        middleName = "mockMiddleName-$number",
        lastName = "mockLastName-$number",
        passportNumber = "mockPassportNumber-$number",
        country = "mockCountry-$number",
        address1 = "mockAddress1-$number",
        address2 = "mockAddress2-$number",
        address3 = "mockAddress3-$number",
        city = "mockCity-$number",
        postalCode = "mockPostalCode-$number",
        title = "mockTitle-$number",
        ssn = "mockSsn-$number",
        phone = "mockPhone-$number",
        company = "mockCompany-$number",
        licenseNumber = "mockLicenseNumber-$number",
        state = "mockState-$number",
        email = "mockEmail-$number",
        username = "mockUsername-$number",
    )

/**
 * Create a mock [Field] with a given [number].
 */
fun createMockSdkField(number: Int): Field =
    Field(
        linkedId = 100U,
        name = "mockName-$number",
        type = FieldType.HIDDEN,
        value = "mockValue-$number",
    )

/**
 * Create a mock [Card] with a given [number].
 */
fun createMockSdkCard(number: Int): Card =
    Card(
        number = "mockNumber-$number",
        expMonth = "mockExpMonth-$number",
        code = "mockCode-$number",
        expYear = "mockExpirationYear-$number",
        cardholderName = "mockCardholderName-$number",
        brand = "mockBrand-$number",
    )

/**
 * Create a mock [Attachment] with a given [number].
 */
fun createMockSdkAttachment(number: Int): Attachment =
    Attachment(
        fileName = "mockFileName-$number",
        size = "1",
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
        url = "mockUrl-$number",
        key = "mockKey-$number",
    )

/**
 * Create a mock [Login] with a given [number].
 */
fun createMockSdkLogin(number: Int): Login =
    Login(
        username = "mockUsername-$number",
        password = "mockPassword-$number",
        passwordRevisionDate = LocalDateTime
            .parse("2023-10-27T12:00:00")
            .toInstant(ZoneOffset.UTC),
        autofillOnPageLoad = false,
        uris = listOf(createMockSdkUri(number = number)),
        totp = "mockTotp-$number",
    )

/**
 * Create a mock [LoginUri] with a given [number].
 */
fun createMockSdkUri(number: Int): LoginUri =
    LoginUri(
        uri = "mockUri-$number",
        match = UriMatchType.HOST,
    )
