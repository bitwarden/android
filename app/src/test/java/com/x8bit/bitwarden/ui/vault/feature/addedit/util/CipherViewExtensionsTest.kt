package com.x8bit.bitwarden.ui.vault.feature.addedit.util

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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class CipherViewExtensionsTest {

    private val resourceManager: ResourceManager = mockk {
        every { getString(R.string.clone) } returns "Clone"
    }

    @BeforeEach
    fun setup() {
        mockkStatic(UUID::randomUUID)
        every { UUID.randomUUID().toString() } returns TEST_ID
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(UUID::randomUUID)
    }

    @Test
    fun `toViewState should create a Card ViewState`() {
        val cipherView = DEFAULT_CARD_CIPHER_VIEW

        val result = cipherView.toViewState(
            isClone = false,
            resourceManager = resourceManager,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    folderName = R.string.folder_none.asText(),
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
                    ownership = "",
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(TEST_ID, "TestBoolean", false),
                        VaultAddEditState.Custom.TextField(TEST_ID, "TestText", "TestText"),
                        VaultAddEditState.Custom.HiddenField(TEST_ID, "TestHidden", "TestHidden"),
                        VaultAddEditState.Custom.LinkedField(
                            TEST_ID,
                            "TestLinked",
                            VaultLinkedFieldType.USERNAME,
                        ),
                    ),
                    availableFolders = emptyList(),
                    availableOwners = emptyList(),
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.Card(
                    cardHolderName = "Bit Warden",
                    number = "4012888888881881",
                    brand = VaultCardBrand.VISA,
                    expirationYear = "2030",
                    securityCode = "123",
                ),
            ),
            result,
        )
    }

    @Test
    fun `toViewState should create a Identity ViewState`() {
        val cipherView = DEFAULT_IDENTITY_CIPHER_VIEW

        val result = cipherView.toViewState(
            isClone = false,
            resourceManager = resourceManager,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    folderName = R.string.folder_none.asText(),
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
                    ownership = "",
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(TEST_ID, "TestBoolean", false),
                        VaultAddEditState.Custom.TextField(TEST_ID, "TestText", "TestText"),
                        VaultAddEditState.Custom.HiddenField(TEST_ID, "TestHidden", "TestHidden"),
                        VaultAddEditState.Custom.LinkedField(
                            TEST_ID,
                            "TestLinked",
                            VaultLinkedFieldType.USERNAME,
                        ),
                    ),
                    availableFolders = emptyList(),
                    availableOwners = emptyList(),
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.Identity(
                    firstName = "John",
                    middleName = "Richard",
                    lastName = "Smith",
                    username = "Dr. JSR",
                    company = "Bitwarden",
                    email = "placeholde@email.com",
                    phone = "555-555-5555",
                    city = "Minneapolis",
                    country = "USA",
                ),
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should create a Login ViewState`() {
        val cipherView = DEFAULT_LOGIN_CIPHER_VIEW

        val result = cipherView.toViewState(
            isClone = false,
            resourceManager = resourceManager,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    folderName = R.string.folder_none.asText(),
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
                    ownership = "",
                    availableFolders = emptyList(),
                    availableOwners = emptyList(),
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(TEST_ID, "TestBoolean", false),
                        VaultAddEditState.Custom.TextField(TEST_ID, "TestText", "TestText"),
                        VaultAddEditState.Custom.HiddenField(TEST_ID, "TestHidden", "TestHidden"),
                        VaultAddEditState.Custom.LinkedField(
                            TEST_ID,
                            "TestLinked",
                            VaultLinkedFieldType.USERNAME,
                        ),
                    ),
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = "username",
                    password = "password",
                    uri = "www.example.com",
                    totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                    canViewPassword = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toViewState should create a Secure Notes ViewState`() {
        val cipherView = DEFAULT_SECURE_NOTES_CIPHER_VIEW

        val result = cipherView.toViewState(
            isClone = false,
            resourceManager = resourceManager,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    folderName = R.string.folder_none.asText(),
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
                    ownership = "",
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(TEST_ID, "TestBoolean", false),
                        VaultAddEditState.Custom.TextField(TEST_ID, "TestText", "TestText"),
                        VaultAddEditState.Custom.HiddenField(TEST_ID, "TestHidden", "TestHidden"),
                    ),
                    availableFolders = emptyList(),
                    availableOwners = emptyList(),
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
            ),
            result,
        )
    }

    @Test
    fun `toViewState with isClone true should append clone text to the cipher name`() {
        val cipherView = DEFAULT_SECURE_NOTES_CIPHER_VIEW

        val result = cipherView.toViewState(
            isClone = true,
            resourceManager = resourceManager,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher - Clone",
                    folderName = R.string.folder_none.asText(),
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
                    ownership = "",
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(TEST_ID, "TestBoolean", false),
                        VaultAddEditState.Custom.TextField(TEST_ID, "TestText", "TestText"),
                        VaultAddEditState.Custom.HiddenField(TEST_ID, "TestHidden", "TestHidden"),
                    ),
                    availableFolders = emptyList(),
                    availableOwners = emptyList(),
                ),
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
            ),
            result,
        )
    }
}

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
            name = "TestBoolean",
            value = false.toString(),
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
            lastUsedDate = Instant.ofEpochSecond(1_000L),
        ),
    ),
    creationDate = Instant.ofEpochSecond(1_000L),
    deletedDate = null,
    revisionDate = Instant.ofEpochSecond(1_000L),
)

private val DEFAULT_CARD_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.CARD,
    card = CardView(
        cardholderName = "Bit Warden",
        expMonth = "04",
        expYear = "2030",
        code = "123",
        brand = "Visa",
        number = "4012888888881881",
    ),
)

private val DEFAULT_IDENTITY_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.IDENTITY,
    identity = IdentityView(
        title = "Dr.",
        firstName = "John",
        lastName = "Smith",
        middleName = "Richard",
        address1 = null,
        address2 = null,
        address3 = null,
        city = "Minneapolis",
        state = "MN",
        postalCode = null,
        country = "USA",
        company = "Bitwarden",
        email = "placeholde@email.com",
        phone = "555-555-5555",
        ssn = null,
        username = "Dr. JSR",
        passportNumber = null,
        licenseNumber = null,
    ),
)

private val DEFAULT_LOGIN_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.LOGIN,
    login = LoginView(
        username = "username",
        password = "password",
        passwordRevisionDate = Instant.ofEpochSecond(1_000L),
        uris = listOf(
            LoginUriView(
                uri = "www.example.com",
                match = null,
            ),
        ),
        totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
        autofillOnPageLoad = false,
    ),
)

private val DEFAULT_SECURE_NOTES_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.SECURE_NOTE,
    fields = listOf(
        FieldView(
            name = "TestBoolean",
            value = false.toString(),
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
    secureNote = SecureNoteView(type = SecureNoteType.GENERIC),
)

private const val TEST_ID = "testID"
