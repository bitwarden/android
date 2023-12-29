package com.x8bit.bitwarden.ui.vault.feature.vault.util

import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class VaultDataExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState for AllVaults should transform full VaultData into ViewState Content without filtering`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1)),
            collectionViewList = listOf(createMockCollectionView(number = 1)),
            folderViewList = listOf(createMockFolderView(number = 1)),
        )

        val actual = vaultData.toViewState(vaultFilterType = VaultFilterType.AllVaults)

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

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState for MyVault should transform full VaultData into ViewState Content with filtering of non-user data`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1).copy(organizationId = null),
                createMockCipherView(number = 2),
            ),
            collectionViewList = listOf(createMockCollectionView(number = 1)),
            folderViewList = listOf(createMockFolderView(number = 1)),
        )

        val actual = vaultData.toViewState(vaultFilterType = VaultFilterType.MyVault)

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
                collectionItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState for OrganizationVault should transform full VaultData into ViewState Content with filtering of non-organization data`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1),
                createMockCipherView(number = 2),
            ),
            collectionViewList = listOf(
                createMockCollectionView(number = 1),
                createMockCollectionView(number = 2),
            ),
            folderViewList = listOf(createMockFolderView(number = 1)),
        )

        val actual = vaultData.toViewState(
            vaultFilterType = VaultFilterType.OrganizationVault(
                organizationId = "mockOrganizationId-1",
                organizationName = "Mock Organization 1",
            ),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                folderItems = listOf(),
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

        val actual = vaultData.toViewState(vaultFilterType = VaultFilterType.AllVaults)

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

        val actual = vaultData.toViewState(vaultFilterType = VaultFilterType.AllVaults)

        assertEquals(
            VaultState.ViewState.NoItems,
            actual,
        )
    }
}
