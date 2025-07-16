package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.vault.CipherListViewType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockUriView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.model.VaultTrailingIcon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CipherListViewExtensionsTest {

    @Test
    fun `toOverflowActions should return all actions for a login cipher when a user has premium`() {
        val cipher = createMockCipherListView(number = 1).copy(
            id = id,
            type = CipherListViewType.Login(
                createMockLoginListView(number = 1).copy(
                    username = username,
                    uris = listOf(createMockUriView(number = 1).copy(uri = uri)),
                ),
            ),
        )

        val result = cipher.toOverflowActions(hasMasterPassword = false, isPremiumUser = true)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.CopyUsernameClick(username = username),
                ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                    requiresPasswordReprompt = false,
                    cipherId = id,
                ),
                ListingItemOverflowAction.VaultAction.CopyTotpClick(
                    totpCode = totpCode,
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.LaunchClick(url = uri),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should not return TOTP action when a user does not have premium`() {
        val cipher = createMockCipherListView(number = 1).copy(
            id = id,
            type = CipherListViewType.Login(
                createMockLoginListView(number = 1).copy(
                    username = username,
                    uris = listOf(createMockUriView(number = 1).copy(uri = uri)),
                ),
            ),
        )

        val result = cipher.toOverflowActions(hasMasterPassword = false, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.CopyUsernameClick(username = username),
                ListingItemOverflowAction.VaultAction.CopyPasswordClick(
                    requiresPasswordReprompt = false,
                    cipherId = id,
                ),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.LaunchClick(url = uri),
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toOverflowActions should return the correct actions when viewPassword is false for a login cipher`() {
        val cipher = createMockCipherListView(number = 1).copy(
            id = id,
            type = CipherListViewType.Login(
                createMockLoginListView(number = 1).copy(
                    username = username,
                    uris = listOf(createMockUriView(number = 1).copy(uri = uri)),
                ),
            ),
            viewPassword = false,
        )
        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.CopyUsernameClick(username = username),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.LaunchClick(url = uri),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a login cipher`() {
        val cipher = createMockCipherListView(
            number = 1,
            isDeleted = true,
            type = CipherListViewType.Login(
                createMockLoginListView(
                    number = 1,
                    uris = emptyList(),
                    username = "",
                    totp = null,
                ),
            ),
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return all actions for a card cipher`() {
        val cipher = createMockCipherListView(
            number = 1,
            type = CipherListViewType.Card(
                createMockCardListView(number = 1),
            ),
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.CopyNumberClick(
                    cipherId = id,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                    cipherId = id,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = cipher.type,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = cipher.type,
                    requiresPasswordReprompt = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a card cipher`() {
        val cipher = createMockCipherListView(
            number = 1,
            isDeleted = true,
            type = CipherListViewType.Card(createMockCardListView(number = 1)),
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = false, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = cipher.type,
                    requiresPasswordReprompt = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return all actions for a identity cipher`() {
        val cipher = createMockCipherListView(
            number = 1,
            type = CipherListViewType.Identity,
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = false, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Identity,
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Identity,
                    requiresPasswordReprompt = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a identity cipher`() {
        val cipher = createMockCipherListView(
            number = 1,
            isDeleted = true,
            type = CipherListViewType.Identity,
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Identity,
                    requiresPasswordReprompt = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return all actions for a secure note cipher`() {
        val cipher = createMockCipherListView(
            number = 1,
            type = CipherListViewType.SecureNote,
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.CopyNoteClick(
                    cipherId = id,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.SecureNote,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherListViewType.SecureNote,
                    requiresPasswordReprompt = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a secure note cipher`() {
        val cipher = createMockCipherListView(
            number = 1,
            isDeleted = true,
            type = CipherListViewType.SecureNote,
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = false, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.SecureNote,
                    requiresPasswordReprompt = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should not return Edit action when cipher cannot be edited`() {
        val cipher = createMockCipherListView(
            number = 1,
            isDeleted = false,
            edit = false,
            type = CipherListViewType.Login(
                createMockLoginListView(number = 1)
                    .copy(
                        username = "",
                        uris = emptyList(),
                        totp = null,
                    ),
            ),
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return Edit action when cipher can be edited`() {
        val cipher = createMockCipherListView(
            number = 1,
            isDeleted = false,
            edit = true,
            type = CipherListViewType.Login(
                createMockLoginListView(number = 1)
                    .copy(
                        username = "",
                        uris = emptyList(),
                        totp = null,
                    ),
            ),
        )
            .copy(id = id)

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherListViewType.Login(createMockLoginListView(number = 1)),
                    requiresPasswordReprompt = true,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toTrailingIcons should return collection icon if collectionId is not empty`() {
        val cipher = createMockCipherListView(1).copy(
            organizationId = null,
            attachments = 0U,
        )

        val expected = listOf(VaultTrailingIcon.COLLECTION).map {
            IconData.Local(
                iconRes = it.iconRes,
                contentDescription = it.contentDescription,
                testTag = it.testTag,
            )
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return collection icon if organizationId is not null`() {
        val cipher = createMockCipherListView(1).copy(
            collectionIds = listOf(),
            attachments = 0U,
        )

        val expected = listOf(VaultTrailingIcon.COLLECTION).map {
            IconData.Local(
                iconRes = it.iconRes,
                contentDescription = it.contentDescription,
                testTag = it.testTag,
            )
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return attachment icon if attachments is not null`() {
        val cipher = createMockCipherListView(1).copy(
            collectionIds = listOf(),
            organizationId = null,
        )

        val expected = listOf(VaultTrailingIcon.ATTACHMENT).map {
            IconData.Local(
                iconRes = it.iconRes,
                contentDescription = it.contentDescription,
                testTag = it.testTag,
            )
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return trailing icons if cipher has correct data`() {
        val cipher = createMockCipherListView(1)

        val expected = listOf(
            VaultTrailingIcon.COLLECTION,
            VaultTrailingIcon.ATTACHMENT,
        ).map {
            IconData.Local(
                iconRes = it.iconRes,
                contentDescription = it.contentDescription,
                testTag = it.testTag,
            )
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return empty list if no data requires an extra icon`() {
        val cipher = createMockCipherListView(1).copy(
            collectionIds = listOf(),
            organizationId = null,
            attachments = 0U,
        )

        val expected = listOf<IconData>()

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }
}

private const val id = "mockId-1"
private const val username = "Bitwarden"
private const val totpCode = "mockTotp-1"
private const val uri = "www.test.com"
