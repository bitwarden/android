package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.bitwarden.core.CipherRepromptType
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.LoginView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
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
        val loginItemType = VaultAddItemState.ItemType.Login(
            name = "mockName-1",
            username = "mockUsername-1",
            password = "mockPassword-1",
            uri = "mockUri-1",
            folder = "mockFolder-1",
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
                    autofillOnPageLoad = false,
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
                fields = null,
                passwordHistory = null,
                creationDate = Instant.MIN,
                deletedDate = null,
                revisionDate = Instant.MIN,
            ),
            result,
        )
    }
}
