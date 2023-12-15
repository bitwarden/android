package com.x8bit.bitwarden.data.vault.datasource.sdk.model

import com.bitwarden.core.AttachmentView
import com.bitwarden.core.CardView
import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.FieldType
import com.bitwarden.core.FieldView
import com.bitwarden.core.IdentityView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.LoginView
import com.bitwarden.core.PasswordHistoryView
import com.bitwarden.core.SecureNoteType
import com.bitwarden.core.SecureNoteView
import com.bitwarden.core.UriMatchType
import java.time.ZonedDateTime

/**
 * Create a mock [CipherView].
 *
 * @param number the number to create the cipher with.
 * @param isDeleted whether or not the cipher has been deleted.
 * @param cipherType the type of cipher to create.
 */
fun createMockCipherView(
    number: Int,
    isDeleted: Boolean = true,
    cipherType: CipherType = CipherType.LOGIN,
): CipherView =
    CipherView(
        id = "mockId-$number",
        organizationId = "mockOrganizationId-$number",
        folderId = "mockId-$number",
        collectionIds = listOf("mockId-$number"),
        key = "mockKey-$number",
        name = "mockName-$number",
        notes = "mockNotes-$number",
        type = cipherType,
        login = createMockLoginView(number = number),
        creationDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
        deletedDate = if (isDeleted) {
            ZonedDateTime
                .parse("2023-10-27T12:00:00Z")
                .toInstant()
        } else {
            null
        },
        revisionDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
        attachments = listOf(createMockAttachmentView(number = number)),
        card = createMockCardView(number = number),
        fields = listOf(createMockFieldView(number = number)),
        identity = createMockIdentityView(number = number),
        favorite = false,
        passwordHistory = listOf(createMockPasswordHistoryView(number = number)),
        reprompt = CipherRepromptType.NONE,
        secureNote = createMockSecureNoteView(),
        edit = false,
        organizationUseTotp = false,
        viewPassword = false,
        localData = null,
    )

/**
 * Create a mock [LoginView] with a given [number].
 */
fun createMockLoginView(number: Int): LoginView =
    LoginView(
        username = "mockUsername-$number",
        password = "mockPassword-$number",
        passwordRevisionDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
        autofillOnPageLoad = false,
        uris = listOf(createMockUriView(number = number)),
        totp = "mockTotp-$number",
    )

/**
 * Create a mock [LoginUriView] with a given [number].
 */
fun createMockUriView(number: Int): LoginUriView =
    LoginUriView(
        uri = "mockUri-$number",
        match = UriMatchType.HOST,
    )

/**
 * Create a mock [AttachmentView] with a given [number].
 */
fun createMockAttachmentView(number: Int): AttachmentView =
    AttachmentView(
        fileName = "mockFileName-$number",
        size = "1",
        sizeName = "mockSizeName-$number",
        id = "mockId-$number",
        url = "mockUrl-$number",
        key = "mockKey-$number",
    )

/**
 * Create a mock [CardView] with a given [number].
 */
fun createMockCardView(number: Int): CardView =
    CardView(
        number = "mockNumber-$number",
        expMonth = "mockExpMonth-$number",
        code = "mockCode-$number",
        expYear = "mockExpirationYear-$number",
        cardholderName = "mockCardholderName-$number",
        brand = "mockBrand-$number",
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
 * Create a mock [PasswordHistoryView] with a given [number].
 */
fun createMockPasswordHistoryView(number: Int): PasswordHistoryView =
    PasswordHistoryView(
        password = "mockPassword-$number",
        lastUsedDate = ZonedDateTime
            .parse("2023-10-27T12:00:00Z")
            .toInstant(),
    )

/**
 * Create a mock [SecureNoteView].
 */
fun createMockSecureNoteView(): SecureNoteView =
    SecureNoteView(
        type = SecureNoteType.GENERIC,
    )
