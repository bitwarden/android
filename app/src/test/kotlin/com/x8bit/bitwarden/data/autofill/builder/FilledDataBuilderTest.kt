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

    private val autofillCipherProvider: AutofillCipherProvider = mockk {
        coEvery { isVaultLocked() } returns false
    }

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

    @Suppress("MaxLineLength")
    @Test
    fun `build should skip null AutofillValues and return filled data and ignored AutofillIds when Login`() =
        runTest {
            // Setup
            val password = "Password"
            val username = "johnDoe"
            val autofillCipher = AutofillCipher.Login(
                cipherId = null,
                name = "Cipher One",
                isTotpEnabled = false,
                password = password,
                username = username,
                subtitle = "Subtitle",
            )
            val filledItemPassword: FilledItem = mockk()
            val filledItemUsername: FilledItem = mockk()
            val autofillViewPassword: AutofillView.Login.Password = mockk {
                every { buildFilledItemOrNull(password) } returns filledItemPassword
            }
            val autofillViewUsernameOne: AutofillView.Login.Username = mockk {
                every { buildFilledItemOrNull(username) } returns filledItemUsername
            }
            val autofillViewUsernameTwo: AutofillView.Login.Username = mockk {
                every { buildFilledItemOrNull(username) } returns null
            }
            val autofillPartition = AutofillPartition.Login(
                views = listOf(
                    autofillViewPassword,
                    autofillViewUsernameOne,
                    autofillViewUsernameTwo,
                ),
            )
            val ignoreAutofillIds: List<AutofillId> = mockk()
            val autofillRequest = AutofillRequest.Fillable(
                ignoreAutofillIds = ignoreAutofillIds,
                inlinePresentationSpecs = emptyList(),
                maxInlineSuggestionsCount = 0,
                packageName = null,
                partition = autofillPartition,
                uri = URI,
            )
            val filledPartition = FilledPartition(
                autofillCipher = autofillCipher,
                filledItems = listOf(
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
                autofillViewPassword.buildFilledItemOrNull(password)
                autofillViewUsernameOne.buildFilledItemOrNull(username)
                autofillViewUsernameTwo.buildFilledItemOrNull(username)
            }
        }

    @Test
    fun `build should return no partitions and ignored AutofillIds when Login and no URI`() =
        runTest {
            // Setup
            val autofillViewPassword: AutofillView.Login.Password = mockk()
            val autofillViewUsername: AutofillView.Login.Username = mockk()
            val autofillPartition = AutofillPartition.Login(
                views = listOf(
                    autofillViewPassword,
                    autofillViewUsername,
                ),
            )
            val ignoreAutofillIds: List<AutofillId> = mockk()
            val autofillRequest = AutofillRequest.Fillable(
                ignoreAutofillIds = ignoreAutofillIds,
                inlinePresentationSpecs = emptyList(),
                maxInlineSuggestionsCount = 0,
                packageName = null,
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

    @Suppress("MaxLineLength")
    @Test
    fun `build should skip null AutofillValues and return filled data and ignored AutofillIds when Card`() =
        runTest {
            // Setup
            val code = "123"
            val expirationMonth = "January"
            val expirationYear = "1999"
            val number = "1234567890"
            val autofillCipher = AutofillCipher.Card(
                cardholderName = "John",
                cipherId = null,
                code = code,
                expirationMonth = expirationMonth,
                expirationYear = expirationYear,
                name = "Cipher One",
                number = number,
                subtitle = "Subtitle",
            )
            val filledItemCode: FilledItem = mockk()
            val filledItemExpirationMonth: FilledItem = mockk()
            val filledItemExpirationYear: FilledItem = mockk()
            val filledItemNumber: FilledItem = mockk()
            val autofillViewCode: AutofillView.Card.SecurityCode = mockk {
                every { buildFilledItemOrNull(code) } returns filledItemCode
            }
            val autofillViewExpirationMonth: AutofillView.Card.ExpirationMonth = mockk {
                every { buildFilledItemOrNull(expirationMonth) } returns filledItemExpirationMonth
            }
            val autofillViewExpirationYear: AutofillView.Card.ExpirationYear = mockk {
                every { buildFilledItemOrNull(expirationYear) } returns filledItemExpirationYear
            }
            val autofillViewNumberOne: AutofillView.Card.Number = mockk {
                every { buildFilledItemOrNull(number) } returns filledItemNumber
            }
            val autofillViewNumberTwo: AutofillView.Card.Number = mockk {
                every { buildFilledItemOrNull(number) } returns null
            }
            val autofillPartition = AutofillPartition.Card(
                views = listOf(
                    autofillViewCode,
                    autofillViewExpirationMonth,
                    autofillViewExpirationYear,
                    autofillViewNumberOne,
                    autofillViewNumberTwo,
                ),
            )
            val ignoreAutofillIds: List<AutofillId> = mockk()
            val autofillRequest = AutofillRequest.Fillable(
                ignoreAutofillIds = ignoreAutofillIds,
                inlinePresentationSpecs = emptyList(),
                maxInlineSuggestionsCount = 0,
                packageName = null,
                partition = autofillPartition,
                uri = URI,
            )
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
            coEvery {
                autofillCipherProvider.getCardAutofillCiphers()
            } returns listOf(autofillCipher)

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
                autofillViewNumberOne.buildFilledItemOrNull(number)
                autofillViewNumberTwo.buildFilledItemOrNull(number)
            }
        }

    @Test
    fun `build should return filled data with max count of inline specs with one spec repeated`() =
        runTest {
            // Setup
            val password = "Password"
            val username = "johnDoe"
            val autofillCipher = AutofillCipher.Login(
                cipherId = null,
                isTotpEnabled = false,
                name = "Cipher One",
                password = password,
                username = username,
                subtitle = "Subtitle",
            )

            val filledItemPassword: FilledItem = mockk()
            val filledItemUsername: FilledItem = mockk()
            val autofillViewPassword: AutofillView.Login.Password = mockk {
                every { buildFilledItemOrNull(password) } returns filledItemPassword
            }
            val autofillViewUsername: AutofillView.Login.Username = mockk {
                every { buildFilledItemOrNull(username) } returns filledItemUsername
            }
            val autofillPartition = AutofillPartition.Login(
                views = listOf(
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
                packageName = null,
                partition = autofillPartition,
                uri = URI,
            )
            val filledPartitionOne = FilledPartition(
                autofillCipher = autofillCipher,
                filledItems = listOf(
                    filledItemPassword,
                    filledItemUsername,
                ),
                inlinePresentationSpec = inlinePresentationSpec,
            )
            val filledPartitionTwo = filledPartitionOne.copy()
            val filledPartitionThree = FilledPartition(
                autofillCipher = autofillCipher,
                filledItems = listOf(
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
                autofillViewPassword.buildFilledItemOrNull(password)
                autofillViewUsername.buildFilledItemOrNull(username)
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `build should return filled data with hard-coded max count of inline specs and partitions`() =
        runTest {
            // Setup
            val password = "Password"
            val username = "johnDoe"
            val autofillCipher = AutofillCipher.Login(
                cipherId = null,
                isTotpEnabled = false,
                name = "Cipher One",
                password = password,
                username = username,
                subtitle = "Subtitle",
            )
            val filledItemPassword: FilledItem = mockk()
            val filledItemUsername: FilledItem = mockk()
            val autofillViewPassword: AutofillView.Login.Password = mockk {
                every { buildFilledItemOrNull(password) } returns filledItemPassword
            }
            val autofillViewUsername: AutofillView.Login.Username = mockk {
                every { buildFilledItemOrNull(username) } returns filledItemUsername
            }
            val autofillPartition = AutofillPartition.Login(
                views = listOf(autofillViewPassword, autofillViewUsername),
            )
            val inlinePresentationSpec: InlinePresentationSpec = mockk()
            val autofillRequest = AutofillRequest.Fillable(
                ignoreAutofillIds = emptyList(),
                inlinePresentationSpecs = listOf(inlinePresentationSpec),
                maxInlineSuggestionsCount = 10,
                packageName = null,
                partition = autofillPartition,
                uri = URI,
            )
            val filledPartition = FilledPartition(
                autofillCipher = autofillCipher,
                filledItems = listOf(filledItemPassword, filledItemUsername),
                inlinePresentationSpec = inlinePresentationSpec,
            )
            val expected = FilledData(
                // 5 with inline specs, 20 total
                filledPartitions = listOf(
                    filledPartition,
                    filledPartition.copy(),
                    filledPartition.copy(),
                    filledPartition.copy(),
                    filledPartition.copy(),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                    filledPartition.copy(inlinePresentationSpec = null),
                ),
                ignoreAutofillIds = emptyList(),
                originalPartition = autofillPartition,
                uri = URI,
                vaultItemInlinePresentationSpec = inlinePresentationSpec,
                isVaultLocked = false,
            )
            coEvery {
                autofillCipherProvider.getLoginAutofillCiphers(uri = URI)
            } returns List(size = 22) { autofillCipher }

            // Test
            val actual = filledDataBuilder.build(
                autofillRequest = autofillRequest,
            )

            // Verify
            assertEquals(expected, actual)
            coVerify(exactly = 1) {
                autofillCipherProvider.getLoginAutofillCiphers(uri = URI)
            }
            verify(exactly = 22) {
                autofillViewPassword.buildFilledItemOrNull(password)
                autofillViewUsername.buildFilledItemOrNull(username)
            }
        }

    companion object {
        private const val URI: String = "androidapp://com.x8bit.bitwarden"
    }
}
