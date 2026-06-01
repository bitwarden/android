package com.x8bit.bitwarden.ui.autofill.util

import android.content.Context
import androidx.core.content.ContextCompat
import com.bitwarden.ui.platform.resource.BitwardenString
import com.x8bit.bitwarden.data.autofill.model.AutofillAppInfo
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillUtilsTest {
    private val context: Context = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(ContextCompat::getString)
        every {
            ContextCompat.getString(context, BitwardenString.autofill_suggestion)
        } returns "Autofill suggestion"
        every {
            ContextCompat.getString(context, BitwardenString.type_card)
        } returns "Card"
        every {
            ContextCompat.getString(context, BitwardenString.type_login)
        } returns "Login"
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(
            ContextCompat::getString,
        )
    }

    @Test
    fun `getAutofillSuggestionContentDescription should return correct content description`() {
        listOf(
            Triple(
                first = AutofillCipher.Card(
                    cardholderName = "John",
                    cipherId = null,
                    code = "code",
                    expirationMonth = "expirationMonth",
                    expirationYear = "expirationYear",
                    name = "Cipher One",
                    number = "number",
                    subtitle = "Subtitle",
                    brand = "Visa",
                ),
                second = AutofillAppInfo(
                    context = context,
                    packageName = "com.x8bit.bitwarden",
                    sdkInt = 34,
                ),
                third = "Autofill suggestion, Card, Cipher One, Subtitle",
            ),
            Triple(
                first = AutofillCipher.Card(
                    cardholderName = "John",
                    cipherId = null,
                    code = "code",
                    expirationMonth = "expirationMonth",
                    expirationYear = "expirationYear",
                    name = "Capital One",
                    number = "number",
                    subtitle = "JohnCardName",
                    brand = "Visa",
                ),
                second = AutofillAppInfo(
                    context = context,
                    packageName = "com.x8bit.bitwarden",
                    sdkInt = 34,
                ),
                third = "Autofill suggestion, Card, Capital One, JohnCardName",
            ),
            Triple(
                first = AutofillCipher.Login(
                    cipherId = null,
                    isTotpEnabled = false,
                    name = "Cipher One",
                    password = "password",
                    username = "username",
                    subtitle = "Subtitle",
                    website = "website",
                ),
                second = AutofillAppInfo(
                    context = context,
                    packageName = "com.x8bit.bitwarden",
                    sdkInt = 34,
                ),
                third = "Autofill suggestion, Login, Cipher One, Subtitle",
            ),
            Triple(
                first = AutofillCipher.Login(
                    cipherId = null,
                    isTotpEnabled = false,
                    name = "Amazon",
                    password = "password",
                    username = "username",
                    subtitle = "AmazonSubtitle",
                    website = "website",
                ),
                second = AutofillAppInfo(
                    context = context,
                    packageName = "com.x8bit.bitwarden",
                    sdkInt = 34,
                ),
                third = "Autofill suggestion, Login, Amazon, AmazonSubtitle",
            ),
        )
            .forEach {
                val result = getAutofillSuggestionContentDescription(
                    autofillCipher = it.first,
                    autofillAppInfo = it.second,
                )
                assertEquals(it.third, result)
            }
    }
}
