package com.x8bit.bitwarden.data.autofill.util

import com.bitwarden.vault.CipherListViewType
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCardListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertNull

class CipherListViewExtensionsTest {

    @Test
    fun `login should return LoginListView when type is Login`() {
        val cipherListView = createMockCipherListView(
            number = 1,
            type = CipherListViewType.Login(createMockLoginListView(1)),
        )
        val loginListView = cipherListView.login
        assertNotNull(loginListView)
    }

    @Test
    fun `login should return null when type is not Login`() {
        val cipherListViews = listOf(
            createMockCipherListView(number = 1, type = CipherListViewType.SecureNote),
            createMockCipherListView(
                number = 2,
                type = CipherListViewType.Card(createMockCardListView(number = 2)),
            ),
            createMockCipherListView(number = 3, type = CipherListViewType.SshKey),
            createMockCipherListView(number = 4, type = CipherListViewType.Identity),
        )
        cipherListViews.forEach { assertNull(it.login) }
    }

    @Test
    fun `card should return CardListView when type is Card`() {
        val cipherListView = createMockCipherListView(
            number = 1,
            type = CipherListViewType.Card(createMockCardListView(number = 1)),
        )
        val cardListView = cipherListView.card
        assertNotNull(cardListView)
    }

    @Test
    fun `card should return null when type is not Card`() {
        listOf(
            createMockCipherListView(
                number = 1,
                type = CipherListViewType.Login(createMockLoginListView(1)),
            ),
            createMockCipherListView(number = 2, type = CipherListViewType.SecureNote),
            createMockCipherListView(number = 3, type = CipherListViewType.SshKey),
            createMockCipherListView(number = 4, type = CipherListViewType.Identity),
        )
            .forEach { assertNull(it.card) }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isActiveWithFido2Credentials should return true when Fido2 credentials are present and cipher is not deleted`() {
        val cipherListView = createMockCipherListView(
            number = 1,
            type = CipherListViewType.Login(
                createMockLoginListView(
                    number = 1,
                    hasFido2 = true,
                ),
            ),
        )
        assertTrue(cipherListView.isActiveWithFido2Credentials)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isActiveWithFido2Credentials should return false when Fido2 credentials are not present`() {
        val cipherListView = createMockCipherListView(
            number = 1,
            type = CipherListViewType.Login(
                createMockLoginListView(
                    number = 1,
                    hasFido2 = false,
                ),
            ),
        )
        assertFalse(cipherListView.isActiveWithFido2Credentials)
    }

    @Test
    fun `isActiveWithFido2Credentials should return false when cipher is deleted`() {
        val cipherListView = createMockCipherListView(
            number = 1,
            type = CipherListViewType.Login(
                createMockLoginListView(
                    number = 1,
                    hasFido2 = true,
                ),
            ),
            isDeleted = true,
        )
        assertFalse(cipherListView.isActiveWithFido2Credentials)
    }

    @Test
    fun `isActiveWithFido2Credentials should return false when cipher type is not login`() {
        val cipherListView = createMockCipherListView(
            number = 1,
            type = CipherListViewType.SecureNote,
        )
        assertFalse(cipherListView.isActiveWithFido2Credentials)
    }
}
