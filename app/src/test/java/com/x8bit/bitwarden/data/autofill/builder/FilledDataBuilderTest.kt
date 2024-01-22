package com.x8bit.bitwarden.data.autofill.builder

import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.inline.InlinePresentationSpec
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.model.AutofillPartition
import com.x8bit.bitwarden.data.autofill.model.AutofillRequest
import com.x8bit.bitwarden.data.autofill.model.AutofillView
import com.x8bit.bitwarden.data.autofill.model.FilledData
import com.x8bit.bitwarden.data.autofill.model.FilledItem
import com.x8bit.bitwarden.data.autofill.model.FilledPartition
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.util.buildFilledItemOrNull
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class FilledDataBuilderTest {
    private lateinit var filledDataBuilder: FilledDataBuilder

    private val autofillCipherProvider: AutofillCipherProvider = mockk()

    private val autofillId: AutofillId = mockk()
    private val autofillViewData = AutofillView.Data(
        autofillId = autofillId,
        idPackage = null,
        isFocused = false,
        webDomain = null,
        webScheme = null,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(AutofillValue::forText)
        mockkStatic(AutofillView::buildFilledItemOrNull)
        filledDataBuilder = FilledDataBuilderImpl(
            autofillCipherProvider = autofillCipherProvider,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(AutofillValue::forText)
        unmockkStatic(AutofillView::buildFilledItemOrNull)
    }

    @Test
    fun `build should return filled data and ignored AutofillIds when Login`() = runTest {
        // Setup
        val password = "Password"
        val username = "johnDoe"
        val autofillCipher = AutofillCipher.Login(
            name = "Cipher One",
            password = password,
            username = username,
            subtitle = "Subtitle",
        )
        val autofillViewEmail = AutofillView.Login.EmailAddress(
            data = autofillViewData,
        )
        val autofillViewPassword = AutofillView.Login.Password(
            data = autofillViewData,
        )
        val autofillViewUsername = AutofillView.Login.Username(
            data = autofillViewData,
        )
        val autofillPartition = AutofillPartition.Login(
            views = listOf(
                autofillViewEmail,
                autofillViewPassword,
                autofillViewUsername,
            ),
        )
        val ignoreAutofillIds: List<AutofillId> = mockk()
        val autofillRequest = AutofillRequest.Fillable(
            ignoreAutofillIds = ignoreAutofillIds,
            inlinePresentationSpecs = emptyList(),
            maxInlineSuggestionsCount = 0,
            partition = autofillPartition,
            uri = URI,
        )
        val filledItemEmail: FilledItem = mockk()
        val filledItemPassword: FilledItem = mockk()
        val filledItemUsername: FilledItem = mockk()
        val filledPartition = FilledPartition(
            autofillCipher = autofillCipher,
            filledItems = listOf(
                filledItemEmail,
                filledItemPassword,
                filledItemUsername,
            ),
            inlinePresentationSpec = null,
        )
        val expected = FilledData(
            filledPartitions = listOf(
                filledPartition,
            ),
            ignoreAutofillIds = ignoreAutofillIds,
            originalPartition = autofillPartition,
            uri = URI,
            vaultItemInlinePresentationSpec = null,
            isVaultLocked = false,
        )
        coEvery {
            autofillCipherProvider.getLoginAutofillCiphers(
                uri = URI,
            )
        } returns listOf(autofillCipher)
        every { autofillViewEmail.buildFilledItemOrNull(username) } returns filledItemEmail
        every { autofillViewPassword.buildFilledItemOrNull(password) } returns filledItemPassword
        every { autofillViewUsername.buildFilledItemOrNull(username) } returns filledItemUsername

        // Test
        val actual = filledDataBuilder.build(
            autofillRequest = autofillRequest,
        )

        // Verify
        assertEquals(expected, actual)
        coVerify(exactly = 1) {
            autofillCipherProvider.getLoginAutofillCiphers(
                uri = URI,
            )
        }
        verify(exactly = 1) {
            autofillViewEmail.buildFilledItemOrNull(username)
            autofillViewPassword.buildFilledItemOrNull(password)
            autofillViewUsername.buildFilledItemOrNull(username)
        }
    }

    @Test
    fun `build should return no partitions and ignored AutofillIds when Login and no URI`() =
        runTest {
            // Setup
            val autofillViewEmail = AutofillView.Login.EmailAddress(
                data = autofillViewData,
            )
            val autofillViewPassword = AutofillView.Login.Password(
                data = autofillViewData,
            )
            val autofillViewUsername = AutofillView.Login.Username(
                data = autofillViewData,
            )
            val autofillPartition = AutofillPartition.Login(
                views = listOf(
                    autofillViewEmail,
                    autofillViewPassword,
                    autofillViewUsername,
                ),
            )
            val ignoreAutofillIds: List<AutofillId> = mockk()
            val autofillRequest = AutofillRequest.Fillable(
                ignoreAutofillIds = ignoreAutofillIds,
                inlinePresentationSpecs = emptyList(),
                maxInlineSuggestionsCount = 0,
                partition = autofillPartition,
                uri = null,
            )
            val expected = FilledData(
                filledPartitions = emptyList(),
                ignoreAutofillIds = ignoreAutofillIds,
                originalPartition = autofillPartition,
                uri = null,
                vaultItemInlinePresentationSpec = null,
                isVaultLocked = false,
            )

            // Test
            val actual = filledDataBuilder.build(
                autofillRequest = autofillRequest,
            )

            // Verify
            assertEquals(expected, actual)
        }

    @Test
    fun `build should return filled data and ignored AutofillIds when Card`() = runTest {
        // Setup
        val code = "123"
        val expirationMonth = "January"
        val expirationYear = "1999"
        val number = "1234567890"
        val autofillCipher = AutofillCipher.Card(
            cardholderName = "John",
            code = code,
            expirationMonth = expirationMonth,
            expirationYear = expirationYear,
            name = "Cipher One",
            number = number,
            subtitle = "Subtitle",
        )
        val autofillViewCode = AutofillView.Card.SecurityCode(
            data = autofillViewData,
        )
        val autofillViewExpirationMonth = AutofillView.Card.ExpirationMonth(
            data = autofillViewData,
        )
        val autofillViewExpirationYear = AutofillView.Card.ExpirationYear(
            data = autofillViewData,
        )
        val autofillViewNumber = AutofillView.Card.Number(
            data = autofillViewData,
        )
        val autofillPartition = AutofillPartition.Card(
            views = listOf(
                autofillViewCode,
                autofillViewExpirationMonth,
                autofillViewExpirationYear,
                autofillViewNumber,
            ),
        )
        val ignoreAutofillIds: List<AutofillId> = mockk()
        val autofillRequest = AutofillRequest.Fillable(
            ignoreAutofillIds = ignoreAutofillIds,
            inlinePresentationSpecs = emptyList(),
            maxInlineSuggestionsCount = 0,
            partition = autofillPartition,
            uri = URI,
        )
        val filledItemCode: FilledItem = mockk()
        val filledItemExpirationMonth: FilledItem = mockk()
        val filledItemExpirationYear: FilledItem = mockk()
        val filledItemNumber: FilledItem = mockk()
        val filledPartition = FilledPartition(
            autofillCipher = autofillCipher,
            filledItems = listOf(
                filledItemCode,
                filledItemExpirationMonth,
                filledItemExpirationYear,
                filledItemNumber,
            ),
            inlinePresentationSpec = null,
        )
        val expected = FilledData(
            filledPartitions = listOf(
                filledPartition,
            ),
            ignoreAutofillIds = ignoreAutofillIds,
            originalPartition = autofillPartition,
            uri = URI,
            vaultItemInlinePresentationSpec = null,
            isVaultLocked = false,
        )
        coEvery { autofillCipherProvider.getCardAutofillCiphers() } returns listOf(autofillCipher)
        every { autofillViewCode.buildFilledItemOrNull(code) } returns filledItemCode
        every {
            autofillViewExpirationMonth.buildFilledItemOrNull(expirationMonth)
        } returns filledItemExpirationMonth
        every {
            autofillViewExpirationYear.buildFilledItemOrNull(expirationYear)
        } returns filledItemExpirationYear
        every { autofillViewNumber.buildFilledItemOrNull(number) } returns filledItemNumber

        // Test
        val actual = filledDataBuilder.build(
            autofillRequest = autofillRequest,
        )

        // Verify
        assertEquals(expected, actual)
        coVerify(exactly = 1) {
            autofillCipherProvider.getCardAutofillCiphers()
            autofillViewCode.buildFilledItemOrNull(code)
            autofillViewExpirationMonth.buildFilledItemOrNull(expirationMonth)
            autofillViewExpirationYear.buildFilledItemOrNull(expirationYear)
            autofillViewNumber.buildFilledItemOrNull(number)
        }
    }

    @Test
    fun `build should return filled data with max count of inline specs with one spec repeated`() =
        runTest {
            // Setup
            val password = "Password"
            val username = "johnDoe"
            val autofillCipher = AutofillCipher.Login(
                name = "Cipher One",
                password = password,
                username = username,
                subtitle = "Subtitle",
            )
            val autofillViewEmail = AutofillView.Login.EmailAddress(
                data = autofillViewData,
            )
            val autofillViewPassword = AutofillView.Login.Password(
                data = autofillViewData,
            )
            val autofillViewUsername = AutofillView.Login.Username(
                data = autofillViewData,
            )
            val autofillPartition = AutofillPartition.Login(
                views = listOf(
                    autofillViewEmail,
                    autofillViewPassword,
                    autofillViewUsername,
                ),
            )
            val inlinePresentationSpec: InlinePresentationSpec = mockk()
            val autofillRequest = AutofillRequest.Fillable(
                ignoreAutofillIds = emptyList(),
                inlinePresentationSpecs = listOf(
                    inlinePresentationSpec,
                ),
                maxInlineSuggestionsCount = 3,
                partition = autofillPartition,
                uri = URI,
            )
            val filledItemEmail: FilledItem = mockk()
            val filledItemPassword: FilledItem = mockk()
            val filledItemUsername: FilledItem = mockk()
            val filledPartitionOne = FilledPartition(
                autofillCipher = autofillCipher,
                filledItems = listOf(
                    filledItemEmail,
                    filledItemPassword,
                    filledItemUsername,
                ),
                inlinePresentationSpec = inlinePresentationSpec,
            )
            val filledPartitionTwo = filledPartitionOne.copy()
            val filledPartitionThree = FilledPartition(
                autofillCipher = autofillCipher,
                filledItems = listOf(
                    filledItemEmail,
                    filledItemPassword,
                    filledItemUsername,
                ),
                inlinePresentationSpec = null,
            )
            val expected = FilledData(
                filledPartitions = listOf(
                    filledPartitionOne,
                    filledPartitionTwo,
                    filledPartitionThree,
                ),
                ignoreAutofillIds = emptyList(),
                originalPartition = autofillPartition,
                uri = URI,
                vaultItemInlinePresentationSpec = inlinePresentationSpec,
                isVaultLocked = false,
            )
            coEvery {
                autofillCipherProvider.getLoginAutofillCiphers(
                    uri = URI,
                )
            } returns listOf(autofillCipher, autofillCipher, autofillCipher)
            every { autofillViewEmail.buildFilledItemOrNull(username) } returns filledItemEmail
            every {
                autofillViewPassword.buildFilledItemOrNull(password)
            } returns filledItemPassword
            every {
                autofillViewUsername.buildFilledItemOrNull(username)
            } returns filledItemUsername

            // Test
            val actual = filledDataBuilder.build(
                autofillRequest = autofillRequest,
            )

            // Verify
            assertEquals(expected, actual)
            coVerify(exactly = 1) {
                autofillCipherProvider.getLoginAutofillCiphers(
                    uri = URI,
                )
            }
            verify(exactly = 3) {
                autofillViewEmail.buildFilledItemOrNull(username)
                autofillViewPassword.buildFilledItemOrNull(password)
                autofillViewUsername.buildFilledItemOrNull(username)
            }
        }

    companion object {
        private const val URI: String = "androidapp://com.x8bit.bitwarden"
    }
}
