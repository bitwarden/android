package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.LoginView
import com.bitwarden.core.PasswordHistoryView
import com.bitwarden.core.SecureNoteType
import com.bitwarden.core.SecureNoteView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.additem.VaultAddItemState
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class VaultDataExtensionsTest {

    @AfterEach
    fun tearDown() {
        // Some individual tests call mockkStatic so we will make sure this is always undone.
        unmockkStatic(Instant::class)
    }

    @Test
    fun `toViewState should transform full VaultData into ViewState Content`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1)),
            collectionViewList = listOf(createMockCollectionView(number = 1)),
            folderViewList = listOf(createMockFolderView(number = 1)),
        )

        val actual = vaultData.toViewState()

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                folderItems = listOf(
                    VaultState.ViewState.FolderItem(
                        id = "mockId-1",
                        name = "mockName-1".asText(),
                        itemCount = 1,
                    ),
                ),
                collectionItems = listOf(
                    VaultState.ViewState.CollectionItem(
                        id = "mockId-1",
                        name = "mockName-1",
                        itemCount = 1,
                    ),
                ),
                noFolderItems = listOf(),
                trashItemsCount = 0,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should transform empty VaultData into ViewState NoItems`() {
        val vaultData = VaultData(
            cipherViewList = emptyList(),
            collectionViewList = emptyList(),
            folderViewList = emptyList(),
        )

        val actual = vaultData.toViewState()

        assertEquals(
            VaultState.ViewState.NoItems,
            actual,
        )
    }

    @Test
    fun `toViewState should not transform ciphers with no ID into ViewState items`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1).copy(id = null)),
            collectionViewList = listOf(createMockCollectionView(number = 1)),
            folderViewList = listOf(createMockFolderView(number = 1)),
        )

        val actual = vaultData.toViewState()

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 0,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = emptyList(),
                folderItems = listOf(
                    VaultState.ViewState.FolderItem(
                        id = "mockId-1",
                        name = "mockName-1".asText(),
                        itemCount = 0,
                    ),
                ),
                collectionItems = listOf(
                    VaultState.ViewState.CollectionItem(
                        id = "mockId-1",
                        name = "mockName-1",
                        itemCount = 0,
                    ),
                ),
                noFolderItems = emptyList(),
                trashItemsCount = 0,
            ),
            actual,
        )
    }

    @Test
    fun `toCipherView should transform Login ItemType to CipherView`() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns Instant.MIN
        val loginItemType = VaultAddItemState.ViewState.Content.Login(
            name = "mockName-1",
            username = "mockUsername-1",
            password = "mockPassword-1",
            uri = "mockUri-1",
            folderName = "mockFolder-1".asText(),
            favorite = false,
            masterPasswordReprompt = false,
            notes = "mockNotes-1",
            ownership = "mockOwnership-1",
        )

        val result = loginItemType.toCipherView()

        assertEquals(
            CipherView(
                id = null,
                organizationId = null,
                folderId = null,
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
                        ),
                    ),
                    totp = null,
                    autofillOnPageLoad = null,
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
                creationDate = Instant.MIN,
                deletedDate = null,
                revisionDate = Instant.MIN,
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform Login ItemType to CipherView with original cipher`() {
        val cipherView = DEFAULT_LOGIN_CIPHER_VIEW
        val loginItemType = VaultAddItemState.ViewState.Content.Login(
            originalCipher = cipherView,
            name = "mockName-1",
            username = "mockUsername-1",
            password = "mockPassword-1",
            uri = "mockUri-1",
            folderName = "mockFolder-1".asText(),
            favorite = true,
            masterPasswordReprompt = false,
            notes = "mockNotes-1",
            ownership = "mockOwnership-1",
            customFieldData = emptyList(),
        )

        val result = loginItemType.toCipherView()

        assertEquals(
            @Suppress("MaxLineLength")
            cipherView.copy(
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.LOGIN,
                login = LoginView(
                    username = "mockUsername-1",
                    password = "mockPassword-1",
                    passwordRevisionDate = Instant.ofEpochSecond(1_000L),
                    uris = listOf(
                        LoginUriView(
                            uri = "mockUri-1",
                            match = UriMatchType.DOMAIN,
                        ),
                    ),
                    totp = "otpauth://totp/Example:alice@google.com?secret=JBSWY3DPEHPK3PXP&issuer=Example",
                    autofillOnPageLoad = false,
                ),
                favorite = true,
                reprompt = CipherRepromptType.NONE,
                fields = emptyList(),
                passwordHistory = listOf(
                    PasswordHistoryView(
                        password = "old_password",
                        lastUsedDate = Instant.ofEpochSecond(1_000L),
                    ),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform SecureNotes ItemType to CipherView`() {
        mockkStatic(Instant::class)
        every { Instant.now() } returns Instant.MIN
        val secureNotesItemType = VaultAddItemState.ViewState.Content.SecureNotes(
            name = "mockName-1",
            folderName = "mockFolder-1".asText(),
            favorite = false,
            masterPasswordReprompt = false,
            notes = "mockNotes-1",
            ownership = "mockOwnership-1",
        )

        val result = secureNotesItemType.toCipherView()

        assertEquals(
            CipherView(
                id = null,
                organizationId = null,
                folderId = null,
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
                fields = emptyList(),
                passwordHistory = null,
                creationDate = Instant.MIN,
                deletedDate = null,
                revisionDate = Instant.MIN,
            ),
            result,
        )
    }

    @Test
    fun `toCipherView should transform SecureNotes ItemType to CipherView with original cipher`() {
        val cipherView = DEFAULT_SECURE_NOTES_CIPHER_VIEW
        val secureNotesItemType = VaultAddItemState.ViewState.Content.SecureNotes(
            originalCipher = cipherView,
            name = "mockName-1",
            folderName = "mockFolder-1".asText(),
            favorite = false,
            masterPasswordReprompt = true,
            notes = "mockNotes-1",
            ownership = "mockOwnership-1",
            customFieldData = emptyList(),
        )

        val result = secureNotesItemType.toCipherView()

        assertEquals(
            cipherView.copy(
                name = "mockName-1",
                notes = "mockNotes-1",
                type = CipherType.SECURE_NOTE,
                secureNote = SecureNoteView(SecureNoteType.GENERIC),
                reprompt = CipherRepromptType.PASSWORD,
                fields = emptyList(),
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
    fields = emptyList(),
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
    secureNote = SecureNoteView(type = SecureNoteType.GENERIC),
)
