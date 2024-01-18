package com.x8bit.bitwarden.ui.vault.feature.vault.util

import android.net.Uri
import com.bitwarden.core.CipherType
import com.bitwarden.core.LoginUriView
import com.bitwarden.core.UriMatchType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
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
            sendViewList = listOf(createMockSendView(number = 1)),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
        )

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
                totpItemsCount = 1,
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
            sendViewList = listOf(createMockSendView(number = 1)),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.MyVault,
        )

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
                totpItemsCount = 1,
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
            sendViewList = listOf(createMockSendView(number = 1)),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
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
                totpItemsCount = 1,
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
            sendViewList = emptyList(),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
        )

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
            sendViewList = listOf(createMockSendView(number = 1)),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
        )

        assertEquals(
            VaultState.ViewState.NoItems,
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should return 1 for totpItemsCount if user has premium and has one totp item`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1)),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                folderItems = listOf(),
                collectionItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 1,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should return 0 for totpItemsCount if user does not have premium and has any totp items`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1)),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = false,
            vaultFilterType = VaultFilterType.AllVaults,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                folderItems = listOf(),
                collectionItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 0,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return a IconData Local type if isIconLoadingDisabled is true`() {
        val actual =
            createMockCipherView(
                number = 1,
                cipherType = CipherType.LOGIN,
            )
                .login
                ?.uris
                .toLoginIconData(
                    isIconLoadingDisabled = true,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                )

        val expected = IconData.Local(iconRes = R.drawable.ic_login_item)

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return a IconData Local type if no valid uris are found`() {
        val actual = listOf(
            LoginUriView(
                uri = "",
                match = UriMatchType.HOST,
            ),
        )
            .toLoginIconData(
                isIconLoadingDisabled = false,
                baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            )

        val expected = IconData.Local(iconRes = R.drawable.ic_login_item)

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return a IconData Local type if an Android uri is detected`() {
        val actual = listOf(
            LoginUriView(
                uri = "androidapp://test.com",
                match = UriMatchType.HOST,
            ),
        )
            .toLoginIconData(
                isIconLoadingDisabled = false,
                baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            )

        val expected = IconData.Local(iconRes = R.drawable.ic_android)

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return a IconData Local type if an iOS uri is detected`() {
        val actual = listOf(
            LoginUriView(
                uri = "iosapp://test.com",
                match = UriMatchType.HOST,
            ),
        )
            .toLoginIconData(
                isIconLoadingDisabled = false,
                baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            )

        val expected = IconData.Local(iconRes = R.drawable.ic_ios)

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return IconData Network type if isIconLoadingDisabled is false`() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri1.com"

        val actual =
            createMockCipherView(
                number = 1,
                cipherType = CipherType.LOGIN,
            )
                .login
                ?.uris
                .toLoginIconData(
                    isIconLoadingDisabled = false,
                    baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                )

        val expected = IconData.Network(
            uri = "https://vault.bitwarden.com/icons/www.mockuri1.com/icon.png",
            fallbackIconRes = R.drawable.ic_login_item,
        )

        assertEquals(expected, actual)

        unmockkStatic(Uri::class)
    }
}
