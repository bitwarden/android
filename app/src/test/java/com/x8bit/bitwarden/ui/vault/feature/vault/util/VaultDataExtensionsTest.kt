package com.x8bit.bitwarden.ui.vault.feature.vault.util

import android.net.Uri
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.LoginUriView
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.R
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import com.x8bit.bitwarden.data.platform.repository.util.baseIconUrl
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSshKeyView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.ui.platform.base.util.asText
import com.x8bit.bitwarden.ui.platform.components.model.IconData
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class VaultDataExtensionsTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState for AllVaults should transform full VaultData into ViewState Content without filtering`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1)),
            collectionViewList = listOf(createMockCollectionView(number = 1)),
            folderViewList = listOf(
                FolderView("1", "test", clock.instant()),
                FolderView("2", "test/test", clock.instant()),
                FolderView("3", "test/", clock.instant()),
                FolderView("4", "test/test/test/", clock.instant()),
                FolderView("5", "Folder", clock.instant()),
            ),
            sendViewList = listOf(createMockSendView(number = 1)),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
                        id = "1",
                        name = "test".asText(),
                        itemCount = 0,
                    ),
                    VaultState.ViewState.FolderItem(
                        id = "3",
                        name = "test/".asText(),
                        itemCount = 0,
                    ),
                    VaultState.ViewState.FolderItem(
                        id = "5",
                        name = "Folder".asText(),
                        itemCount = 0,
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
                totpItemsCount = 0,
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
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
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
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
            folderViewList = listOf(
                createMockFolderView(number = 1),
                createMockFolderView(number = 3),
            ),
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
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
                totpItemsCount = 0,
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
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
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.NoItems,
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should return 1 for totpItemsCount if user has premium and has one totp item and item is owned by user`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1, organizationId = null)),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
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
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should return 1 for totpItemsCount if user does not have premium and has at least 1 totp items with org TOTP true`() {
        val vaultData = VaultData(
            cipherViewList = listOf(createMockCipherView(number = 1).copy(organizationUseTotp = true)),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = false,
            vaultFilterType = VaultFilterType.AllVaults,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should omit non org related totp codes when user does not have premium`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1).copy(organizationUseTotp = true),
                createMockCipherView(number = 2),
            ),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = false,
            vaultFilterType = VaultFilterType.AllVaults,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 2,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                folderItems = listOf(),
                collectionItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 1,
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
            ),
            actual,
        )
    }

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
                    usePasskeyDefaultIcon = false,
                )

        val expected = IconData.Local(iconRes = R.drawable.ic_globe)

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return a IconData Local type if isIconLoadingDisabled is true and usePasskeyDefaultIcon true`() {
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
                    usePasskeyDefaultIcon = true,
                )

        val expected = IconData.Local(iconRes = R.drawable.ic_bw_passkey)

        assertEquals(expected, actual)
    }

    @Test
    fun `toLoginIconData should return a IconData Local type if no valid uris are found`() {
        val actual = listOf(
            LoginUriView(
                uri = "",
                match = UriMatchType.HOST,
                uriChecksum = null,
            ),
        )
            .toLoginIconData(
                isIconLoadingDisabled = false,
                baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                usePasskeyDefaultIcon = false,
            )

        val expected = IconData.Local(iconRes = R.drawable.ic_globe)

        assertEquals(expected, actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return a IconData Local type if no valid uris are found and usePasskeyDefaultIcon true`() {
        val actual = listOf(
            LoginUriView(
                uri = "",
                match = UriMatchType.HOST,
                uriChecksum = null,
            ),
        )
            .toLoginIconData(
                isIconLoadingDisabled = false,
                baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                usePasskeyDefaultIcon = true,
            )

        val expected = IconData.Local(iconRes = R.drawable.ic_bw_passkey)

        assertEquals(expected, actual)
    }

    @Test
    fun `toLoginIconData should return a IconData Local type if an Android uri is detected`() {
        val actual = listOf(
            LoginUriView(
                uri = "androidapp://test.com",
                match = UriMatchType.HOST,
                uriChecksum = null,
            ),
        )
            .toLoginIconData(
                isIconLoadingDisabled = false,
                baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                usePasskeyDefaultIcon = false,
            )

        val expected = IconData.Local(iconRes = R.drawable.ic_android)

        assertEquals(expected, actual)
    }

    @Test
    fun `toLoginIconData should return a IconData Local type if an iOS uri is detected`() {
        val actual = listOf(
            LoginUriView(
                uri = "iosapp://test.com",
                match = UriMatchType.HOST,
                uriChecksum = null,
            ),
        )
            .toLoginIconData(
                isIconLoadingDisabled = false,
                baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
                usePasskeyDefaultIcon = false,
            )

        val expected = IconData.Local(iconRes = R.drawable.ic_ios)

        assertEquals(expected, actual)
    }

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
                    usePasskeyDefaultIcon = false,
                )

        val expected = IconData.Network(
            uri = "https://vault.bitwarden.com/icons/www.mockuri1.com/icon.png",
            fallbackIconRes = R.drawable.ic_globe,
        )

        assertEquals(expected, actual)

        unmockkStatic(Uri::class)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toLoginIconData should return IconData Network type if isIconLoadingDisabled is false and usePasskeyDefaultIcon`() {
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
                    usePasskeyDefaultIcon = true,
                )

        val expected = IconData.Network(
            uri = "https://vault.bitwarden.com/icons/www.mockuri1.com/icon.png",
            fallbackIconRes = R.drawable.ic_bw_passkey,
        )

        assertEquals(expected, actual)

        unmockkStatic(Uri::class)
    }

    @Test
    fun `toViewState should only count deleted items for the trash count`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1, isDeleted = true),
                createMockCipherView(number = 2, isDeleted = true),
                createMockCipherView(number = 3, isDeleted = false),
            ),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
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
                trashItemsCount = 2,
                totpItemsCount = 0,
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should show content with trashed items only`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1, isDeleted = true),
                createMockCipherView(number = 2, isDeleted = true),
            ),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 0,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                folderItems = listOf(),
                collectionItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 2,
                totpItemsCount = 0,
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState with over 100 no folder items should show no folder option`() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri1.com"
        val vaultData = VaultData(
            cipherViewList = List(100) {
                createMockCipherView(number = it, folderId = null, organizationUsesTotp = true)
            },
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 100,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                folderItems = listOf(
                    VaultState.ViewState.FolderItem(
                        id = null,
                        name = R.string.folder_none.asText(),
                        itemCount = 100,
                    ),
                ),
                collectionItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 100,
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
            ),
            actual,
        )
        unmockkStatic(Uri::class)
    }

    @Test
    fun `toViewState should properly filter nested items out`() {
        val vaultData = VaultData(
            listOf(createMockCipherView(number = 1)),
            collectionViewList = listOf(
                createMockCollectionView(1, "test"),
                createMockCollectionView(2, "test/test"),
                createMockCollectionView(3, "Collection/test"),
                createMockCollectionView(4, "test/Collection"),
                createMockCollectionView(5, "Collection"),
            ),
            folderViewList = listOf(
                FolderView("1", "test", clock.instant()),
                FolderView("2", "test/test", clock.instant()),
                FolderView("3", "test/", clock.instant()),
                FolderView("4", "test/test/test/", clock.instant()),
                FolderView("5", "Folder", clock.instant()),
            ),
            sendViewList = emptyList(),
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(),
                collectionItems = listOf(
                    VaultState.ViewState.CollectionItem(
                        id = "mockId-1",
                        name = "test",
                        itemCount = 1,
                    ),
                    VaultState.ViewState.CollectionItem(
                        id = "mockId-5",
                        name = "Collection",
                        itemCount = 0,
                    ),
                ),
                folderItems = listOf(
                    VaultState.ViewState.FolderItem(
                        id = "1",
                        name = "test".asText(),
                        itemCount = 0,
                    ),
                    VaultState.ViewState.FolderItem(
                        id = "3",
                        name = "test/".asText(),
                        itemCount = 0,
                    ),
                    VaultState.ViewState.FolderItem(
                        id = "5",
                        name = "Folder".asText(),
                        itemCount = 0,
                    ),

                    ),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 0,
                itemTypesCount = 4,
                sshKeyItemsCount = 0,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should exclude SSH keys if showSshKeys is false`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1),
                createMockCipherView(number = 2, cipherType = CipherType.SSH_KEY),
            ),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
            fido2CredentialAutofillViewList = null,
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = false,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                // Verify SSH key vault items are not counted when showSshKeys is false.
                sshKeyItemsCount = 0,
                favoriteItems = listOf(),
                collectionItems = listOf(),
                folderItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 0,
                // Verify item types count excludes CipherType.SSH_KEY when showSshKeys is false.
                itemTypesCount = 4,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should include SSH key vault items and type count if showSshKeys is true`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1, organizationId = null),
                createMockCipherView(number = 2, cipherType = CipherType.SSH_KEY),
            ),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
            fido2CredentialAutofillViewList = null,
        )

        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = true,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                // Verify SSH key vault items are counted
                sshKeyItemsCount = 1,
                favoriteItems = listOf(),
                collectionItems = listOf(),
                folderItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 1,
                // Verify item types count includes all CipherTypes when showSshKeys is true.
                itemTypesCount = CipherType.entries.size,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should transform SSH key vault items into correct vault item`() {
        val vaultData = VaultData(
            cipherViewList = listOf(
                createMockCipherView(number = 1, cipherType = CipherType.SSH_KEY, folderId = null),
                createMockCipherView(
                    number = 2,
                    cipherType = CipherType.SSH_KEY,
                    repromptType = CipherRepromptType.PASSWORD,
                    folderId = null,
                    sshKey = createMockSshKeyView(number = 1)
                        .copy(
                            publicKey = "publicKey",
                            privateKey = "privateKey",
                            fingerprint = "fingerprint",
                        ),
                ),
                createMockCipherView(
                    number = 3,
                    cipherType = CipherType.SSH_KEY,
                    folderId = null,
                    sshKey = null,
                ),
            ),
            collectionViewList = listOf(),
            folderViewList = listOf(),
            sendViewList = listOf(),
        )
        val actual = vaultData.toViewState(
            isPremium = true,
            isIconLoadingDisabled = false,
            baseIconUrl = Environment.Us.environmentUrlData.baseIconUrl,
            vaultFilterType = VaultFilterType.AllVaults,
            hasMasterPassword = true,
            showSshKeys = true,
            organizationPremiumStatusMap = emptyMap(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 0,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                sshKeyItemsCount = 3,
                favoriteItems = listOf(),
                collectionItems = listOf(),
                folderItems = listOf(),
                noFolderItems = listOf(
                    createMockSshKeyVaultItem(number = 1),
                    createMockSshKeyVaultItem(number = 2)
                        .copy(
                            publicKey = "publicKey".asText(),
                            privateKey = "privateKey".asText(),
                            fingerprint = "fingerprint".asText(),
                            shouldShowMasterPasswordReprompt = true,
                        ),
                    createMockSshKeyVaultItem(number = 3)
                        .copy(
                            publicKey = "".asText(),
                            privateKey = "".asText(),
                            fingerprint = "".asText(),
                        ),
                ),
                trashItemsCount = 0,
                totpItemsCount = 0,
                itemTypesCount = CipherType.entries.size,
            ),
            actual,
        )
    }
}

private fun createMockSshKeyVaultItem(number: Int): VaultState.ViewState.VaultItem.SshKey =
    VaultState.ViewState.VaultItem.SshKey(
        id = "mockId-$number",
        name = "mockName-$number".asText(),
        publicKey = "mockPublicKey-$number".asText(),
        privateKey = "mockPrivateKey-$number".asText(),
        fingerprint = "mockKeyFingerprint-$number".asText(),
        overflowOptions = listOf(
            ListingItemOverflowAction.VaultAction.ViewClick("mockId-$number"),
            ListingItemOverflowAction.VaultAction.EditClick("mockId-$number", true),
        ),
        startIcon = IconData.Local(iconRes = R.drawable.ic_ssh_key),
        startIconTestTag = "SshKeyCipherIcon",
        extraIconList = listOf(
            IconRes(
                iconRes = R.drawable.ic_collections,
                contentDescription = R.string.collections.asText(),
                testTag = "CipherInCollectionIcon",
            ),
            IconRes(
                iconRes = R.drawable.ic_paperclip,
                contentDescription = R.string.attachments.asText(),
                testTag = "CipherWithAttachmentsIcon",
            ),
        ),
        shouldShowMasterPasswordReprompt = false,
    )
