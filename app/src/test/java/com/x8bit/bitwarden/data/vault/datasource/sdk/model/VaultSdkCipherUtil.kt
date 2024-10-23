package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.vault.Attachment
import com.bitwarden.vault.Card
import com.bitwarden.vault.Cipher
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
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
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

/**
 * Default date time used for [ZonedDateTime] properties of mock objects.
 */
private const val DEFAULT_TIMESTAMP = "2023-10-27T12:00:00Z"
private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse(DEFAULT_TIMESTAMP),
    ZoneOffset.UTC,
)

/**
 * Create a mock [Cipher] with a given [number].
 */
fun createMockSdkCipher(number: Int, clock: Clock = FIXED_CLOCK): Cipher =
    Cipher(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        folderId = "mockFolderId-$number",
        collectionIds = listOf("mockCollectionId-$number"),
        key = "mockKey-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        type = CipherType.LOGIN,
        login = createMockSdkLogin(number = number, clock = clock),
        creationDate = clock.instant(),
        deletedDate = clock.instant(),
        revisionDate = clock.instant(),
        attachments = listOf(createMockSdkAttachment(number = number)),
        card = createMockSdkCard(number = number),
        fields = listOf(createMockSdkField(number = number)),
        identity = createMockSdkIdentity(number = number),
        sshKey = createMockSdkSshKey(number = number),
        favorite = false,
        passwordHistory = listOf(createMockSdkPasswordHistory(number = number, clock = clock)),
        reprompt = CipherRepromptType.NONE,
        secureNote = createMockSdkSecureNote(),
        edit = false,
        organizationUseTotp = false,
        viewPassword = false,
        localData = null,
    )

/**
 * Create a mock [SecureNote].
 */
fun createMockSdkSecureNote(): SecureNote =
    SecureNote(
        type = SecureNoteType.GENERIC,
    )

/**
 * Create a mock [PasswordHistory] with a given [number].
 */
fun createMockSdkPasswordHistory(number: Int, clock: Clock): PasswordHistory =
    PasswordHistory(
        password = "mockPassword-$number",
        lastUsedDate = clock.instant(),
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
 * Create a mock [SshKey] with a given [number].
 */
fun createMockSdkSshKey(number: Int): SshKey =
    SshKey(
        publicKey = "mockPublicKey-$number",
        privateKey = "mockPrivateKey-$number",
        fingerprint = "mockKeyFingerprint-$number",
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
fun createMockSdkAttachment(number: Int, key: String? = "mockKey-$number"): Attachment =
    Attachment(
        fileName = "mockFileName-$number",
        size = "1",
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
        url = "mockUrl-$number",
        key = key,
    )

/**
 * Create a mock [Login] with a given [number].
 */
fun createMockSdkLogin(number: Int, clock: Clock): Login =
    Login(
        username = "mockUsername-$number",
        password = "mockPassword-$number",
        passwordRevisionDate = clock.instant(),
        autofillOnPageLoad = false,
        uris = listOf(createMockSdkUri(number = number)),
        totp = "mockTotp-$number",
        fido2Credentials = createMockSdkFido2CredentialList(number, clock),
    )

/**
 * Create a mock [LoginUri] with a given [number].
 */
fun createMockSdkUri(number: Int): LoginUri =
    LoginUri(
        uri = "mockUri-$number",
        match = UriMatchType.HOST,
        uriChecksum = "mockUriChecksum-$number",
    )
