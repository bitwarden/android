package com.x8bit.bitwarden.data.autofill.processor

import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.LoginView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProviderImpl
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillCipherProviderTest {
    private val cardView: CardView = mockk {
        every { cardholderName } returns CARD_CARDHOLDER_NAME
        every { code } returns CARD_CODE
        every { expMonth } returns CARD_EXP_MONTH
        every { expYear } returns CARD_EXP_YEAR
        every { number } returns CARD_NUMBER
    }
    private val cardCipherView: CipherView = mockk {
        every { card } returns cardView
        every { deletedDate } returns null
        every { id } returns CIPHER_ID
        every { name } returns CARD_NAME
        every { reprompt } returns CipherRepromptType.NONE
        every { type } returns CipherType.CARD
    }
    private val loginViewWithoutTotp: LoginView = mockk {
        every { password } returns LOGIN_PASSWORD
        every { username } returns LOGIN_USERNAME
        every { totp } returns null
    }
    private val loginCipherViewWithoutTotp: CipherView = mockk {
        every { deletedDate } returns null
        every { id } returns CIPHER_ID
        every { login } returns loginViewWithoutTotp
        every { name } returns LOGIN_NAME
        every { reprompt } returns CipherRepromptType.NONE
        every { type } returns CipherType.LOGIN
    }
    private val loginViewWithTotp: LoginView = mockk {
        every { password } returns LOGIN_PASSWORD
        every { username } returns LOGIN_USERNAME
        every { totp } returns "TOTP-CODE"
    }
    private val loginCipherViewWithTotp: CipherView = mockk {
        every { deletedDate } returns null
        every { id } returns CIPHER_ID
        every { login } returns loginViewWithTotp
        every { name } returns LOGIN_NAME
        every { reprompt } returns CipherRepromptType.NONE
        every { type } returns CipherType.LOGIN
    }
    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns ACTIVE_USER_ID
    }
    private val cipherMatchingManager: CipherMatchingManager = mockk()
    private val mutableVaultStateFlow = MutableStateFlow<List<VaultUnlockData>>(
        emptyList(),
    )
    private val mutableCiphersStateFlow = MutableStateFlow<DataState<List<CipherView>>>(
        DataState.Loading,
    )
    private val vaultRepository: VaultRepository = mockk {
        every { ciphersStateFlow } returns mutableCiphersStateFlow
        every { vaultUnlockDataStateFlow } returns mutableVaultStateFlow
        every { isVaultUnlocked(ACTIVE_USER_ID) } answers {
            mutableVaultStateFlow.value.statusFor(ACTIVE_USER_ID) == VaultUnlockData.Status.UNLOCKED
        }
    }

    private lateinit var autofillCipherProvider: AutofillCipherProvider

    @BeforeEach
    fun setup() {
        mockkStatic(CipherView::subtitle)
        autofillCipherProvider = AutofillCipherProviderImpl(
            authRepository = authRepository,
            cipherMatchingManager = cipherMatchingManager,
            vaultRepository = vaultRepository,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(CipherView::subtitle)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `isVaultLocked when there is no active user should return true`() =
        runTest {
            every { authRepository.activeUserId } returns null

            val result = async {
                autofillCipherProvider.isVaultLocked()
            }

            testScheduler.advanceUntilIdle()
            assertTrue(result.isCompleted)
            assertTrue(result.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `isVaultLocked when there is an active user should wait for pending unlocking to finish and return the locked state for that user`() =
        runTest {
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(
                    userId = ACTIVE_USER_ID,
                    status = VaultUnlockData.Status.UNLOCKING,
                ),
            )

            val result = async {
                autofillCipherProvider.isVaultLocked()
            }

            testScheduler.runCurrent()
            assertFalse(result.isCompleted)

            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(
                    userId = ACTIVE_USER_ID,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )

            testScheduler.advanceUntilIdle()
            assertTrue(result.isCompleted)

            assertFalse(result.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `isVaultLocked when there is an active user should wait for pending unlocking to finish and return the locked state for that user when it times out`() =
        runTest {
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(
                    userId = ACTIVE_USER_ID,
                    status = VaultUnlockData.Status.UNLOCKING,
                ),
            )

            val result = async {
                autofillCipherProvider.isVaultLocked()
            }

            testScheduler.runCurrent()
            assertFalse(result.isCompleted)

            testScheduler.advanceTimeBy(delayTimeMillis = 1_000L)
            testScheduler.runCurrent()
            assertTrue(result.isCompleted)
            assertTrue(result.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getCardAutofillCiphers when unlocked should return empty list when retrieving ciphers times out`() =
        runTest {
            coEvery { vaultRepository.isVaultUnlocked(ACTIVE_USER_ID) } returns true
            mutableCiphersStateFlow.value = DataState.Loading

            // Test
            val actual = async {
                autofillCipherProvider.getCardAutofillCiphers()
            }

            testScheduler.runCurrent()
            assertFalse(actual.isCompleted)
            testScheduler.advanceTimeBy(delayTimeMillis = 2_000L)
            testScheduler.runCurrent()

            // Verify
            assertTrue(actual.isCompleted)
            assertEquals(emptyList<AutofillCipher.Card>(), actual.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getCardAutofillCiphers when unlocked should return non-null, non-deleted, and non-reprompt card ciphers`() =
        runTest {
            val deletedCardCipherView: CipherView = mockk {
                every { deletedDate } returns mockk()
                every { type } returns CipherType.CARD
            }
            val repromptCardCipherView: CipherView = mockk {
                every { deletedDate } returns null
                every { reprompt } returns CipherRepromptType.PASSWORD
                every { type } returns CipherType.CARD
            }
            val cipherViews = listOf(
                cardCipherView,
                deletedCardCipherView,
                repromptCardCipherView,
                loginCipherViewWithTotp,
                loginCipherViewWithoutTotp,
            )
            mutableCiphersStateFlow.value = DataState.Loaded(
                data = cipherViews,
            )
            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(
                    userId = ACTIVE_USER_ID,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )
            val expected = listOf(
                CARD_AUTOFILL_CIPHER,
            )
            every { cardCipherView.subtitle } returns CARD_SUBTITLE

            // Test & Verify
            val actual = autofillCipherProvider.getCardAutofillCiphers()

            assertEquals(expected, actual)
        }

    @Test
    fun `getCardAutofillCiphers when locked should return an empty list`() = runTest {
        mutableVaultStateFlow.value = emptyList()

        // Test & Verify
        val actual = autofillCipherProvider.getCardAutofillCiphers()

        assertEquals(emptyList<AutofillCipher.Card>(), actual)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getLoginAutofillCiphers when unlocked should return empty list when retrieving ciphers times out`() =
        runTest {
            coEvery { vaultRepository.isVaultUnlocked(ACTIVE_USER_ID) } returns true
            mutableCiphersStateFlow.value = DataState.Loading

            // Test
            val actual = async {
                autofillCipherProvider.getLoginAutofillCiphers(uri = URI)
            }

            testScheduler.runCurrent()
            assertFalse(actual.isCompleted)
            testScheduler.advanceTimeBy(delayTimeMillis = 2_000L)
            testScheduler.runCurrent()

            // Verify
            assertTrue(actual.isCompleted)
            assertEquals(emptyList<AutofillCipher.Login>(), actual.await())
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getLoginAutofillCiphers when unlocked should return matched, non-deleted, non-reprompt, login ciphers`() =
        runTest {
            val deletedLoginCipherView: CipherView = mockk {
                every { deletedDate } returns mockk()
                every { type } returns CipherType.LOGIN
            }
            val repromptLoginCipherView: CipherView = mockk {
                every { deletedDate } returns null
                every { reprompt } returns CipherRepromptType.PASSWORD
                every { type } returns CipherType.LOGIN
            }
            val cipherViews = listOf(
                cardCipherView,
                loginCipherViewWithTotp,
                loginCipherViewWithoutTotp,
                deletedLoginCipherView,
                repromptLoginCipherView,
            )
            val filteredCipherViews = listOf(
                loginCipherViewWithTotp,
                loginCipherViewWithoutTotp,
            )
            coEvery {
                cipherMatchingManager.filterCiphersForMatches(
                    ciphers = filteredCipherViews,
                    matchUri = URI,
                )
            } returns filteredCipherViews
            mutableCiphersStateFlow.value = DataState.Loaded(
                data = cipherViews,
            )
            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(
                    userId = ACTIVE_USER_ID,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )
            val expected = listOf(
                LOGIN_AUTOFILL_CIPHER_WITH_TOTP,
                LOGIN_AUTOFILL_CIPHER_WITHOUT_TOTP,
            )
            every { loginCipherViewWithTotp.subtitle } returns LOGIN_SUBTITLE
            every { loginCipherViewWithoutTotp.subtitle } returns LOGIN_SUBTITLE

            // Test
            val actual = autofillCipherProvider.getLoginAutofillCiphers(
                uri = URI,
            )

            // Verify
            assertEquals(expected, actual)
            coVerify {
                cipherMatchingManager.filterCiphersForMatches(
                    ciphers = filteredCipherViews,
                    matchUri = URI,
                )
            }
        }

    @Test
    fun `getLoginAutofillCiphers when locked should return an empty list`() = runTest {
        mutableVaultStateFlow.value = emptyList()

        // Test & Verify
        val actual = autofillCipherProvider.getLoginAutofillCiphers(
            uri = URI,
        )

        assertEquals(emptyList<AutofillCipher.Login>(), actual)
    }
}

private const val ACTIVE_USER_ID = "activeUserId"
private const val CARD_CARDHOLDER_NAME = "John Doe"
private const val CARD_CODE = "123"
private const val CARD_EXP_MONTH = "January"
private const val CARD_EXP_YEAR = "2029"
private const val CARD_NAME = "John's Card"
private const val CARD_NUMBER = "1234567890"
private const val CARD_SUBTITLE = "7890"
private const val CIPHER_ID = "1234567890"
private val CARD_AUTOFILL_CIPHER = AutofillCipher.Card(
    cardholderName = CARD_CARDHOLDER_NAME,
    cipherId = CIPHER_ID,
    code = CARD_CODE,
    expirationMonth = CARD_EXP_MONTH,
    expirationYear = CARD_EXP_YEAR,
    name = CARD_NAME,
    number = CARD_NUMBER,
    subtitle = CARD_SUBTITLE,
)
private const val LOGIN_NAME = "John's Login"
private const val LOGIN_PASSWORD = "Password123"
private const val LOGIN_SUBTITLE = "John Doe"
private const val LOGIN_USERNAME = "John-Bitwarden"
private val LOGIN_AUTOFILL_CIPHER_WITH_TOTP = AutofillCipher.Login(
    cipherId = CIPHER_ID,
    isTotpEnabled = true,
    name = LOGIN_NAME,
    password = LOGIN_PASSWORD,
    subtitle = LOGIN_SUBTITLE,
    username = LOGIN_USERNAME,
)
private val LOGIN_AUTOFILL_CIPHER_WITHOUT_TOTP = AutofillCipher.Login(
    cipherId = CIPHER_ID,
    isTotpEnabled = false,
    name = LOGIN_NAME,
    password = LOGIN_PASSWORD,
    subtitle = LOGIN_SUBTITLE,
    username = LOGIN_USERNAME,
)
private const val URI: String = "androidapp://com.x8bit.bitwarden"
