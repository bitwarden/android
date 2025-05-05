package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.fido.Fido2CredentialAutofillView
import com.bitwarden.vault.AttachmentView
import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.Fido2Credential
import com.bitwarden.vault.FieldType
import com.bitwarden.vault.FieldView
import com.bitwarden.vault.IdentityView
import com.bitwarden.vault.LoginUriView
import com.bitwarden.vault.LoginView
import com.bitwarden.vault.PasswordHistoryView
import com.bitwarden.vault.SecureNoteType
import com.bitwarden.vault.SecureNoteView
import com.bitwarden.vault.SshKeyView
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
 * Create a mock [CipherView].
 *
 * @param number the number to create the cipher with.
 * @param isDeleted whether or not the cipher has been deleted.
 * @param cipherType the type of cipher to create.
 */
@Suppress("LongParameterList")
fun createMockCipherView(
    number: Int,
    isDeleted: Boolean = false,
    cipherType: CipherType = CipherType.LOGIN,
    repromptType: CipherRepromptType = CipherRepromptType.NONE,
    totp: String? = "mockTotp-$number",
    organizationId: String? = "mockOrganizationId-$number",
    folderId: String? = "mockId-$number",
    clock: Clock = FIXED_CLOCK,
    fido2Credentials: List<Fido2Credential>? = null,
    sshKey: SshKeyView? = createMockSshKeyView(number = number),
    login: LoginView? = createMockLoginView(
        number = number,
        totp = totp,
        clock = clock,
        fido2Credentials = fido2Credentials,
    ),
): CipherView =
    CipherView(
        id = "mockId-$number",
        organizationId = organizationId,
        folderId = folderId,
        collectionIds = listOf("mockId-$number"),
        key = "mockKey-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        type = cipherType,
        login = login.takeIf { cipherType == CipherType.LOGIN },
        creationDate = clock.instant(),
        deletedDate = if (isDeleted) {
            clock.instant()
        } else {
            null
        },
        revisionDate = clock.instant(),
        attachments = listOf(createMockAttachmentView(number = number)),
        card = createMockCardView(number = number).takeIf { cipherType == CipherType.CARD },
        fields = listOf(createMockFieldView(number = number)),
        identity = createMockIdentityView(number = number).takeIf {
            cipherType == CipherType.IDENTITY
        },
        sshKey = sshKey.takeIf { cipherType == CipherType.SSH_KEY },
        favorite = false,
        passwordHistory = listOf(createMockPasswordHistoryView(number = number, clock)),
        reprompt = repromptType,
        secureNote = createMockSecureNoteView().takeIf { cipherType == CipherType.SECURE_NOTE },
        edit = true,
        organizationUseTotp = false,
        viewPassword = true,
        localData = null,
    )

/**
 * Create a mock [LoginView] with a given [number].
 */
fun createMockLoginView(
    number: Int,
    totp: String? = "mockTotp-$number",
    clock: Clock = FIXED_CLOCK,
    hasUris: Boolean = true,
    fido2Credentials: List<Fido2Credential>? = createMockSdkFido2CredentialList(number, clock),
): LoginView =
    LoginView(
        username = "mockUsername-$number",
        password = "mockPassword-$number",
        passwordRevisionDate = clock.instant(),
        autofillOnPageLoad = false,
        uris = listOf(createMockUriView(number = number)).takeIf { hasUris },
        totp = totp,
        fido2Credentials = fido2Credentials,
    )

/**
 * Create a list of mock [Fido2Credential] with a given [number].
 */
fun createMockSdkFido2CredentialList(
    number: Int,
    clock: Clock = FIXED_CLOCK,
): List<Fido2Credential> = listOf(createMockSdkFido2Credential(number = number, clock = clock))

/**
 * Create a mock [Fido2Credential] with a given [number].
 */
fun createMockSdkFido2Credential(
    number: Int,
    rpId: String = "mockRpId-$number",
    clock: Clock = FIXED_CLOCK,
): Fido2Credential = Fido2Credential(
    credentialId = "mockCredentialId-$number",
    keyType = "mockKeyType-$number",
    keyAlgorithm = "mockKeyAlgorithm-$number",
    keyCurve = "mockKeyCurve-$number",
    keyValue = "mockKeyValue-$number",
    rpId = rpId,
    userHandle = "mockUserHandle-$number",
    userName = "mockUserName-$number",
    counter = "mockCounter-$number",
    rpName = "mockRpName-$number",
    userDisplayName = "mockUserDisplayName-$number",
    discoverable = "mockDiscoverable-$number",
    creationDate = clock.instant(),
)

/**
 * Create a mock [Fido2CredentialAutofillView] with a given [number] and optional [cipherId].
 */
fun createMockFido2CredentialAutofillView(
    number: Int,
    cipherId: String? = null,
    rpId: String = "mockRpId-$number",
): Fido2CredentialAutofillView =
    Fido2CredentialAutofillView(
        credentialId = "mockCredentialId-$number".encodeToByteArray(),
        cipherId = cipherId ?: "mockCipherId-$number",
        rpId = rpId,
        userNameForUi = "mockUserNameForUi-$number",
        userHandle = "mockUserHandle-$number".encodeToByteArray(),
    )

/**
 * Create a mock [LoginUriView] with a given [number].
 */
fun createMockUriView(number: Int): LoginUriView =
    LoginUriView(
        uri = "www.mockuri$number.com",
        match = UriMatchType.HOST,
        uriChecksum = "mockUriChecksum-$number",
    )

/**
 * Create a mock [AttachmentView] with a given [number].
 */
fun createMockAttachmentView(number: Int, key: String? = "mockKey-$number"): AttachmentView =
    AttachmentView(
        fileName = "mockFileName-$number",
        size = "1",
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
        url = "mockUrl-$number",
        key = key,
    )

/**
 * Create a mock [CardView] with a given [number].
 */
fun createMockCardView(number: Int, brand: String = "mockBrand-$number"): CardView =
    CardView(
        number = "mockNumber-$number",
        expMonth = "mockExpMonth-$number",
        code = "mockCode-$number",
        expYear = "mockExpirationYear-$number",
        cardholderName = "mockCardholderName-$number",
        brand = brand,
    )

/**
 * Create a mock [FieldView] with a given [number].
 */
fun createMockFieldView(number: Int): FieldView =
    FieldView(
        linkedId = 100U,
        name = "mockName-$number",
        type = FieldType.HIDDEN,
        value = "mockValue-$number",
    )

/**
 * Create a mock [IdentityView] with a given [number].
 */
fun createMockIdentityView(number: Int): IdentityView =
    IdentityView(
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
 * Create a mock [SshKeyView] with a given [number].
 */
fun createMockSshKeyView(number: Int): SshKeyView =
    SshKeyView(
        publicKey = "mockPublicKey-$number",
        privateKey = "mockPrivateKey-$number",
        fingerprint = "mockKeyFingerprint-$number",
    )

/**
 * Create a mock [PasswordHistoryView] with a given [number].
 */
fun createMockPasswordHistoryView(number: Int, clock: Clock = FIXED_CLOCK): PasswordHistoryView =
    PasswordHistoryView(
        password = "mockPassword-$number",
        lastUsedDate = clock.instant(),
    )

/**
 * Create a mock [SecureNoteView].
 */
fun createMockSecureNoteView(): SecureNoteView =
    SecureNoteView(
        type = SecureNoteType.GENERIC,
    )
