package com.x8bit.bitwarden.ui.vault.feature.item.util

import com.bitwarden.core.CipherType
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import com.x8bit.bitwarden.ui.vault.feature.item.model.TotpCodeItemData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class CipherViewExtensionsTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content without master password reprompt`() {
        val cipherView = createCipherView(type = CipherType.LOGIN, isEmpty = false)
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = false,
            totpCodeItemData = TotpCodeItemData(
                periodSeconds = 30,
                timeLeftSeconds = 15,
                verificationCode = "123456",
                totpCode = "testCode",
            ),
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = true).copy(
                    currentCipher = cipherView,
                    requiresReprompt = false,
                ),
                type = createLoginContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content with premium`() {
        val cipherView = createCipherView(type = CipherType.LOGIN, isEmpty = false)
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = TotpCodeItemData(
                periodSeconds = 30,
                timeLeftSeconds = 15,
                verificationCode = "123456",
                totpCode = "testCode",
            ),
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = true)
                    .copy(currentCipher = cipherView),
                type = createLoginContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content without premium`() {
        val isPremiumUser = false
        val cipherView = createCipherView(type = CipherType.LOGIN, isEmpty = false)
        val viewState = cipherView.toViewState(
            isPremiumUser = isPremiumUser,
            hasMasterPassword = true,
            totpCodeItemData = TotpCodeItemData(
                periodSeconds = 30,
                timeLeftSeconds = 15,
                verificationCode = "123456",
                totpCode = "testCode",
            ),
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = isPremiumUser)
                    .copy(currentCipher = cipherView),
                type = createLoginContent(isEmpty = false).copy(isPremiumUser = isPremiumUser),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform empty CipherView into ViewState Login Content`() {
        val cipherView = createCipherView(type = CipherType.LOGIN, isEmpty = true)
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = null,
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = true, isPremiumUser = true).copy(
                    currentCipher = cipherView,
                ),
                type = createLoginContent(isEmpty = true),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Identity Content`() {
        val cipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = null,
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = true)
                    .copy(currentCipher = cipherView),
                type = createIdentityContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform empty CipherView into ViewState Identity Content`() {
        val cipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = true)
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = null,
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = true, isPremiumUser = true)
                    .copy(currentCipher = cipherView),
                type = createIdentityContent(isEmpty = true),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform CipherView with odd naming into ViewState Identity Content`() {
        val initialCipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val cipherView = initialCipherView
            .copy(
                identity = initialCipherView.identity?.copy(
                    title = "MX",
                    firstName = null,
                    middleName = "middleName",
                    lastName = null,
                ),
            )
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = null,
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = true)
                    .copy(currentCipher = cipherView),
                type = createIdentityContent(
                    isEmpty = false,
                    identityName = "Mx middleName",
                ),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform CipherView with odd address into ViewState Identity Content`() {
        val initialCipherView = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val cipherView = initialCipherView.copy(
            identity = initialCipherView.identity?.copy(
                address1 = null,
                address2 = null,
                address3 = "address3",
                city = null,
                state = "state",
                postalCode = null,
                country = null,
            ),
        )
        val result = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = null,
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = true).copy(
                    currentCipher = cipherView.copy(
                        identity = cipherView.identity?.copy(
                            address1 = null,
                            address2 = null,
                            address3 = "address3",
                            city = null,
                            state = "state",
                            postalCode = null,
                            country = null,
                        ),
                    ),
                ),
                type = createIdentityContent(
                    isEmpty = false,
                    address = """
                        address3
                        -, state, -
                    """.trimIndent(),
                ),
            ),
            result,
        )
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Secure Note Content`() {
        val cipherView = createCipherView(type = CipherType.SECURE_NOTE, isEmpty = false)
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = null,
            clock = fixedClock,
        )

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false, isPremiumUser = true)
                    .copy(currentCipher = cipherView),
                type = VaultItemState.ViewState.Content.ItemType.SecureNote,
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform empty Secure Note CipherView into ViewState Secure Note Content`() {
        val cipherView = createCipherView(type = CipherType.SECURE_NOTE, isEmpty = true)
        val viewState = cipherView.toViewState(
            isPremiumUser = true,
            hasMasterPassword = true,
            totpCodeItemData = null,
            clock = fixedClock,
        )

        val expectedState = VaultItemState.ViewState.Content(
            common = createCommonContent(isEmpty = true, isPremiumUser = true)
                .copy(currentCipher = cipherView),
            type = VaultItemState.ViewState.Content.ItemType.SecureNote,
        )

        assertEquals(expectedState, viewState)
    }
}
