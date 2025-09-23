@file:Suppress("LongParameterList")

package com.bitwarden.network.model

import java.time.ZonedDateTime

/**
 * Constant date time used for [ZonedDateTime] properties of mock objects.
 */
private val MOCK_ZONED_DATE_TIME = ZonedDateTime.parse("2023-10-27T12:00:00Z")

/**
 * Create a mock [SyncResponseJson.Cipher] with a given [number].
 */
fun createMockCipher(
    number: Int,
    id: String = "mockId-$number",
    organizationId: String? = "mockOrganizationId-$number",
    folderId: String? = "mockFolderId-$number",
    collectionIds: List<String>? = listOf("mockCollectionId-$number"),
    name: String? = "mockName-$number",
    notes: String? = "mockNotes-$number",
    creationDate: ZonedDateTime = MOCK_ZONED_DATE_TIME,
    revisionDate: ZonedDateTime = MOCK_ZONED_DATE_TIME,
    deletedDate: ZonedDateTime? = MOCK_ZONED_DATE_TIME,
    archivedDate: ZonedDateTime? = MOCK_ZONED_DATE_TIME,
    attachments: List<SyncResponseJson.Cipher.Attachment>? = listOf(
        createMockAttachment(number = number),
    ),
    type: CipherTypeJson = CipherTypeJson.LOGIN,
    login: SyncResponseJson.Cipher.Login? = createMockLogin(number = number),
    card: SyncResponseJson.Cipher.Card? = createMockCard(number = number),
    identity: SyncResponseJson.Cipher.Identity? = createMockIdentity(number = number),
    sshKey: SyncResponseJson.Cipher.SshKey? = createMockSshKey(number = number),
    secureNote: SyncResponseJson.Cipher.SecureNote? = createMockSecureNote(),
    fields: List<SyncResponseJson.Cipher.Field>? = listOf(createMockField(number = number)),
    isFavorite: Boolean = false,
    passwordHistory: List<SyncResponseJson.Cipher.PasswordHistory>? = listOf(
        createMockPasswordHistory(number = number),
    ),
    permissions: SyncResponseJson.Cipher.CipherPermissions? = createMockCipherPermissions(),
    reprompt: CipherRepromptTypeJson = CipherRepromptTypeJson.NONE,
    shouldEdit: Boolean = false,
    shouldOrganizationUseTotp: Boolean = false,
    shouldViewPassword: Boolean = false,
    key: String? = "mockKey-$number",
    encryptedFor: String? = "mockEncryptedFor-$number",
): SyncResponseJson.Cipher =
    SyncResponseJson.Cipher(
        id = id,
        organizationId = organizationId,
        folderId = folderId,
        collectionIds = collectionIds,
        name = name,
        notes = notes,
        type = type,
        login = login,
        card = card,
        identity = identity,
        secureNote = secureNote,
        sshKey = sshKey,
        creationDate = creationDate,
        revisionDate = revisionDate,
        deletedDate = deletedDate,
        archivedDate = archivedDate,
        attachments = attachments,
        fields = fields,
        isFavorite = isFavorite,
        passwordHistory = passwordHistory,
        permissions = permissions,
        reprompt = reprompt,
        shouldEdit = shouldEdit,
        shouldOrganizationUseTotp = shouldOrganizationUseTotp,
        shouldViewPassword = shouldViewPassword,
        key = key,
        encryptedFor = encryptedFor,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Identity] with a given [number].
 */
fun createMockIdentity(
    number: Int,
    firstName: String? = "mockFirstName-$number",
    middleName: String? = "mockMiddleName-$number",
    lastName: String? = "mockLastName-$number",
    passportNumber: String? = "mockPassportNumber-$number",
    country: String? = "mockCountry-$number",
    address1: String? = "mockAddress1-$number",
    address2: String? = "mockAddress2-$number",
    address3: String? = "mockAddress3-$number",
    city: String? = "mockCity-$number",
    postalCode: String? = "mockPostalCode-$number",
    title: String? = "mockTitle-$number",
    ssn: String? = "mockSsn-$number",
    phone: String? = "mockPhone-$number",
    company: String? = "mockCompany-$number",
    licenseNumber: String? = "mockLicenseNumber-$number",
    state: String? = "mockState-$number",
    email: String? = "mockEmail-$number",
    username: String? = "mockUsername-$number",
): SyncResponseJson.Cipher.Identity =
    SyncResponseJson.Cipher.Identity(
        firstName = firstName,
        middleName = middleName,
        lastName = lastName,
        passportNumber = passportNumber,
        country = country,
        address1 = address1,
        address2 = address2,
        address3 = address3,
        city = city,
        postalCode = postalCode,
        title = title,
        ssn = ssn,
        phone = phone,
        company = company,
        licenseNumber = licenseNumber,
        state = state,
        email = email,
        username = username,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Attachment] with a given [number].
 */
fun createMockAttachment(
    number: Int,
    fileName: String? = "mockFileName-$number",
    size: Int = 1,
    sizeName: String? = "mockSizeName-$number",
    id: String? = "mockId-$number",
    url: String? = "mockUrl-$number",
    key: String? = "mockKey-$number",
): SyncResponseJson.Cipher.Attachment =
    SyncResponseJson.Cipher.Attachment(
        fileName = fileName,
        size = size,
        sizeName = sizeName,
        id = id,
        url = url,
        key = key,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Card] with a given [number].
 */
