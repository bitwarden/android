package com.x8bit.bitwarden.ui.vault.feature.itemlisting.util

import android.net.Uri
import com.bitwarden.core.CipherType
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.VaultItemListingState
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.Assert.assertEquals
import org.junit.Test

class VaultItemListingDataExtensionsTest {

    @Test
    @Suppress("MaxLineLength")
    fun `determineListingPredicate should return the correct predicate for non trash Login cipherView`() {
        val cipherView = createMockCipherView(
            number = 1,
            isDeleted = false,
            cipherType = CipherType.LOGIN,
        )

        mapOf(
            VaultItemListingState.ItemListingType.Login to true,
            VaultItemListingState.ItemListingType.Card to false,
            VaultItemListingState.ItemListingType.SecureNote to false,
            VaultItemListingState.ItemListingType.Identity to false,
            VaultItemListingState.ItemListingType.Trash to false,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to true,
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
            VaultItemListingState.ItemListingType.Login to false,
            VaultItemListingState.ItemListingType.Card to false,
            VaultItemListingState.ItemListingType.SecureNote to false,
            VaultItemListingState.ItemListingType.Identity to false,
            VaultItemListingState.ItemListingType.Trash to true,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to false,
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
            VaultItemListingState.ItemListingType.Login to false,
            VaultItemListingState.ItemListingType.Card to true,
            VaultItemListingState.ItemListingType.SecureNote to false,
            VaultItemListingState.ItemListingType.Identity to false,
            VaultItemListingState.ItemListingType.Trash to false,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to true,
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
            VaultItemListingState.ItemListingType.Login to false,
            VaultItemListingState.ItemListingType.Card to false,
            VaultItemListingState.ItemListingType.SecureNote to false,
            VaultItemListingState.ItemListingType.Identity to false,
            VaultItemListingState.ItemListingType.Trash to true,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to false,
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
            VaultItemListingState.ItemListingType.Login to false,
            VaultItemListingState.ItemListingType.Card to false,
            VaultItemListingState.ItemListingType.SecureNote to false,
            VaultItemListingState.ItemListingType.Identity to true,
            VaultItemListingState.ItemListingType.Trash to false,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to true,
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
            VaultItemListingState.ItemListingType.Login to false,
            VaultItemListingState.ItemListingType.Card to false,
            VaultItemListingState.ItemListingType.SecureNote to false,
            VaultItemListingState.ItemListingType.Identity to false,
            VaultItemListingState.ItemListingType.Trash to true,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to false,
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
            VaultItemListingState.ItemListingType.Login to false,
            VaultItemListingState.ItemListingType.Card to false,
            VaultItemListingState.ItemListingType.SecureNote to true,
            VaultItemListingState.ItemListingType.Identity to false,
            VaultItemListingState.ItemListingType.Trash to false,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to true,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to true,
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
            VaultItemListingState.ItemListingType.Login to false,
            VaultItemListingState.ItemListingType.Card to false,
            VaultItemListingState.ItemListingType.SecureNote to false,
            VaultItemListingState.ItemListingType.Identity to false,
            VaultItemListingState.ItemListingType.Trash to true,
            VaultItemListingState.ItemListingType.Folder(folderId = "mockId-1") to false,
            VaultItemListingState.ItemListingType.Collection(collectionId = "mockId-1") to false,
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
    fun `toViewState should transform a list of CipherViews into a ViewState`() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
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
                    createMockItemListingDisplayItem(
                        number = 1,
                        cipherType = CipherType.LOGIN,
                    ),
                    createMockItemListingDisplayItem(
                        number = 2,
                        cipherType = CipherType.CARD,
                    ),
                    createMockItemListingDisplayItem(
                        number = 3,
                        cipherType = CipherType.SECURE_NOTE,
                    ),
                    createMockItemListingDisplayItem(
                        number = 4,
                        cipherType = CipherType.IDENTITY,
                    ),
                ),
            ),
            result,
        )

        unmockkStatic(Uri::class)
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

        val result = VaultItemListingState.ItemListingType.Folder(
            folderId = "mockId-1",
            folderName = "wrong name",
        )
            .updateWithAdditionalDataIfNecessary(
                folderList = folderViewList,
                collectionList = collectionViewList,
            )

        assertEquals(
            VaultItemListingState.ItemListingType.Folder(
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

        val result = VaultItemListingState.ItemListingType.Collection(
            collectionId = "mockId-1",
            collectionName = "wrong name",
        )
            .updateWithAdditionalDataIfNecessary(
                folderList = folderViewList,
                collectionList = collectionViewList,
            )

        assertEquals(
            VaultItemListingState.ItemListingType.Collection(
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

        val result = VaultItemListingState.ItemListingType.Login
            .updateWithAdditionalDataIfNecessary(
                folderList = folderViewList,
                collectionList = collectionViewList,
            )

        assertEquals(
            VaultItemListingState.ItemListingType.Login,
            result,
        )
    }
}
