package com.x8bit.bitwarden.ui.vault.feature.addedit.util

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
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.vault.datasource.network.model.OrganizationType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.manager.resource.ResourceManager
import com.x8bit.bitwarden.ui.vault.feature.addedit.VaultAddEditState
import com.x8bit.bitwarden.ui.vault.feature.addedit.model.UriItem
import com.x8bit.bitwarden.ui.vault.model.VaultAddEditType
import com.x8bit.bitwarden.ui.vault.model.VaultCardBrand
import com.x8bit.bitwarden.ui.vault.model.VaultCollection
import com.x8bit.bitwarden.ui.vault.model.VaultItemCipherType
import com.x8bit.bitwarden.ui.vault.model.VaultLinkedFieldType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class CipherViewExtensionsTest {

    private val resourceManager: ResourceManager = mockk {
        every { getString(R.string.clone) } returns "Clone"
        every { getString(R.string.folder_none) } returns "No Folder"
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
            isIndividualVaultDisabled = false,
            totpData = null,
            resourceManager = resourceManager,
            clock = FIXED_CLOCK,
            canDelete = true,
            canAssignToCollections = true,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
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
                isIndividualVaultDisabled = false,
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
            isIndividualVaultDisabled = true,
            totpData = null,
            resourceManager = resourceManager,
            clock = FIXED_CLOCK,
            canDelete = true,
            canAssignToCollections = true,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
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
                isIndividualVaultDisabled = true,
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
            isIndividualVaultDisabled = false,
            totpData = null,
            resourceManager = resourceManager,
            clock = FIXED_CLOCK,
            canDelete = true,
            canAssignToCollections = true,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
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
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = "username",
                    password = "password",
                    uriList = listOf(
                        UriItem(
                            id = TEST_ID,
                            uri = "www.example.com",
                            match = null,
                            checksum = null,
                        ),
                    ),
                    totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                    canViewPassword = false,
                    fido2CredentialCreationDateTime = R.string.created_xy.asText(
                        "10/27/23",
                        "12:00 PM",
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toViewState should create a Login ViewState with a predefined totp`() {
        val totp = "otpauth://totp/alice@google.com?secret=JBSWY3DPEHPK3PXP"
        val cipherView = DEFAULT_LOGIN_CIPHER_VIEW.copy(
            login = DEFAULT_LOGIN_CIPHER_VIEW.login?.copy(totp = null),
        )

        val result = cipherView.toViewState(
            isClone = false,
            isIndividualVaultDisabled = false,
            totpData = mockk { every { uri } returns totp },
            resourceManager = resourceManager,
            clock = FIXED_CLOCK,
            canDelete = true,
            canAssignToCollections = true,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
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
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.Login(
                    username = "username",
                    password = "password",
                    uriList = listOf(
                        UriItem(
                            id = TEST_ID,
                            uri = "www.example.com",
                            match = null,
                            checksum = null,
                        ),
                    ),
                    totp = totp,
                    canViewPassword = false,
                    fido2CredentialCreationDateTime = R.string.created_xy.asText(
                        "10/27/23",
                        "12:00 PM",
                    ),
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
            isIndividualVaultDisabled = true,
            totpData = null,
            resourceManager = resourceManager,
            clock = FIXED_CLOCK,
            canDelete = true,
            canAssignToCollections = true,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(TEST_ID, "TestBoolean", false),
                        VaultAddEditState.Custom.TextField(TEST_ID, "TestText", "TestText"),
                        VaultAddEditState.Custom.HiddenField(TEST_ID, "TestHidden", "TestHidden"),
                    ),
                    availableFolders = emptyList(),
                    availableOwners = emptyList(),
                ),
                isIndividualVaultDisabled = true,
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
            ),
            result,
        )
    }

    @Test
    fun `toViewState should create SSH Key ViewState`() {
        val cipherView = DEFAULT_SSH_KEY_CIPHER_VIEW

        val result = cipherView.toViewState(
            isClone = false,
            isIndividualVaultDisabled = false,
            totpData = null,
            resourceManager = resourceManager,
            clock = FIXED_CLOCK,
            canDelete = true,
            canAssignToCollections = true,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher",
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
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
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.SshKey(
                    publicKey = "PublicKey",
                    privateKey = "PrivateKey",
                    fingerprint = "Fingerprint",
                ),
            ),
            result,
        )
    }

    @Test
    fun `toViewState with isClone true should append clone text to the cipher name`() {
        val cipherView = DEFAULT_SECURE_NOTES_CIPHER_VIEW

        val result = cipherView.toViewState(
            isClone = true,
            isIndividualVaultDisabled = false,
            totpData = null,
            resourceManager = resourceManager,
            clock = FIXED_CLOCK,
            canDelete = true,
            canAssignToCollections = true,
        )

        assertEquals(
            VaultAddEditState.ViewState.Content(
                common = VaultAddEditState.ViewState.Content.Common(
                    originalCipher = cipherView,
                    name = "cipher - Clone",
                    favorite = false,
                    masterPasswordReprompt = true,
                    notes = "Lots of notes",
                    customFieldData = listOf(
                        VaultAddEditState.Custom.BooleanField(TEST_ID, "TestBoolean", false),
                        VaultAddEditState.Custom.TextField(TEST_ID, "TestText", "TestText"),
                        VaultAddEditState.Custom.HiddenField(TEST_ID, "TestHidden", "TestHidden"),
                    ),
                    availableFolders = emptyList(),
                    availableOwners = emptyList(),
                ),
                isIndividualVaultDisabled = false,
                type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
            ),
            result,
        )
    }

    @Test
    fun `validateCipherOrReturnErrorState with valid cipher should return provided state`() {
        val providedState = VaultAddEditState.ViewState.Loading

        val result = createMockCipherView(number = 1)
            .validateCipherOrReturnErrorState(
                currentAccount = createAccount(),
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            ) { _, _ -> providedState }

        assertEquals(providedState, result)
    }

    @Test
    fun `validateCipherOrReturnErrorState with EditItem null cipher type should return Error`() {
        val providedState = VaultAddEditState.ViewState.Loading

        val result = null
            .validateCipherOrReturnErrorState(
                currentAccount = createAccount(),
                vaultAddEditType = VaultAddEditType.EditItem(vaultItemId = "mockId-1"),
            ) { _, _ -> providedState }

        assertEquals(
            VaultAddEditState.ViewState.Error(R.string.generic_error_message.asText()),
            result,
        )
    }

    @Test
    fun `validateCipherOrReturnErrorState with null account type should return Error`() {
        val providedState = VaultAddEditState.ViewState.Loading

        val result = createMockCipherView(number = 1)
            .validateCipherOrReturnErrorState(
                currentAccount = null,
                vaultAddEditType = VaultAddEditType.AddItem(VaultItemCipherType.LOGIN),
            ) { _, _ -> providedState }

        assertEquals(
            VaultAddEditState.ViewState.Error(R.string.generic_error_message.asText()),
            result,
        )
    }

    @Test
    fun `appendFolderAndOwnerData should append folder and owner data`() {
        val viewState = createSecureNoteViewState(withFolderAndOwnerData = false)
        val account = createAccount()
        val folderView = listOf(createMockFolderView(number = 1))
        val collectionList = listOf(createMockCollectionView(number = 1))

        val result = viewState.appendFolderAndOwnerData(
            folderViewList = folderView,
            collectionViewList = collectionList,
            activeAccount = account,
            isIndividualVaultDisabled = false,
            resourceManager = resourceManager,
        )

        assertEquals(
            createSecureNoteViewState(withFolderAndOwnerData = true),
            result,
        )
    }

    private fun createSecureNoteViewState(
        cipherView: CipherView = createMockCipherView(number = 1),
        withFolderAndOwnerData: Boolean,
    ): VaultAddEditState.ViewState.Content =
        VaultAddEditState.ViewState.Content(
            common = VaultAddEditState.ViewState.Content.Common(
                originalCipher = cipherView,
                name = "cipher",
                favorite = false,
                masterPasswordReprompt = true,
                notes = "Lots of notes",
                customFieldData = listOf(
                    VaultAddEditState.Custom.BooleanField(
                        itemId = TEST_ID,
                        name = "TestBoolean",
                        value = false,
                    ),
                    VaultAddEditState.Custom.TextField(
                        itemId = TEST_ID,
                        name = "TestText",
                        value = "TestText",
                    ),
                    VaultAddEditState.Custom.HiddenField(
                        itemId = TEST_ID,
                        name = "TestHidden",
                        value = "TestHidden",
                    ),
                ),
                availableFolders = emptyList(),
                availableOwners = emptyList(),
            )
                .let {
                    if (withFolderAndOwnerData) {
                        it.copy(
                            selectedFolderId = "mockId-1",
                            selectedOwnerId = "mockOrganizationId-1",
                            availableFolders = listOf(
                                VaultAddEditState.Folder(
                                    id = null,
                                    name = "No Folder",
                                ),
                                VaultAddEditState.Folder(
                                    id = "mockId-1",
                                    name = "mockName-1",
                                ),
                            ),
                            hasOrganizations = true,
                            availableOwners = listOf(
                                VaultAddEditState.Owner(
                                    id = null,
                                    name = "activeEmail",
                                    collections = emptyList(),
                                ),
                                VaultAddEditState.Owner(
                                    id = "mockOrganizationId-1",
                                    name = "organizationName",
                                    collections = listOf(
                                        VaultCollection(
                                            id = "mockId-1",
                                            name = "mockName-1",
                                            isSelected = true,
                                        ),
                                    ),
                                ),
                            ),
                        )
                    } else {
                        it
                    }
                },
            isIndividualVaultDisabled = true,
            type = VaultAddEditState.ViewState.Content.ItemType.SecureNotes,
        )

    private fun createAccount(): UserState.Account =
        UserState.Account(
            userId = "activeUserId",
            name = "activeName",
            email = "activeEmail",
            avatarColorHex = "#ffecbc49",
            environment = Environment.Eu,
            isPremium = true,
            isLoggedIn = false,
            isVaultUnlocked = false,
            needsPasswordReset = false,
            organizations = listOf(
                Organization(
                    id = "mockOrganizationId-1",
                    name = "organizationName",
                    shouldManageResetPassword = false,
                    shouldUseKeyConnector = false,
                    role = OrganizationType.ADMIN,
                    shouldUsersGetPremium = false,
                ),
            ),
            isBiometricsEnabled = true,
            vaultUnlockType = VaultUnlockType.MASTER_PASSWORD,
            needsMasterPassword = false,
            trustedDevice = null,
            hasMasterPassword = true,
            isUsingKeyConnector = false,
            onboardingStatus = OnboardingStatus.COMPLETE,
            firstTimeState = FirstTimeState(showImportLoginsCard = true),
        )
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
    edit = true,
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
            lastUsedDate = FIXED_CLOCK.instant(),
        ),
    ),
    creationDate = FIXED_CLOCK.instant(),
    deletedDate = null,
    revisionDate = FIXED_CLOCK.instant(),
    sshKey = null,
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
        fido2Credentials = createMockSdkFido2CredentialList(number = 1, clock = FIXED_CLOCK),
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

private val DEFAULT_SSH_KEY_CIPHER_VIEW: CipherView = DEFAULT_BASE_CIPHER_VIEW.copy(
    type = CipherType.SSH_KEY,
    sshKey = SshKeyView(
        publicKey = "PublicKey",
        privateKey = "PrivateKey",
        fingerprint = "Fingerprint",
    ),
)

private const val TEST_ID = "testID"