fun createMockCard(
    number: Int,
    cardNumber: String? = "mockNumber-$number",
    expMonth: String? = "mockExpMonth-$number",
    code: String? = "mockCode-$number",
    expirationYear: String? = "mockExpirationYear-$number",
    cardholderName: String? = "mockCardholderName-$number",
    brand: String? = "mockBrand-$number",
): SyncResponseJson.Cipher.Card =
    SyncResponseJson.Cipher.Card(
        number = cardNumber,
        expMonth = expMonth,
        code = code,
        expirationYear = expirationYear,
        cardholderName = cardholderName,
        brand = brand,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.PasswordHistory] with a given [number].
 */
fun createMockPasswordHistory(
    number: Int,
    password: String = "mockPassword-$number",
    lastUsedDate: ZonedDateTime = MOCK_ZONED_DATE_TIME,
): SyncResponseJson.Cipher.PasswordHistory =
    SyncResponseJson.Cipher.PasswordHistory(
        password = password,
        lastUsedDate = lastUsedDate,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.CipherPermissions].
 */
fun createMockCipherPermissions(
    delete: Boolean = true,
    restore: Boolean = true,
): SyncResponseJson.Cipher.CipherPermissions =
    SyncResponseJson.Cipher.CipherPermissions(
        delete = delete,
        restore = restore,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.SecureNote].
 */
fun createMockSecureNote(
    type: SecureNoteTypeJson = SecureNoteTypeJson.GENERIC,
): SyncResponseJson.Cipher.SecureNote =
    SyncResponseJson.Cipher.SecureNote(
        type = type,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Field] with a given [number].
 */
fun createMockField(
    number: Int,
    linkedIdType: LinkedIdTypeJson? = LinkedIdTypeJson.LOGIN_USERNAME,
    name: String? = "mockName-$number",
    type: FieldTypeJson = FieldTypeJson.HIDDEN,
    value: String? = "mockValue-$number",
): SyncResponseJson.Cipher.Field =
    SyncResponseJson.Cipher.Field(
        linkedIdType = linkedIdType,
        name = name,
        type = type,
        value = value,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Login] with a given [number].
 */
fun createMockLogin(
    number: Int,
    username: String? = "mockUsername-$number",
    password: String? = "mockPassword-$number",
    passwordRevisionDate: ZonedDateTime? = MOCK_ZONED_DATE_TIME,
    shouldAutofillOnPageLoad: Boolean? = false,
    uri: String? = "mockUri-$number",
    uris: List<SyncResponseJson.Cipher.Login.Uri> = listOf(createMockUri(number = number)),
    totp: String? = "mockTotp-$number",
    fido2Credentials: List<SyncResponseJson.Cipher.Fido2Credential> = listOf(
        createMockFido2Credential(number = number),
    ),
): SyncResponseJson.Cipher.Login =
    SyncResponseJson.Cipher.Login(
        username = username,
        password = password,
        passwordRevisionDate = passwordRevisionDate,
        shouldAutofillOnPageLoad = shouldAutofillOnPageLoad,
        uri = uri,
        uris = uris,
        totp = totp,
        fido2Credentials = fido2Credentials,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.SshKey] with a given [number].
 */
fun createMockSshKey(
    number: Int,
    publicKey: String = "mockPublicKey-$number",
    privateKey: String = "mockPrivateKey-$number",
    keyFingerprint: String = "mockKeyFingerprint-$number",
): SyncResponseJson.Cipher.SshKey =
    SyncResponseJson.Cipher.SshKey(
        publicKey = publicKey,
        privateKey = privateKey,
        keyFingerprint = keyFingerprint,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Fido2Credential] with a given [number].
 */
fun createMockFido2Credential(
    number: Int,
    credentialId: String = "mockCredentialId-$number",
    keyType: String = "mockKeyType-$number",
    keyAlgorithm: String = "mockKeyAlgorithm-$number",
    keyCurve: String = "mockKeyCurve-$number",
    keyValue: String = "mockKeyValue-$number",
    rpId: String = "mockRpId-$number",
    rpName: String? = "mockRpName-$number",
    userHandle: String? = "mockUserHandle-$number",
    userName: String? = "mockUserName-$number",
    userDisplayName: String? = "mockUserDisplayName-$number",
    counter: String = "mockCounter-$number",
    discoverable: String = "mockDiscoverable-$number",
    creationDate: ZonedDateTime = MOCK_ZONED_DATE_TIME,
): SyncResponseJson.Cipher.Fido2Credential =
    SyncResponseJson.Cipher.Fido2Credential(
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
        creationDate = creationDate,
    )

/**
 * Create a mock [SyncResponseJson.Cipher.Login.Uri] with a given [number].
 */
fun createMockUri(
    number: Int,
    uri: String? = "mockUri-$number",
    uriMatchType: UriMatchTypeJson? = UriMatchTypeJson.HOST,
    uriChecksum: String? = "mockUriChecksum-$number",
): SyncResponseJson.Cipher.Login.Uri =
    SyncResponseJson.Cipher.Login.Uri(
        uri = uri,
        uriMatchType = uriMatchType,
        uriChecksum = uriChecksum,
    )
