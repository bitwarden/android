package com.x8bit.bitwarden.data.autofill.util

import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillSaveItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillRequestExtensionsTest {
    @BeforeEach
    fun setup() {
        mockkStatic(AUTOFILL_REQUEST_EXTENSIONS_PATH)
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AUTOFILL_REQUEST_EXTENSIONS_PATH)
    }

    @Test
    fun `toAutofillSaveItem should return AutofillSaveItem Card when card partition`() {
        // Setup
        val autofillPartition: AutofillPartition.Card = mockk {
            every { expirationMonthSaveValue } returns SAVE_VALUE_MONTH
            every { expirationYearSaveValue } returns SAVE_VALUE_YEAR
            every { numberSaveValue } returns SAVE_VALUE_NUMBER
            every { securityCodeSaveValue } returns SAVE_VALUE_CODE
        }
        val autofillRequest: AutofillRequest.Fillable = mockk {
            every { partition } returns autofillPartition
        }
        val expected = AutofillSaveItem.Card(
            number = SAVE_VALUE_NUMBER,
            expirationMonth = SAVE_VALUE_MONTH,
            expirationYear = SAVE_VALUE_YEAR,
            securityCode = SAVE_VALUE_CODE,
        )

        // Test
        val actual = autofillRequest.toAutofillSaveItem()

        // Verify
        assertEquals(expected, actual)
    }

    @Test
    fun `toAutofillSaveItem should return AutofillSaveItem Login when card partition`() {
        RAW_URI_LIST
            .forEach { rawUri ->
                // Setup
                val autofillPartition: AutofillPartition.Login = mockk {
                    every { usernameSaveValue } returns SAVE_VALUE_USERNAME
                    every { passwordSaveValue } returns SAVE_VALUE_PASSWORD
                }
                val autofillRequest: AutofillRequest.Fillable = mockk {
                    every { partition } returns autofillPartition
                    every { uri } returns rawUri
                }
                val expected = AutofillSaveItem.Login(
                    username = SAVE_VALUE_USERNAME,
                    password = SAVE_VALUE_PASSWORD,
                    uri = FINAL_URI,
                )

                // Test
                val actual = autofillRequest.toAutofillSaveItem()

                // Verify
                assertEquals(expected, actual)
            }
    }

    @Test
    fun `toAutofillSaveItem should return AutofillSaveItem Login with androidapp on URI`() {

        val autofillPartition: AutofillPartition.Login = mockk {
            every { usernameSaveValue } returns SAVE_VALUE_USERNAME
            every { passwordSaveValue } returns SAVE_VALUE_PASSWORD
        }
        val autofillRequest: AutofillRequest.Fillable = mockk {
            every { partition } returns autofillPartition
            every { uri } returns RAW_ANDROIDAPP_URI
        }
        val expected = AutofillSaveItem.Login(
            username = SAVE_VALUE_USERNAME,
            password = SAVE_VALUE_PASSWORD,
            uri = RAW_ANDROIDAPP_URI,
        )

        // Test
        val actual = autofillRequest.toAutofillSaveItem()

        // Verify
        assertEquals(expected, actual)
    }
}

private const val AUTOFILL_REQUEST_EXTENSIONS_PATH =
    "com.x8bit.bitwarden.data.autofill.util.AutofillPartitionExtensionsKt"

// CARD DATA
private const val SAVE_VALUE_CODE: String = "SAVE_VALUE_CODE"
private const val SAVE_VALUE_MONTH: String = "SAVE_VALUE_MONTH"
private const val SAVE_VALUE_NUMBER: String = "SAVE_VALUE_NUMBER"
private const val SAVE_VALUE_YEAR: String = "SAVE_VALUE_YEAR"

// LOGIN DATA
private const val SAVE_VALUE_PASSWORD: String = "SAVE_VALUE_PASSWORD"
private const val SAVE_VALUE_USERNAME: String = "SAVE_VALUE_USERNAME"
private const val FINAL_URI: String = "URI"
private val RAW_URI_LIST: List<String> = listOf(
    "https://URI",
    "http://URI",
)
private const val RAW_ANDROIDAPP_URI: String = "androidapp://URI"
