package com.x8bit.bitwarden.ui.vault.feature.util

import com.bitwarden.core.CipherType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockIdentityView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSecureNoteView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockUriView
import com.x8bit.bitwarden.ui.vault.feature.itemlisting.model.ListingItemOverflowAction
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
}
