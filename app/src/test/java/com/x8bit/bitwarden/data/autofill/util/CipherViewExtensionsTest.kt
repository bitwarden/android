package com.x8bit.bitwarden.data.autofill.util

import com.bitwarden.vault.CipherType
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockSdkFido2CredentialList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CipherViewExtensionsTest {

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillCipherProvider should return a provider with the correct data for a Login type without TOTP`() =
        runTest {
            val cipherView = createMockCipherView(
                number = 1,
                cipherType = CipherType.LOGIN,
                totp = null,
            )

            val autofillCipherProvider = cipherView.toAutofillCipherProvider()

            assertFalse(autofillCipherProvider.isVaultLocked())
            assertEquals(
                emptyList<AutofillCipher.Card>(),
                autofillCipherProvider.getCardAutofillCiphers(),
            )
            assertEquals(
                listOf(
                    AutofillCipher.Login(
                        cipherId = "mockId-1",
                        isTotpEnabled = false,
                        name = "mockName-1",
                        subtitle = "mockUsername-1",
                        password = "mockPassword-1",
                        username = "mockUsername-1",
                    ),
                ),
                autofillCipherProvider.getLoginAutofillCiphers(uri = "uri"),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillCipherProvider should return a provider with the correct data for a Login type with TOTP`() =
        runTest {
            val cipherView = createMockCipherView(
                number = 1,
                cipherType = CipherType.LOGIN,
                totp = "mockkTotp-1",
            )

            val autofillCipherProvider = cipherView.toAutofillCipherProvider()

            assertFalse(autofillCipherProvider.isVaultLocked())
            assertEquals(
                emptyList<AutofillCipher.Card>(),
                autofillCipherProvider.getCardAutofillCiphers(),
            )
            assertEquals(
                listOf(
                    AutofillCipher.Login(
                        cipherId = "mockId-1",
                        isTotpEnabled = true,
                        name = "mockName-1",
                        subtitle = "mockUsername-1",
                        password = "mockPassword-1",
                        username = "mockUsername-1",
                    ),
                ),
                autofillCipherProvider.getLoginAutofillCiphers(uri = "uri"),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillCipherProvider should return a provider with the correct data for a Card type`() =
        runTest {
            val cipherView = createMockCipherView(
                number = 1,
                cipherType = CipherType.CARD,
            )

            val autofillCipherProvider = cipherView.toAutofillCipherProvider()

            assertFalse(autofillCipherProvider.isVaultLocked())
            assertEquals(
                emptyList<AutofillCipher.Login>(),
                autofillCipherProvider.getLoginAutofillCiphers(uri = "uri"),
            )
            assertEquals(
                listOf(
                    AutofillCipher.Card(
                        cipherId = "mockId-1",
                        name = "mockName-1",
                        subtitle = "mockBrand-1, *er-1",
                        cardholderName = "mockCardholderName-1",
                        code = "mockCode-1",
                        expirationMonth = "mockExpMonth-1",
                        expirationYear = "mockExpirationYear-1",
                        number = "mockNumber-1",
                    ),
                ),
                autofillCipherProvider.getCardAutofillCiphers(),
            )
        }

    @Suppress("MaxLineLength")
    @Test
    fun `toAutofillCipherProvider should return a provider with the correct data for any other type`() =
        runTest {
            CipherType
                .entries
                .filterNot { it == CipherType.CARD }
                .filterNot { it == CipherType.LOGIN }
                .forEach { cipherType ->
                    val autofillCipherProvider = createMockCipherView(
                        number = 1,
                        cipherType = cipherType,
                    )
                        .toAutofillCipherProvider()

                    assertFalse(autofillCipherProvider.isVaultLocked())
                    assertEquals(
                        emptyList<AutofillCipher.Card>(),
                        autofillCipherProvider.getCardAutofillCiphers(),
                    )
                    assertEquals(
                        emptyList<AutofillCipher.Login>(),
                        autofillCipherProvider.getLoginAutofillCiphers(uri = "uri"),
                    )
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `isActiveWithFido2Credentials should return true when type is login, deleted date is null, and fido2 credentials are not null`() {
        assertTrue(
            createMockCipherView(
                number = 1,
                fido2Credentials = createMockSdkFido2CredentialList(number = 1),
            )
                .isActiveWithFido2Credentials,
        )
    }

    @Test
    fun `isActiveWithFido2Credentials should return false when deleted date is not null`() {
        assertFalse(
            createMockCipherView(number = 1, isDeleted = true)
                .isActiveWithFido2Credentials,
        )
    }

    @Test
    fun `isActiveWithFido2Credentials should return false when type is not login`() {
        assertFalse(
            createMockCipherView(number = 1, cipherType = CipherType.CARD)
                .isActiveWithFido2Credentials,
        )
    }

    @Test
    fun `isActiveWithFido2Credentials should return false when login is null`() {
        assertFalse(
            createMockCipherView(number = 1)
                .copy(login = null)
                .isActiveWithFido2Credentials,
        )
    }

    @Test
    fun `isActiveWithFido2Credentials should return false when fido2Credentials is null`() {
        assertFalse(
            createMockCipherView(number = 1, fido2Credentials = null)
                .isActiveWithFido2Credentials,
        )
    }
}
