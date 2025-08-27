package com.x8bit.bitwarden.ui.vault.feature.vault.util

import android.net.Uri
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.data.repository.util.baseIconUrl
import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.ui.platform.resource.BitwardenDrawable
import com.bitwarden.ui.platform.resource.BitwardenString
import com.bitwarden.ui.util.asText
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.FolderView
import com.bitwarden.vault.LoginUriView
import com.bitwarden.vault.UriMatchType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCollectionView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockDecryptCipherListResult
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockFolderView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSendView
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import com.x8bit.bitwarden.data.vault.repository.util.toFailureCipherListView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.feature.util.toLabelIcons
import com.x8bit.bitwarden.ui.vault.feature.util.toOverflowActions
import com.x8bit.bitwarden.ui.vault.feature.vault.VaultState
import com.x8bit.bitwarden.ui.vault.feature.vault.model.VaultFilterType
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.collections.immutable.persistentListOf
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
        val mockCipher = createMockSdkCipher(number = 2).copy(
            folderId = null,
            favorite = true,
            deletedDate = null,
        )
        val vaultData = VaultData(
            decryptCipherListResult = DecryptCipherListResult(
                successes = listOf(createMockCipherListView(number = 1)),
                failures = listOf(mockCipher),
            ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 2,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                favoriteItems = listOf(
                    VaultState.ViewState.VaultItem.Login(
                        id = "mockId-2",
                        name = BitwardenString.error_cannot_decrypt.asText(),
                        startIcon = IconData.Local(iconRes = BitwardenDrawable.ic_globe),
                        startIconTestTag = "LoginCipherIcon",
                        extraIconList = mockCipher.toFailureCipherListView().toLabelIcons(),
                        overflowOptions = emptyList(),
                        shouldShowMasterPasswordReprompt = false,
                        username = null,
                        hasDecryptionError = true,
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
                    VaultState.ViewState.FolderItem(
                        id = null,
                        name = BitwardenString.folder_none.asText(),
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
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState for MyVault should transform full VaultData into ViewState Content with filtering of non-user data`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(number = 1).copy(organizationId = null),
                    createMockCipherListView(number = 2),
                ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState for OrganizationVault should transform full VaultData into ViewState Content with filtering of non-organization data`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(number = 1),
                    createMockCipherListView(number = 2),
                ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                    VaultState.ViewState.FolderItem(
                        id = null,
                        name = BitwardenString.folder_none.asText(),
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
                totpItemsCount = 1,
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should transform empty VaultData into ViewState NoItems`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = emptyList(),
            ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
        )

        assertEquals(
            VaultState.ViewState.NoItems,
            actual,
        )
    }

    @Test
    fun `toViewState should not transform ciphers with no ID into ViewState items`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(createMockCipherListView(number = 1).copy(id = null)),
            ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(createMockCipherListView(number = 1)),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should return 0 for totpItemsCount if user does not have premium and has any totp items`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(createMockCipherListView(number = 1)),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should return 1 for totpItemsCount if user does not have premium and has at least 1 totp items with org TOTP true`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(number = 1).copy(organizationUseTotp = true),
                ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should omit non org related totp codes when user does not have premium`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(number = 1).copy(organizationUseTotp = true),
                    createMockCipherListView(number = 2),
                ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
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

        val expected = IconData.Local(iconRes = BitwardenDrawable.ic_globe)

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

        val expected = IconData.Local(iconRes = BitwardenDrawable.ic_bw_passkey)

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

        val expected = IconData.Local(iconRes = BitwardenDrawable.ic_globe)

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

        val expected = IconData.Local(iconRes = BitwardenDrawable.ic_bw_passkey)

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

        val expected = IconData.Local(iconRes = BitwardenDrawable.ic_android)

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

        val expected = IconData.Local(iconRes = BitwardenDrawable.ic_ios)

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
            uri = "https://icons.bitwarden.net/www.mockuri1.com/icon.png",
            fallbackIconRes = BitwardenDrawable.ic_globe,
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
            uri = "https://icons.bitwarden.net/www.mockuri1.com/icon.png",
            fallbackIconRes = BitwardenDrawable.ic_bw_passkey,
        )

        assertEquals(expected, actual)

        unmockkStatic(Uri::class)
    }

    @Test
    fun `toViewState should only count deleted items for the trash count`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(number = 1, isDeleted = true),
                    createMockCipherListView(number = 2, isDeleted = true),
                    createMockCipherListView(number = 3, isDeleted = false),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                totpItemsCount = 1,
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should show content with trashed items only`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(number = 1, isDeleted = true),
                    createMockCipherListView(number = 2, isDeleted = true),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
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
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = List(100) {
                    createMockCipherListView(number = it, folderId = null)
                },
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                        name = BitwardenString.folder_none.asText(),
                        itemCount = 100,
                    ),
                ),
                collectionItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 100,
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
        unmockkStatic(Uri::class)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState with under 100 no folder items and no collections should not show no folder option`() {
        mockkStatic(Uri::class)
        val uriMock = mockk<Uri>()
        every { Uri.parse(any()) } returns uriMock
        every { uriMock.host } returns "www.mockuri1.com"
        val mockCipher = createMockCipherListView(number = 1, folderId = null)
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(mockCipher),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                noFolderItems = listOf(
                    VaultState.ViewState.VaultItem.Login(
                        id = "mockId-1",
                        name = mockCipher.name.asText(),
                        startIcon = IconData.Network(
                            uri = "https://icons.bitwarden.net/www.mockuri1.com/icon.png",
                            fallbackIconRes = BitwardenDrawable.ic_globe,
                        ),
                        startIconTestTag = "LoginCipherIcon",
                        extraIconList = mockCipher.toLabelIcons(),
                        overflowOptions = mockCipher.toOverflowActions(
                            hasMasterPassword = true,
                            isPremiumUser = true,
                        ),
                        shouldShowMasterPasswordReprompt = false,
                        username = "mockUsername-1".asText(),
                        hasDecryptionError = false,
                    ),
                ),
                trashItemsCount = 0,
                totpItemsCount = 1,
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
        unmockkStatic(Uri::class)
    }

    @Test
    fun `toViewState should properly filter nested items out`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(createMockCipherListView(number = 1)),
            ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                    VaultState.ViewState.FolderItem(
                        id = null,
                        name = BitwardenString.folder_none.asText(),
                        itemCount = 0,
                    ),
                ),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 1,
                itemTypesCount = 5,
                sshKeyItemsCount = 0,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should excluded card vault items and adjust type count for ciphers with orgId in restrictItemTypesPolicyOrgIds and set showCardGroup to true if there are remaining cards`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(
                        number = 1,
                        type = CipherListViewType.Card(v1 = createMockCardListView(number = 1)),
                    ),
                    createMockCipherListView(
                        number = 2,
                        organizationId = "restrict_item_type_policy_id",
                        type = CipherListViewType.Card(v1 = createMockCardListView(number = 2)),
                    ),
                    createMockCipherListView(
                        number = 3,
                        organizationId = "another_id",
                        type = CipherListViewType.Card(v1 = createMockCardListView(number = 3)),
                    ),
                    createMockCipherListView(
                        number = 4,
                        organizationId = null,
                        type = CipherListViewType.Card(v1 = createMockCardListView(number = 4)),
                    ),
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
            restrictItemTypesPolicyOrgIds = listOf("restrict_item_type_policy_id"),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 0,
                cardItemsCount = 2,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                sshKeyItemsCount = 0,
                favoriteItems = listOf(),
                collectionItems = listOf(),
                folderItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 0,
                itemTypesCount = CipherType.entries.size,
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should excluded card vault items for ciphers with orgId in restrictItemTypesPolicyOrgIds and set showCardGroup to false if there are no remaining cards`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(
                        number = 1,
                        organizationId = "restrict_item_type_policy_id",
                        type = CipherListViewType.Login(v1 = createMockLoginListView(number = 1)),
                    ),
                    createMockCipherListView(
                        number = 2,
                        organizationId = "restrict_item_type_policy_id",
                        type = CipherListViewType.Card(v1 = createMockCardListView(number = 2)),
                    ),
                    createMockCipherListView(
                        number = 3,
                        organizationId = "restrict_item_type_policy_id",
                        type = CipherListViewType.Card(v1 = createMockCardListView(number = 3)),
                    ),
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
            restrictItemTypesPolicyOrgIds = listOf("restrict_item_type_policy_id"),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 1,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                sshKeyItemsCount = 0,
                favoriteItems = listOf(),
                collectionItems = listOf(),
                folderItems = listOf(),
                noFolderItems = listOf(),
                trashItemsCount = 0,
                totpItemsCount = 1,
                itemTypesCount = CipherType.entries.size,
                showCardGroup = false,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should include SSH key vault items and type count`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(number = 1),
                    createMockCipherListView(number = 2, type = CipherListViewType.SshKey),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
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
                showCardGroup = true,
            ),
            actual,
        )
    }

    @Test
    fun `toViewState should transform SSH key vault items into correct vault item`() {
        val vaultData = VaultData(
            decryptCipherListResult = createMockDecryptCipherListResult(
                number = 1,
                successes = listOf(
                    createMockCipherListView(
                        number = 1,
                        type = CipherListViewType.SshKey,
                        folderId = null,
                        favorite = true,
                    ),
                    createMockCipherListView(
                        number = 2,
                        type = CipherListViewType.SshKey,
                        reprompt = CipherRepromptType.PASSWORD,
                        folderId = null,
                    ),
                    createMockCipherListView(
                        number = 3,
                        type = CipherListViewType.SshKey,
                        folderId = null,
                    ),
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
            restrictItemTypesPolicyOrgIds = emptyList(),
        )

        assertEquals(
            VaultState.ViewState.Content(
                loginItemsCount = 0,
                cardItemsCount = 0,
                identityItemsCount = 0,
                secureNoteItemsCount = 0,
                sshKeyItemsCount = 3,
                favoriteItems = listOf(createMockSshKeyVaultItem(number = 1)),
                collectionItems = listOf(),
                folderItems = listOf(),
                noFolderItems = listOf(
                    createMockSshKeyVaultItem(number = 1),
                    createMockSshKeyVaultItem(number = 2)
                        .copy(shouldShowMasterPasswordReprompt = true),
                    createMockSshKeyVaultItem(number = 3),
                ),
                trashItemsCount = 0,
                totpItemsCount = 0,
                itemTypesCount = CipherType.entries.size,
                showCardGroup = true,
            ),
            actual,
        )
    }
}

private fun createMockSshKeyVaultItem(number: Int): VaultState.ViewState.VaultItem.SshKey =
    VaultState.ViewState.VaultItem.SshKey(
        id = "mockId-$number",
        name = "mockName-$number".asText(),
        overflowOptions = listOf(
            ListingItemOverflowAction.VaultAction.ViewClick(
                cipherId = "mockId-$number",
                cipherType = CipherType.SSH_KEY,
                requiresPasswordReprompt = true,
            ),
            ListingItemOverflowAction.VaultAction.EditClick(
                cipherId = "mockId-$number",
                cipherType = CipherType.SSH_KEY,
                requiresPasswordReprompt = true,
            ),
        ),
        startIcon = IconData.Local(iconRes = BitwardenDrawable.ic_ssh_key),
        startIconTestTag = "SshKeyCipherIcon",
        extraIconList = persistentListOf(
            IconData.Local(
                iconRes = BitwardenDrawable.ic_collections,
                contentDescription = BitwardenString.collections.asText(),
                testTag = "CipherInCollectionIcon",
            ),
            IconData.Local(
                iconRes = BitwardenDrawable.ic_paperclip,
                contentDescription = BitwardenString.attachments.asText(),
                testTag = "CipherWithAttachmentsIcon",
            ),
        ),
        shouldShowMasterPasswordReprompt = false,
        hasDecryptionError = false,
    )
