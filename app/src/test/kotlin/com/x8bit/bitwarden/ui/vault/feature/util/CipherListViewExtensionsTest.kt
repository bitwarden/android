package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.ui.platform.components.icon.model.IconData
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CopyableCipherFields
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockUriView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.model.VaultTrailingIcon
import com.x8bit.bitwarden.ui.vault.util.toSdkCipherType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CipherListViewExtensionsTest {

    @Test
    fun `toOverflowActions should return all actions for a login cipher when a user has premium`() {
        val loginListView = createMockLoginListView(
            number = 1,
            username = username,
            totp = totpCode,
            uris = listOf(
                createMockUriView(
                    number = 1,
                    uri = uri,
                ),
            ),
        )
        val cipher = createMockCipherListView(number = 1).copy(
            id = id,
            type = CipherListViewType.Login(loginListView),
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
                    cipherId = id,
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherType.LOGIN,
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherType.LOGIN,
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.LaunchClick(url = uri),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should not return TOTP action when a user does not have premium`() {
        val type = CipherListViewType.Login(
            createMockLoginListView(number = 1).copy(
                username = username,
                uris = listOf(createMockUriView(number = 1).copy(uri = uri)),
            ),
        )
        val cipher = createMockCipherListView(
            number = 1,
            id = id,
            type = type,
        )

        val result = cipher.toOverflowActions(hasMasterPassword = false, isPremiumUser = false)

        assertTrue(
            result.none { it is ListingItemOverflowAction.VaultAction.CopyTotpClick },
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toOverflowActions should return the correct actions when viewPassword is false for a login cipher`() {
        val loginListView = createMockLoginListView(number = 1).copy(
            username = username,
            uris = listOf(createMockUriView(number = 1).copy(uri = uri)),
        )
        val cipher = createMockCipherListView(number = 1).copy(
            id = id,
            type = CipherListViewType.Login(loginListView),
            viewPassword = false,
        )
        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.CopyUsernameClick(username = username),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherType.LOGIN,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherType.LOGIN,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.LaunchClick(url = uri),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a login cipher`() {
        val loginListView = createMockLoginListView(
            number = 1,
            uris = emptyList(),
            username = "",
            totp = null,
        )
        val cipher = createMockCipherListView(
            number = 1,
            isDeleted = true,
            type = CipherListViewType.Login(loginListView),
            id = id,
            copyableFields = emptyList(),
        )

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherType.LOGIN,
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
            id = id,
            copyableFields = listOf(
                CopyableCipherFields.CARD_NUMBER,
                CopyableCipherFields.CARD_SECURITY_CODE,
            ),
        )

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
                    cipherType = cipher.type.toSdkCipherType(),
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = cipher.type.toSdkCipherType(),
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
                    cipherType = cipher.type.toSdkCipherType(),
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
                    cipherType = CipherType.IDENTITY,
                    requiresPasswordReprompt = false,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherType.IDENTITY,
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
                    cipherType = CipherType.IDENTITY,
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
            id = id,
            copyableFields = listOf(CopyableCipherFields.SECURE_NOTES),
        )

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.CopyNoteClick(
                    cipherId = id,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.ViewClick(
                    cipherId = id,
                    cipherType = CipherType.SECURE_NOTE,
                    requiresPasswordReprompt = true,
                ),
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherType.SECURE_NOTE,
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
                    cipherType = CipherType.SECURE_NOTE,
                    requiresPasswordReprompt = false,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should not return Edit action when cipher cannot be edited`() {
        val type = CipherListViewType.Login(createMockLoginListView(number = 1))
        val cipher = createMockCipherListView(
            number = 1,
            id = id,
            isDeleted = false,
            edit = false,
            type = type,
        )
        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertTrue(
            result.none { it is ListingItemOverflowAction.VaultAction.EditClick },
        )
    }

    @Test
    fun `toOverflowActions should return Edit action when cipher can be edited`() {
        val loginListView = createMockLoginListView(
            number = 1,
            username = "",
            uris = emptyList(),
            totp = null,
        )
        val cipher = createMockCipherListView(
            number = 1,
            id = id,
            isDeleted = false,
            edit = true,
            type = CipherListViewType.Login(loginListView),
        )

        val result = cipher.toOverflowActions(hasMasterPassword = true, isPremiumUser = false)

        assertTrue(
            result.contains(
                ListingItemOverflowAction.VaultAction.EditClick(
                    cipherId = id,
                    cipherType = CipherType.LOGIN,
                    requiresPasswordReprompt = true,
                ),
            ),
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
