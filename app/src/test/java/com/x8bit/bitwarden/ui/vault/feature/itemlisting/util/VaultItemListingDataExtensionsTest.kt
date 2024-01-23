package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import android.net.Uri
import com.bitwarden.core.CipherType
import com.bitwarden.core.CipherView
import com.bitwarden.core.SendType
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.platform.repository.util.baseWebSendUrl
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class VaultItemListingDataExtensionsTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for non trash Login cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = false,
            cipherType = CipherType.LOGIN,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to true,
            VaultItemListingState.ItemListingType.Vault.Card to false,
            VaultItemListingState.ItemListingType.Vault.SecureNote to false,
            VaultItemListingState.ItemListingType.Vault.Identity to false,
            VaultItemListingState.ItemListingType.Vault.Trash to false,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to true,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for trash Login cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.LOGIN,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to false,
            VaultItemListingState.ItemListingType.Vault.Card to false,
            VaultItemListingState.ItemListingType.Vault.SecureNote to false,
            VaultItemListingState.ItemListingType.Vault.Identity to false,
            VaultItemListingState.ItemListingType.Vault.Trash to true,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to false,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for non trash Card cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = false,
            cipherType = CipherType.CARD,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to false,
            VaultItemListingState.ItemListingType.Vault.Card to true,
            VaultItemListingState.ItemListingType.Vault.SecureNote to false,
            VaultItemListingState.ItemListingType.Vault.Identity to false,
            VaultItemListingState.ItemListingType.Vault.Trash to false,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to true,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for trash Card cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.CARD,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to false,
            VaultItemListingState.ItemListingType.Vault.Card to false,
            VaultItemListingState.ItemListingType.Vault.SecureNote to false,
            VaultItemListingState.ItemListingType.Vault.Identity to false,
            VaultItemListingState.ItemListingType.Vault.Trash to true,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to false,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for non trash Identity cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = false,
            cipherType = CipherType.IDENTITY,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to false,
            VaultItemListingState.ItemListingType.Vault.Card to false,
            VaultItemListingState.ItemListingType.Vault.SecureNote to false,
            VaultItemListingState.ItemListingType.Vault.Identity to true,
            VaultItemListingState.ItemListingType.Vault.Trash to false,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to true,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for trash Identity cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.IDENTITY,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to false,
            VaultItemListingState.ItemListingType.Vault.Card to false,
            VaultItemListingState.ItemListingType.Vault.SecureNote to false,
            VaultItemListingState.ItemListingType.Vault.Identity to false,
            VaultItemListingState.ItemListingType.Vault.Trash to true,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to false,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for non trash SecureNote cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = false,
            cipherType = CipherType.SECURE_NOTE,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to false,
            VaultItemListingState.ItemListingType.Vault.Card to false,
            VaultItemListingState.ItemListingType.Vault.SecureNote to true,
            VaultItemListingState.ItemListingType.Vault.Identity to false,
            VaultItemListingState.ItemListingType.Vault.Trash to false,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to true,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for trash SecureNote cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.SECURE_NOTE,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Vault.Login to false,
            VaultItemListingState.ItemListingType.Vault.Card to false,
            VaultItemListingState.ItemListingType.Vault.SecureNote to false,
            VaultItemListingState.ItemListingType.Vault.Identity to false,
            VaultItemListingState.ItemListingType.Vault.Trash to true,
            VaultItemListingState.ItemListingType.Vault.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Vault.Collection(collectionId = "mockId-1") to false,
        )
            .forEach { (type, expected) ->
                val result = cipherView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    fun `determineListingPredicate should return the correct predicate for File sendView`() {
        val sendView = createMockSendView(number = 1, type = SendType.FILE)

        mapOf(
            VaultItemListingState.ItemListingType.Send.SendFile to true,
            VaultItemListingState.ItemListingType.Send.SendText to false,
        )
            .forEach { (type, expected) ->
                val result = sendView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    fun `determineListingPredicate should return the correct predicate for Text sendView`() {
        val sendView = createMockSendView(number = 1, type = SendType.TEXT)

        mapOf(
            VaultItemListingState.ItemListingType.Send.SendFile to false,
            VaultItemListingState.ItemListingType.Send.SendText to true,
        )
            .forEach { (type, expected) ->
                val result = sendView.determineListingPredicate(
                    itemListingType = type,
                )
                assertEquals(
                    expected,
                    result,
                )
            }
    }

    @Test
    fun `toViewState should transform a list of CipherViews into a ViewState`() {
        mockkStatic(CipherView::subtitle)
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { any<CipherView>().subtitle } returns null
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri.com"

        val cipherViewList = listOf(
            createMockCipherView(
                number = 1,
                isDeleted = false,
                cipherType = CipherType.LOGIN,
            ),
            createMockCipherView(
                number = 2,
                isDeleted = false,
                cipherType = CipherType.CARD,
            ),
            createMockCipherView(
                number = 3,
                isDeleted = false,
                cipherType = CipherType.SECURE_NOTE,
            ),
            createMockCipherView(
                number = 4,
                isDeleted = false,
                cipherType = CipherType.IDENTITY,
            ),
        )

        val result = cipherViewList.toViewState(
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
        )

        assertEquals(
            VaultItemListingState.ViewState.Content(
                displayItemList = listOf(
                    createMockDisplayItemForCipher(
                        number = 1,
                        cipherType = CipherType.LOGIN,
                        subtitle = null,
                    ),
                    createMockDisplayItemForCipher(
                        number = 2,
                        cipherType = CipherType.CARD,
                        subtitle = null,
                    ),
                    createMockDisplayItemForCipher(
                        number = 3,
                        cipherType = CipherType.SECURE_NOTE,
                        subtitle = null,
                    ),
                    createMockDisplayItemForCipher(
                        number = 4,
                        cipherType = CipherType.IDENTITY,
                        subtitle = null,
                    ),
                ),
            ),
            result,
        )

        unmockkStatic(CipherView::subtitle)
        unmockkStatic(Uri::class)
    }

    @Test
    fun `toViewState should transform a list of SendViews into a ViewState`() {
        val sendViewList = listOf(
            createMockSendView(number = 1, type = SendType.FILE),
            createMockSendView(number = 2, type = SendType.TEXT),
        )

        val result = sendViewList.toViewState(
            baseWebSendUrl = Environment.Us.environmentUrlData.baseWebSendUrl,
            clock = clock,
        )

        assertEquals(
            VaultItemListingState.ViewState.Content(
                displayItemList = listOf(
                    createMockDisplayItemForSend(number = 1, sendType = SendType.FILE),
                    createMockDisplayItemForSend(number = 2, sendType = SendType.TEXT),
                ),
            ),
            result,
        )
    }

    @Test
    fun `updateWithAdditionalDataIfNecessary should update a folder itemListingType`() {
        val folderViewList = listOf(
            createMockFolderView(number = 1),
            createMockFolderView(number = 2),
            createMockFolderView(number = 3),
        )
        val collectionViewList = listOf(
            createMockCollectionView(number = 1),
            createMockCollectionView(number = 2),
            createMockCollectionView(number = 3),
        )

        val result = VaultItemListingState.ItemListingType.Vault.Folder(
            folderId = "mockId-1",
            folderName = "wrong name",
        )
            .updateWithAdditionalDataIfNecessary(
                folderList = folderViewList,
                collectionList = collectionViewList,
            )

        assertEquals(
            VaultItemListingState.ItemListingType.Vault.Folder(
                folderId = "mockId-1",
                folderName = "mockName-1",
            ),
            result,
        )
    }

    @Test
    fun `updateWithAdditionalDataIfNecessary should update a collection itemListingType`() {
        val folderViewList = listOf(
            createMockFolderView(number = 1),
            createMockFolderView(number = 2),
            createMockFolderView(number = 3),
        )
        val collectionViewList = listOf(
            createMockCollectionView(number = 1),
            createMockCollectionView(number = 2),
            createMockCollectionView(number = 3),
        )

        val result = VaultItemListingState.ItemListingType.Vault.Collection(
            collectionId = "mockId-1",
            collectionName = "wrong name",
        )
            .updateWithAdditionalDataIfNecessary(
                folderList = folderViewList,
                collectionList = collectionViewList,
            )

        assertEquals(
            VaultItemListingState.ItemListingType.Vault.Collection(
                collectionId = "mockId-1",
                collectionName = "mockName-1",
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `updateWithAdditionalDataIfNecessary should not change a non-folder or non-collection itemListingType`() {
        val folderViewList = listOf(
            createMockFolderView(number = 1),
            createMockFolderView(number = 2),
            createMockFolderView(number = 3),
        )
        val collectionViewList = listOf(
            createMockCollectionView(number = 1),
            createMockCollectionView(number = 2),
            createMockCollectionView(number = 3),
        )

        val result = VaultItemListingState.ItemListingType.Vault.Login
            .updateWithAdditionalDataIfNecessary(
                folderList = folderViewList,
                collectionList = collectionViewList,
            )

        assertEquals(
            VaultItemListingState.ItemListingType.Vault.Login,
            result,
        )
    }
}
