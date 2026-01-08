package com.x8bit.bitwarden.ui.vault.feature.vault.util

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
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCardExpirationMonth
import com.x8bit.bitwarden.ui.vault.model.VaultIdentityTitle
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class VaultAddItemStateExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `toCipherView should transform Login ItemType to CipherView`() {
        val loginItemType = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                name = "mockName-1",
                selectedFolderId = "mockFolderId-1",
                favorite = false,
                masterPasswordReprompt = false,
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(
                username = "mockUsername-1",
                password = "mockPassword-1",
                uriList = listOf(
                    UriItem(
                        id = "testId",
                        uri = "mockUri-1",
                        match = UriMatchType.DOMAIN,
                        checksum = null,
                    ),
                ),
                totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                fido2CredentialCreationDateTime = null,
            ),
        )

        val result = loginItemType.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            CipherView(
                id = null,
                organizationId = "mockOwnerId-1",
                folderId = "mockFolderId-1",
                collectionIds = emptyList(),
                key = null,
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.LOGIN,
                login = LoginView(
                    username = "mockUsername-1",
                    password = "mockPassword-1",
                    passwordRevisionDate = null,
                    uris = listOf(
                        LoginUriView(
                            uri = "mockUri-1",
                            match = UriMatchType.DOMAIN,
                            uriChecksum = null,
                        ),
                    ),
                    totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                    autofillOnPageLoad = null,
                    fido2Credentials = null,
                ),
                identity = null,
                card = null,
                secureNote = null,
                favorite = false,
                reprompt = CipherRepromptType.NONE,
                organizationUseTotp = false,
                edit = true,
                viewPassword = true,
                localData = null,
                attachments = null,
                fields = emptyList(),
                passwordHistory = null,
                permissions = null,
                creationDate = FIXED_CLOCK.instant(),
                deletedDate = null,
                revisionDate = FIXED_CLOCK.instant(),
                archivedDate = null,
                sshKey = null,
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toCipherView should transform Login ItemType to CipherView with original cipher`() {
        val cipherView = DEFAULT_LOGIN_CIPHER_VIEW
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                selectedFolderId = "mockFolderId-1",
                favorite = true,
                masterPasswordReprompt = false,
                customFieldData = listOf(
                    VaultAddEditState.Custom.BooleanField("testId", "TestBoolean", false),
                    VaultAddEditState.Custom.TextField("testId", "TestText", "TestText"),
                    VaultAddEditState.Custom.HiddenField("testId", "TestHidden", "TestHidden"),
                    VaultAddEditState.Custom.LinkedField(
                        "testId",
                        "TestLinked",
                        VaultLinkedFieldType.USERNAME,
                    ),
                ),
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(
                username = "mockUsername-1",
                password = "mockPassword-1",
                uriList = listOf(
                    UriItem(id = "TestId", uri = "mockUri-1", match = null, checksum = null),
                ),
                totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
            ),
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            @Suppress("MaxLineLength")
            cipherView.copy(
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.LOGIN,
                folderId = "mockFolderId-1",
                organizationId = "mockOwnerId-1",
                login = LoginView(
                    username = "mockUsername-1",
                    password = "mockPassword-1",
                    passwordRevisionDate = FIXED_CLOCK.instant(),
                    uris = listOf(
                        LoginUriView(
                            uri = "mockUri-1",
                            match = null,
                            uriChecksum = null,
                        ),
                    ),
                    totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                    autofillOnPageLoad = false,
                    fido2Credentials = null,
                ),
                favorite = true,
                reprompt = CipherRepromptType.NONE,
                fields = listOf(
                    FieldView(
                        name = "TestBoolean",
                        value = "false",
                        type = FieldType.BOOLEAN,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestText",
                        value = "TestText",
                        type = FieldType.TEXT,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestHidden",
                        value = "TestHidden",
                        type = FieldType.HIDDEN,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestLinked",
                        value = null,
                        type = FieldType.LINKED,
                        linkedId = VaultLinkedFieldType.USERNAME.id,
                    ),
                ),
                passwordHistory = listOf(
                    PasswordHistoryView(
                        password = "old_password",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                    PasswordHistoryView(
                        password = "password",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                    PasswordHistoryView(
                        password = "hidden: value",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform SecureNotes ItemType to CipherView`() {
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                name = "mockName-1",
                selectedFolderId = "mockId-1",
                favorite = false,
                masterPasswordReprompt = false,
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnership-1",
                customFieldData = listOf(
                    VaultAddEditState.Custom.BooleanField("testId", "TestBoolean", false),
                    VaultAddEditState.Custom.TextField("testId", "TestText", "TestText"),
                    VaultAddEditState.Custom.HiddenField("testId", "TestHidden", "TestHidden"),
                ),
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            CipherView(
                id = null,
                organizationId = "mockOwnership-1",
                folderId = "mockId-1",
                collectionIds = emptyList(),
                key = null,
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.SECURE_NOTE,
                login = null,
                identity = null,
                card = null,
                secureNote = SecureNoteView(SecureNoteType.GENERIC),
                favorite = false,
                reprompt = CipherRepromptType.NONE,
                organizationUseTotp = false,
                edit = true,
                viewPassword = true,
                localData = null,
                attachments = null,
                fields = listOf(
                    FieldView(
                        name = "TestBoolean",
                        value = "false",
                        type = FieldType.BOOLEAN,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestText",
                        value = "TestText",
                        type = FieldType.TEXT,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestHidden",
                        value = "TestHidden",
                        type = FieldType.HIDDEN,
                        linkedId = null,
                    ),
                ),
                passwordHistory = null,
                permissions = null,
                creationDate = FIXED_CLOCK.instant(),
                deletedDate = null,
                revisionDate = FIXED_CLOCK.instant(),
                archivedDate = null,
                sshKey = null,
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform SecureNotes ItemType to CipherView with original cipher`() {
        val cipherView = DEFAULT_SECURE_NOTES_CIPHER_VIEW.copy(passwordHistory = null)
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                selectedFolderId = "mockId-1",
                favorite = false,
                masterPasswordReprompt = true,
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
                customFieldData = emptyList(),
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            cipherView.copy(
                name = "mockName-1",
                notes = "mockNotes-1",
                organizationId = "mockOwnerId-1",
                folderId = "mockId-1",
                type = CipherType.SECURE_NOTE,
                secureNote = SecureNoteView(SecureNoteType.GENERIC),
                reprompt = CipherRepromptType.PASSWORD,
                fields = emptyList(),
                passwordHistory = listOf(
                    PasswordHistoryView(
                        password = "hidden: value",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform Identity ItemType to CipherView`() {
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                name = "mockName-1",
                selectedFolderId = "mockId-1",
                favorite = false,
                masterPasswordReprompt = false,
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Identity(
                selectedTitle = VaultIdentityTitle.MR,
                firstName = "mockFirstName",
                lastName = "mockLastName",
                middleName = "mockMiddleName",
                address1 = "mockAddress1",
                address2 = "mockAddress2",
                address3 = "mockAddress3",
                city = "mockCity",
                state = "mockState",
                zip = "mockPostalCode",
                country = "mockCountry",
                company = "mockCompany",
                email = "mockEmail",
                phone = "mockPhone",
                ssn = "mockSsn",
                username = "MockUsername",
                passportNumber = "mockPassportNumber",
                licenseNumber = "mockLicenseNumber",
            ),
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            CipherView(
                id = null,
                organizationId = "mockOwnerId-1",
                folderId = "mockId-1",
                collectionIds = emptyList(),
                key = null,
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.IDENTITY,
                login = null,
                identity = IdentityView(
                    title = "MR",
                    firstName = "mockFirstName",
                    lastName = "mockLastName",
                    middleName = "mockMiddleName",
                    address1 = "mockAddress1",
                    address2 = "mockAddress2",
                    address3 = "mockAddress3",
                    city = "mockCity",
                    state = "mockState",
                    postalCode = "mockPostalCode",
                    country = "mockCountry",
                    company = "mockCompany",
                    email = "mockEmail",
                    phone = "mockPhone",
                    ssn = "mockSsn",
                    username = "MockUsername",
                    passportNumber = "mockPassportNumber",
                    licenseNumber = "mockLicenseNumber",
                ),
                card = null,
                secureNote = null,
                favorite = false,
                reprompt = CipherRepromptType.NONE,
                organizationUseTotp = false,
                edit = true,
                viewPassword = true,
                localData = null,
                attachments = null,
                fields = emptyList(),
                passwordHistory = null,
                permissions = null,
                creationDate = FIXED_CLOCK.instant(),
                deletedDate = null,
                revisionDate = FIXED_CLOCK.instant(),
                archivedDate = null,
                sshKey = null,
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform Identity ItemType to CipherView with original cipher`() {
        val cipherView = DEFAULT_IDENTITY_CIPHER_VIEW
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                selectedFolderId = "mockId-1",
                favorite = true,
                masterPasswordReprompt = false,
                customFieldData = listOf(
                    VaultAddEditState.Custom.BooleanField("testId", "TestBoolean", false),
                    VaultAddEditState.Custom.TextField("testId", "TestText", "TestText"),
                    VaultAddEditState.Custom.HiddenField("testId", "TestHidden", "TestHidden"),
                    VaultAddEditState.Custom.LinkedField(
                        "testId",
                        "TestLinked",
                        VaultLinkedFieldType.USERNAME,
                    ),
                ),
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Identity(
                selectedTitle = VaultIdentityTitle.MR,
                firstName = "mockFirstName",
                lastName = "mockLastName",
                middleName = "mockMiddleName",
                address1 = "mockAddress1",
                address2 = "mockAddress2",
                address3 = "mockAddress3",
                city = "mockCity",
                state = "mockState",
                zip = "mockPostalCode",
                country = "mockCountry",
                company = "mockCompany",
                email = "mockEmail",
                phone = "mockPhone",
                ssn = "mockSsn",
                username = "MockUsername",
                passportNumber = "mockPassportNumber",
                licenseNumber = "mockLicenseNumber",
            ),
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            @Suppress("MaxLineLength")
            cipherView.copy(
                name = "mockName-1",
                notes = "mockNotes-1",
                organizationId = "mockOwnerId-1",
                folderId = "mockId-1",
                type = CipherType.IDENTITY,
                identity = IdentityView(
                    title = "MR",
                    firstName = "mockFirstName",
                    lastName = "mockLastName",
                    middleName = "mockMiddleName",
                    address1 = "mockAddress1",
                    address2 = "mockAddress2",
                    address3 = "mockAddress3",
                    city = "mockCity",
                    state = "mockState",
                    postalCode = "mockPostalCode",
                    country = "mockCountry",
                    company = "mockCompany",
                    email = "mockEmail",
                    phone = "mockPhone",
                    ssn = "mockSsn",
                    username = "MockUsername",
                    passportNumber = "mockPassportNumber",
                    licenseNumber = "mockLicenseNumber",
                ),
                favorite = true,
                reprompt = CipherRepromptType.NONE,
                fields = listOf(
                    FieldView(
                        name = "TestBoolean",
                        value = "false",
                        type = FieldType.BOOLEAN,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestText",
                        value = "TestText",
                        type = FieldType.TEXT,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestHidden",
                        value = "TestHidden",
                        type = FieldType.HIDDEN,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestLinked",
                        value = null,
                        type = FieldType.LINKED,
                        linkedId = VaultLinkedFieldType.USERNAME.id,
                    ),
                ),
                passwordHistory = listOf(
                    PasswordHistoryView(
                        password = "old_password",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                    PasswordHistoryView(
                        password = "hidden: value",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform Card ItemType to CipherView`() {
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                name = "mockName-1",
                selectedFolderId = "mockId-1",
                favorite = false,
                masterPasswordReprompt = false,
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Card(
                cardHolderName = "mockName-1",
                number = "1234567",
                brand = VaultCardBrand.VISA,
                expirationMonth = VaultCardExpirationMonth.MARCH,
                expirationYear = "2028",
                securityCode = "987",
            ),
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            CipherView(
                id = null,
                organizationId = "mockOwnerId-1",
                folderId = "mockId-1",
                collectionIds = emptyList(),
                key = null,
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.CARD,
                login = null,
                identity = null,
                card = CardView(
                    cardholderName = "mockName-1",
                    expMonth = "3",
                    expYear = "2028",
                    code = "987",
                    brand = "Visa",
                    number = "1234567",
                ),
                secureNote = null,
                favorite = false,
                reprompt = CipherRepromptType.NONE,
                organizationUseTotp = false,
                edit = true,
                viewPassword = true,
                localData = null,
                attachments = null,
                fields = emptyList(),
                passwordHistory = null,
                permissions = null,
                creationDate = FIXED_CLOCK.instant(),
                deletedDate = null,
                revisionDate = FIXED_CLOCK.instant(),
                archivedDate = null,
                sshKey = null,
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform Card ItemType to CipherView with original cipher`() {
        val cipherView = DEFAULT_CARD_CIPHER_VIEW
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                selectedFolderId = "mockId-1",
                favorite = true,
                masterPasswordReprompt = false,
                customFieldData = listOf(
                    VaultAddEditState.Custom.BooleanField("testId", "TestBoolean", false),
                    VaultAddEditState.Custom.TextField("testId", "TestText", "TestText"),
                    VaultAddEditState.Custom.HiddenField("testId", "TestHidden", "TestHidden"),
                    VaultAddEditState.Custom.LinkedField(
                        "testId",
                        "TestLinked",
                        VaultLinkedFieldType.USERNAME,
                    ),
                ),
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Card(
                cardHolderName = "mockName-1",
                number = "1234567",
                brand = VaultCardBrand.VISA,
                expirationMonth = VaultCardExpirationMonth.MARCH,
                expirationYear = "2028",
                securityCode = "987",
            ),
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            cipherView.copy(
                name = "mockName-1",
                notes = "mockNotes-1",
                organizationId = "mockOwnerId-1",
                folderId = "mockId-1",
                favorite = true,
                reprompt = CipherRepromptType.NONE,
                fields = listOf(
                    FieldView(
                        name = "TestBoolean",
                        value = "false",
                        type = FieldType.BOOLEAN,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestText",
                        value = "TestText",
                        type = FieldType.TEXT,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestHidden",
                        value = "TestHidden",
                        type = FieldType.HIDDEN,
                        linkedId = null,
                    ),
                    FieldView(
                        name = "TestLinked",
                        value = null,
                        type = FieldType.LINKED,
                        linkedId = VaultLinkedFieldType.USERNAME.id,
                    ),
                ),
                passwordHistory = listOf(
                    PasswordHistoryView(
                        password = "old_password",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                    PasswordHistoryView(
                        password = "hidden: value",
                        lastUsedDate = FIXED_CLOCK.instant(),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform SSH Key ItemType to CipherView`() {
        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                name = "mockName-1",
                selectedFolderId = "mockId-1",
                favorite = false,
                masterPasswordReprompt = false,
                notes = "mockNotes-1",
                selectedOwnerId = "mockOwnerId-1",
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.SshKey(
                publicKey = "mockPublicKey-1",
                privateKey = "mockPrivateKey-1",
                fingerprint = "mockFingerprint-1",
            ),
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            CipherView(
                id = null,
                organizationId = "mockOwnerId-1",
                folderId = "mockId-1",
                collectionIds = emptyList(),
                key = null,
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.SSH_KEY,
                login = null,
                identity = null,
                card = null,
                secureNote = null,
                favorite = false,
                reprompt = CipherRepromptType.NONE,
                organizationUseTotp = false,
                edit = true,
                viewPassword = true,
                localData = null,
                attachments = null,
                fields = emptyList(),
                passwordHistory = null,
                permissions = null,
                creationDate = FIXED_CLOCK.instant(),
                deletedDate = null,
                revisionDate = FIXED_CLOCK.instant(),
                archivedDate = null,
                sshKey = SshKeyView(
                    publicKey = "mockPublicKey-1",
                    privateKey = "mockPrivateKey-1",
                    fingerprint = "mockFingerprint-1",
                ),
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginView should transform Login ItemType to LoginView deleting fido2Credentials with original cipher`() {
        val cipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
            type = CipherType.LOGIN,
            notes = null,
            fields = emptyList(),
            login = LoginView(
                username = "mockUsername-1",
                password = "mockPassword-1",
                passwordRevisionDate = FIXED_CLOCK.instant(),
                uris = null,
                totp = null,
                autofillOnPageLoad = false,
                fido2Credentials = createMockSdkFido2CredentialList(1),
            ),
        )

        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                customFieldData = emptyList(),
                masterPasswordReprompt = true,
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(
                username = "mockUsername-1",
                password = "mockPassword-1",
                totp = null,
                fido2CredentialCreationDateTime = null,
            ),
        )

        val result = viewState.toCipherView(clock = FIXED_CLOCK)

        assertEquals(
            cipherView.copy(
                name = "mockName-1",
                login = LoginView(
                    username = "mockUsername-1",
                    password = "mockPassword-1",
                    totp = null,
                    fido2Credentials = null,
                    uris = null,
                    passwordRevisionDate = FIXED_CLOCK.instant(),
                    autofillOnPageLoad = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toLoginView should update revision date when password differs`() {
        val cipherView = DEFAULT_LOGIN_CIPHER_VIEW

        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                favorite = true,
                masterPasswordReprompt = false,
                customFieldData = emptyList(),
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(
                username = "mockUsername-1",
                password = "mockPassword-1",
            ),
        )
        val futureClock = Clock.fixed(
            Instant.parse("2023-11-27T12:00:00Z"),
            ZoneOffset.UTC,
        )

        // We need to pass in a future clock to make sure that when the
        // revision date is updated it is updated to a new time
        val result = viewState.toCipherView(clock = futureClock)

        assertNotEquals(
            viewState.common.originalCipher?.login?.passwordRevisionDate,
            result.login?.passwordRevisionDate,
        )
    }

    @Test
    fun `toLoginView should keep revision date when password is equal`() {
        val cipherView = DEFAULT_LOGIN_CIPHER_VIEW

        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                favorite = true,
                masterPasswordReprompt = false,
                customFieldData = emptyList(),
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(
                username = "mockUsername-1",
                password = cipherView.login?.password ?: "",
            ),
        )
        val futureClock = Clock.fixed(
            Instant.parse("2023-11-27T12:00:00Z"),
            ZoneOffset.UTC,
        )

        // We need to pass in a future clock to make sure that if the
        // revision date were to be updated it would be updated to a new time
        val result = viewState.toCipherView(clock = futureClock)

        assertEquals(
            viewState.common.originalCipher?.login?.passwordRevisionDate,
            result.login?.passwordRevisionDate,
        )
    }

    @Test
    fun `toLoginView should not update revision date when password is null and has no history`() {
        val cipherView = DEFAULT_LOGIN_CIPHER_VIEW.copy(
            passwordHistory = null,
            login = DEFAULT_LOGIN_CIPHER_VIEW.login?.copy(password = null),
        )

        val viewState = VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "mockName-1",
                favorite = true,
                masterPasswordReprompt = false,
                customFieldData = emptyList(),
            ),
            isIndividualVaultDisabled = false,
            type = VaultAddEditState.ViewState.Content.ItemType.Login(
                username = "mockUsername-1",
                password = "updated password",
            ),
        )
        val futureClock = Clock.fixed(
            Instant.parse("2023-11-27T12:00:00Z"),
            ZoneOffset.UTC,
        )

        // We need to pass in a future clock to make sure that if the
        // revision date were to be updated it would be updated to a new time
        val result = viewState.toCipherView(clock = futureClock)

        assertEquals(
            viewState.common.originalCipher?.login?.passwordRevisionDate,
            result.login?.passwordRevisionDate,
        )
    }
}

private val FIXED_CLOCK: Clock = Clock.fixed(
    Instant.parse("2023-10-27T12:00:00Z"),
    ZoneOffset.UTC,
)

private val DEFAULT_BASE_CIPHER_VIEW: CipherView = CipherView(
    id = "id1234",
    organizationId = null,
    folderId = null,
    collectionIds = emptyList(),
    key = null,
    name = "cipher",
    notes = "Lots of notes",
    type = CipherType.LOGIN,
    login = null,
    identity = null,
    card = null,
    secureNote = null,
    favorite = false,
    reprompt = CipherRepromptType.PASSWORD,
    organizationUseTotp = false,
    edit = false,
    viewPassword = false,
    localData = null,
    attachments = null,
    fields = listOf(
        FieldView(
            name = "text",
            value = "value",
            type = FieldType.TEXT,
            linkedId = null,
        ),
        FieldView(
            name = "hidden",
            value = "value",
            type = FieldType.HIDDEN,
            linkedId = null,
        ),
        FieldView(
            name = "boolean",
            value = "true",
            type = FieldType.BOOLEAN,
            linkedId = null,
        ),
        FieldView(
            name = "linked username",
            value = null,
            type = FieldType.LINKED,
            linkedId = 100U,
        ),
        FieldView(
            name = "linked password",
            value = null,
            type = FieldType.LINKED,
            linkedId = 101U,
        ),
    ),
    passwordHistory = listOf(
        PasswordHistoryView(
            password = "old_password",
            lastUsedDate = FIXED_CLOCK.instant(),
        ),
    ),
    permissions = null,
    creationDate = FIXED_CLOCK.instant(),
    deletedDate = null,
    revisionDate = FIXED_CLOCK.instant(),
    archivedDate = null,
    sshKey = null,
)

private val DEFAULT_LOGIN_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.LOGIN,
    login = LoginView(
        username = "username",
        password = "password",
        passwordRevisionDate = FIXED_CLOCK.instant(),
        uris = listOf(
            LoginUriView(
                uri = "www.example.com",
                match = null,
                uriChecksum = null,
            ),
        ),
        totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
        autofillOnPageLoad = false,
        fido2Credentials = null,
    ),
)

private val DEFAULT_SECURE_NOTES_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.SECURE_NOTE,
    fields = listOf(
        FieldView(
            name = "text",
            value = "value",
            type = FieldType.TEXT,
            linkedId = null,
        ),
        FieldView(
            name = "hidden",
            value = "value",
            type = FieldType.HIDDEN,
            linkedId = null,
        ),
        FieldView(
            name = "boolean",
            value = "true",
            type = FieldType.BOOLEAN,
            linkedId = null,
        ),
    ),
    secureNote = SecureNoteView(type = SecureNoteType.GENERIC),
)

private val DEFAULT_IDENTITY_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.IDENTITY,
    identity = IdentityView(
        title = "MR",
        firstName = "mockFirstName",
        lastName = "mockLastName",
        middleName = "mockMiddleName",
        address1 = "mockAddress1",
        address2 = "mockAddress2",
        address3 = "mockAddress3",
        city = "mockCity",
        state = "mockState",
        postalCode = "mockPostalCode",
        country = "mockCountry",
        company = "mockCompany",
        email = "mockEmail",
        phone = "mockPhone",
        ssn = "mockSsn",
        username = "MockUsername",
        passportNumber = "mockPassportNumber",
        licenseNumber = "mockLicenseNumber",
    ),
)

private val DEFAULT_CARD_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.CARD,
    card = CardView(
        cardholderName = "mockName-1",
        expMonth = "3",
        expYear = "2028",
        code = "987",
        brand = "Visa",
        number = "1234567",
    ),
)
