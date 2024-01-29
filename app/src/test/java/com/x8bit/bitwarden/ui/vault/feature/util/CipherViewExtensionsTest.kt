package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.core.CipherType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockIdentityView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSecureNoteView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockUriView
import com.x8bit.bitwarden.ui.platform.components.model.IconRes
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
import com.x8bit.bitwarden.ui.vault.model.VaultTrailingIcon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class CipherViewExtensionsTest {

    @Test
    fun `toOverflowActions should return all actions for a login cipher`() {
        val id = "mockId-1"
        val username = "Bitwarden"
        val password = "password"
        val uri = "www.test.com"
        val cipher = createMockCipherView(number = 1, cipherType = CipherType.LOGIN).copy(
            id = id,
            login = createMockLoginView(number = 1).copy(
                username = username,
                password = password,
                uris = listOf(createMockUriView(number = 1).copy(uri = uri)),
            ),
        )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id),
                ListingItemOverflowAction.VaultAction.EditClick(cipherId = id),
                ListingItemOverflowAction.VaultAction.CopyUsernameClick(username = username),
                ListingItemOverflowAction.VaultAction.CopyPasswordClick(password = password),
                ListingItemOverflowAction.VaultAction.LaunchClick(url = uri),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a login cipher`() {
        val id = "mockId-1"
        val cipher = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.LOGIN,
        )
            .copy(
                id = id,
                login = createMockLoginView(number = 1).copy(
                    username = null,
                    password = null,
                    uris = null,
                ),
            )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id)),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return all actions for a card cipher`() {
        val id = "mockId-1"
        val number = "1322-2414-7634-2354"
        val securityCode = "123"
        val cipher = createMockCipherView(number = 1, cipherType = CipherType.CARD).copy(
            id = id,
            card = createMockCardView(number = 1).copy(
                number = number,
                code = securityCode,
            ),
        )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id),
                ListingItemOverflowAction.VaultAction.EditClick(cipherId = id),
                ListingItemOverflowAction.VaultAction.CopyNumberClick(number = number),
                ListingItemOverflowAction.VaultAction.CopySecurityCodeClick(
                    securityCode = securityCode,
                ),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a card cipher`() {
        val id = "mockId-1"
        val cipher = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.CARD,
        )
            .copy(
                id = id,
                card = createMockCardView(number = 1).copy(
                    number = null,
                    code = null,
                ),
            )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id)),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return all actions for a identity cipher`() {
        val id = "mockId-1"
        val cipher = createMockCipherView(number = 1, cipherType = CipherType.IDENTITY).copy(
            id = id,
            identity = createMockIdentityView(number = 1),
        )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id),
                ListingItemOverflowAction.VaultAction.EditClick(cipherId = id),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a identity cipher`() {
        val id = "mockId-1"
        val cipher = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.IDENTITY,
        )
            .copy(
                id = id,
                identity = createMockIdentityView(number = 1),
            )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id)),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return all actions for a secure note cipher`() {
        val id = "mockId-1"
        val notes = "so secure"
        val cipher = createMockCipherView(number = 1, cipherType = CipherType.SECURE_NOTE).copy(
            id = id,
            secureNote = createMockSecureNoteView(),
            notes = notes,
        )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(
                ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id),
                ListingItemOverflowAction.VaultAction.EditClick(cipherId = id),
                ListingItemOverflowAction.VaultAction.CopyNoteClick(notes = notes),
            ),
            result,
        )
    }

    @Test
    fun `toOverflowActions should return minimum actions for a secure note cipher`() {
        val id = "mockId-1"
        val cipher = createMockCipherView(
            number = 1,
            isDeleted = true,
            cipherType = CipherType.SECURE_NOTE,
        )
            .copy(
                id = id,
                secureNote = createMockSecureNoteView(),
                notes = null,
            )

        val result = cipher.toOverflowActions()

        assertEquals(
            listOf(ListingItemOverflowAction.VaultAction.ViewClick(cipherId = id)),
            result,
        )
    }

    @Test
    fun `toTrailingIcons should return collection icon if collectionId is not empty`() {
        val cipher = createMockCipherView(1).copy(
            organizationId = null,
            attachments = null,
        )

        val expected = listOf(VaultTrailingIcon.COLLECTION).map {
            IconRes(iconRes = it.iconRes, contentDescription = it.contentDescription)
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return collection icon if organizationId is not null`() {
        val cipher = createMockCipherView(1).copy(
            collectionIds = listOf(),
            attachments = null,
        )

        val expected = listOf(VaultTrailingIcon.COLLECTION).map {
            IconRes(iconRes = it.iconRes, contentDescription = it.contentDescription)
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return attachment icon if attachments is not null`() {
        val cipher = createMockCipherView(1).copy(
            collectionIds = listOf(),
            organizationId = null,
        )

        val expected = listOf(VaultTrailingIcon.ATTACHMENT).map {
            IconRes(iconRes = it.iconRes, contentDescription = it.contentDescription)
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return trailing icons if cipher has correct data`() {
        val cipher = createMockCipherView(1)

        val expected = listOf(
            VaultTrailingIcon.COLLECTION,
            VaultTrailingIcon.ATTACHMENT,
        ).map {
            IconRes(iconRes = it.iconRes, contentDescription = it.contentDescription)
        }

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }

    @Test
    fun `toTrailingIcons should return empty list if no data requires an extra icon`() {
        val cipher = createMockCipherView(1).copy(
            collectionIds = listOf(),
            organizationId = null,
            attachments = null,
        )

        val expected = listOf<IconRes>()

        val result = cipher.toLabelIcons()

        assertEquals(expected, result)
    }
}
