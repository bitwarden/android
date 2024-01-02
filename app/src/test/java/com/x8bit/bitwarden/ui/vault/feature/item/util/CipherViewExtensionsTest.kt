package com.x8bit.bitwarden.ui.vault.feature.item.util

import com.bitwarden.core.CipherType
import com.x8bit.bitwarden.ui.vault.feature.item.VaultItemState
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.TimeZone

class CipherViewExtensionsTest {

    @BeforeEach
    fun setup() {
        // Setting the timezone so the tests pass consistently no matter the environment.
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @AfterEach
    fun tearDown() {
        // Clearing the timezone after the test.
        TimeZone.setDefault(null)
    }

    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content with premium`() {
        val viewState = createCipherView(type = CipherType.LOGIN, isEmpty = false)
            .toViewState(isPremiumUser = true)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false),
                type = createLoginContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Login Content without premium`() {
        val isPremiumUser = false
        val viewState = createCipherView(type = CipherType.LOGIN, isEmpty = false)
            .toViewState(isPremiumUser = isPremiumUser)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false).copy(isPremiumUser = isPremiumUser),
                type = createLoginContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform empty CipherView into ViewState Login Content`() {
        val viewState = createCipherView(type = CipherType.LOGIN, isEmpty = true)
            .toViewState(isPremiumUser = true)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = true),
                type = createLoginContent(isEmpty = true),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Identity Content with premium`() {
        val viewState = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
            .toViewState(isPremiumUser = true)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false),
                type = createIdentityContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Identity Content without premium`() {
        val isPremiumUser = false
        val viewState = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
            .toViewState(isPremiumUser = isPremiumUser)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false).copy(isPremiumUser = isPremiumUser),
                type = createIdentityContent(isEmpty = false),
            ),
            viewState,
        )
    }

    @Test
    fun `toViewState should transform empty CipherView into ViewState Identity Content`() {
        val viewState = createCipherView(type = CipherType.IDENTITY, isEmpty = true)
            .toViewState(isPremiumUser = true)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = true),
                type = createIdentityContent(isEmpty = true),
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform CipherView with odd naming into ViewState Identity Content`() {
        val viewState = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val result = viewState
            .copy(
                identity = viewState.identity?.copy(
                    title = "MX",
                    firstName = null,
                    middleName = "middleName",
                    lastName = null,
                ),
            )
            .toViewState(isPremiumUser = true)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false),
                type = createIdentityContent(
                    isEmpty = false,
                    identityName = "Mx middleName",
                ),
            ),
            result,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform CipherView with odd address into ViewState Identity Content`() {
        val viewState = createCipherView(type = CipherType.IDENTITY, isEmpty = false)
        val result = viewState
            .copy(
                identity = viewState.identity?.copy(
                    address1 = null,
                    address2 = null,
                    address3 = "address3",
                    city = null,
                    state = "state",
                    postalCode = null,
                    country = null,
                ),
            )
            .toViewState(isPremiumUser = true)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false),
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

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform full CipherView into ViewState Secure Note Content with premium`() {
        val viewState = createCipherView(type = CipherType.SECURE_NOTE, isEmpty = false)
            .toViewState(isPremiumUser = true)

        assertEquals(
            VaultItemState.ViewState.Content(
                common = createCommonContent(isEmpty = false),
                type = VaultItemState.ViewState.Content.ItemType.SecureNote,
            ),
            viewState,
        )
    }

    @Suppress("MaxLineLength")
    @Test
    fun `toViewState should transform empty Secure Note CipherView into ViewState Secure Note Content`() {
        val viewState = createCipherView(type = CipherType.SECURE_NOTE, isEmpty = true)
            .toViewState(isPremiumUser = true)

        val expectedState = VaultItemState.ViewState.Content(
            common = createCommonContent(isEmpty = true).copy(isPremiumUser = true),
            type = VaultItemState.ViewState.Content.ItemType.SecureNote,
        )

        assertEquals(expectedState, viewState)
    }
}
