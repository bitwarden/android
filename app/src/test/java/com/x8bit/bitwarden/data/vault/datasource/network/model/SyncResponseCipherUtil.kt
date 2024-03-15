package com.x8bit.bitwarden.data.vault.datasource.network.model

import java.time.ZonedDateTime

/**
 * Create a mock [SyncResponseJson.Cipher] with a given [number].
 */
fun createMockCipher(number: Int, hasNullUri: Boolean = false): SyncResponseJson.Cipher =
    SyncResponseJson.Cipher(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        folderId = "mockFolderId-$number",
        collectionIds = listOf("mockCollectionId-$number"),
        name = "mockName-$number",
        notes = "mockNotes-$number",
        type = CipherTypeJson.LOGIN,
        login = createMockLogin(number = number, hasNullUri = hasNullUri),
        creationDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        deletedDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        attachments = listOf(createMockAttachment(number = number)),
        card = createMockCard(number = number),
        fields = listOf(createMockField(number = number)),
        identity = createMockIdentity(number = number),
        isFavorite = false,
        passwordHistory = listOf(createMockPasswordHistory(number = number)),
        reprompt = CipherRepromptTypeJson.NONE,
        secureNote = createMockSecureNote(),
        shouldEdit = false,
        shouldOrganizationUseTotp = false,
        shouldViewPassword = false,
        key = "mockKey-$number",
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Identity] with a given [number].
 */
fun createMockIdentity(number: Int): SyncResponseJson.Cipher.Identity =
    SyncResponseJson.Cipher.Identity(
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
 * Create a mock [SyncResponseJson.Cipher.Attachment] with a given [number].
 */
fun createMockAttachment(number: Int): SyncResponseJson.Cipher.Attachment =
    SyncResponseJson.Cipher.Attachment(
        fileName = "mockFileName-$number",
        size = 1,
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
        url = "mockUrl-$number",
        key = "mockKey-$number",
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Card] with a given [number].
 */
fun createMockCard(number: Int): SyncResponseJson.Cipher.Card =
    SyncResponseJson.Cipher.Card(
        number = "mockNumber-$number",
        expMonth = "mockExpMonth-$number",
        code = "mockCode-$number",
        expirationYear = "mockExpirationYear-$number",
        cardholderName = "mockCardholderName-$number",
        brand = "mockBrand-$number",
    )

/**
 * Create a mock [SyncResponseJson.Cipher.PasswordHistory] with a given [number].
 */
fun createMockPasswordHistory(number: Int): SyncResponseJson.Cipher.PasswordHistory =
    SyncResponseJson.Cipher.PasswordHistory(
        password = "mockPassword-$number",
        lastUsedDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
    )

/**
 * Create a mock [SyncResponseJson.Cipher.SecureNote].
 */
fun createMockSecureNote(): SyncResponseJson.Cipher.SecureNote =
    SyncResponseJson.Cipher.SecureNote(
        type = SecureNoteTypeJson.GENERIC,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Field] with a given [number].
 */
fun createMockField(number: Int): SyncResponseJson.Cipher.Field =
    SyncResponseJson.Cipher.Field(
        linkedIdType = LinkedIdTypeJson.LOGIN_USERNAME,
        name = "mockName-$number",
        type = FieldTypeJson.HIDDEN,
        value = "mockValue-$number",
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Login] with a given [number].
 */
fun createMockLogin(number: Int, hasNullUri: Boolean = false): SyncResponseJson.Cipher.Login =
    SyncResponseJson.Cipher.Login(
        username = "mockUsername-$number",
        password = "mockPassword-$number",
        passwordRevisionDate = ZonedDateTime.parse("2023-10-27T12:00:00Z"),
        shouldAutofillOnPageLoad = false,
        uri = if (hasNullUri) null else "mockUri-$number",
        uris = listOf(createMockUri(number = number)),
        totp = "mockTotp-$number",
        fido2Credentials = listOf(createMockFido2Credential(number)),
    )

fun createMockFido2Credential(number: Int) = SyncResponseJson.Cipher.Fido2Credential(
    credentialId = "mockCredentialId-$number",
    keyType = "mockKeyType-$number",
    keyAlgorithm = "mockKeyAlgorithm-$number",
    keyCurve = "mockKeyCurve-$number",
    keyValue = "mockKeyValue-$number",
    rpId = "mockRpId-$number",
    rpName = "mockRpName-$number",
    userHandle = "mockUserHandle-$number",
    userName = "mockUserName-$number",
    userDisplayName = "mockUserDisplayName-$number",
    counter = "mockCounter-$number",
    discoverable = "mockDiscoverable-$number",
    creationDate = ZonedDateTime.parse("2024-03-12T20:20:16.456Z"),
)

/**
 * Create a mock [SyncResponseJson.Cipher.Login.Uri] with a given [number].
 */
fun createMockUri(number: Int): SyncResponseJson.Cipher.Login.Uri =
    SyncResponseJson.Cipher.Login.Uri(
        uri = "mockUri-$number",
        uriMatchType = UriMatchTypeJson.HOST,
    )
