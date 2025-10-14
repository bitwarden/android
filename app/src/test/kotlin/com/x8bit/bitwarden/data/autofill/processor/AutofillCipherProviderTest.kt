package com.x8bit.bitwarden.data.autofill.processor

import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockPolicy
import com.bitwarden.vault.CardListView
import com.bitwarden.vault.CardView
import com.bitwarden.vault.CipherListView
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.CipherType
import com.bitwarden.vault.CipherView
import com.bitwarden.vault.DecryptCipherListResult
import com.bitwarden.vault.LoginListView
import com.bitwarden.vault.LoginView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProviderImpl
import com.x8bit.bitwarden.data.autofill.util.card
import com.x8bit.bitwarden.data.autofill.util.login
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.ciphermatching.CipherMatchingManager
import com.x8bit.bitwarden.data.platform.util.subtitle
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.data.vault.repository.util.statusFor
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
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
import timber.log.Timber

@OptIn(ExperimentalCoroutinesApi::class)
class AutofillCipherProviderTest {
    private val cardListView: CardListView = mockk {
        every { brand } returns "Visa"
    }
    private val cardCipherListView: CipherListView = mockk {
        every { card } returns cardListView
        every { deletedDate } returns null
        every { id } returns CARD_CIPHER_ID
        every { name } returns CARD_NAME
        every { reprompt } returns CipherRepromptType.NONE
        every { organizationId } returns null
        every { type } returns CipherListViewType.Card(v1 = cardListView)
    }
    private val cardView: CardView = mockk {
        every { cardholderName } returns CARD_CARDHOLDER_NAME
        every { code } returns CARD_CODE
        every { expMonth } returns CARD_EXP_MONTH
        every { expYear } returns CARD_EXP_YEAR
        every { number } returns CARD_NUMBER
        every { brand } returns CARD_BRAND
    }
    private val cardCipherView: CipherView = mockk {
        every { card } returns cardView
        every { deletedDate } returns null
        every { id } returns CARD_CIPHER_ID
        every { name } returns CARD_NAME
        every { reprompt } returns CipherRepromptType.NONE
        every { type } returns CipherType.CARD
    }
    private val loginListViewWithoutTotp: LoginListView = mockk {
        every { username } returns LOGIN_USERNAME
        every { totp } returns null
    }
    private val loginCipherListViewWithoutTotp: CipherListView = mockk {
        every { deletedDate } returns null
        every { id } returns LOGIN_WITHOUT_TOTP_CIPHER_ID
        every { login } returns loginListViewWithoutTotp
        every { name } returns LOGIN_NAME
        every { reprompt } returns CipherRepromptType.NONE
        every { type } returns CipherListViewType.Login(v1 = loginListViewWithoutTotp)
    }
    private val loginListViewWithTotp: LoginListView = mockk {
        every { username } returns LOGIN_USERNAME
        every { totp } returns "TOTP-CODE"
    }
    private val loginCipherListViewWithTotp: CipherListView = mockk {
        every { deletedDate } returns null
        every { id } returns LOGIN_WITH_TOTP_CIPHER_ID
        every { login } returns loginListViewWithTotp
        every { name } returns LOGIN_NAME
        every { reprompt } returns CipherRepromptType.NONE
        every { type } returns CipherListViewType.Login(v1 = loginListViewWithTotp)
    }
    private val loginViewWithoutTotp: LoginView = mockk {
        every { password } returns LOGIN_PASSWORD
        every { username } returns LOGIN_USERNAME
        every { totp } returns null
    }
    private val loginCipherViewWithoutTotp: CipherView = mockk {
        every { deletedDate } returns null
        every { id } returns LOGIN_WITHOUT_TOTP_CIPHER_ID
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
        every { id } returns LOGIN_WITH_TOTP_CIPHER_ID
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
    private val mutableCipherListViewsWithFailuresStateFlow =
        MutableStateFlow<DataState<DecryptCipherListResult>>(DataState.Loading)
    private val vaultRepository: VaultRepository = mockk {
        every {
            decryptCipherListResultStateFlow
        } returns mutableCipherListViewsWithFailuresStateFlow
        every { vaultUnlockDataStateFlow } returns mutableVaultStateFlow
        every { isVaultUnlocked(ACTIVE_USER_ID) } answers {
            mutableVaultStateFlow.value.statusFor(ACTIVE_USER_ID) == VaultUnlockData.Status.UNLOCKED
        }
    }
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
        } returns emptyList()
    }

    private lateinit var autofillCipherProvider: AutofillCipherProvider

    @BeforeEach
    fun setup() {
        mockkStatic(CipherView::subtitle)
        autofillCipherProvider = AutofillCipherProviderImpl(
            authRepository = authRepository,
            cipherMatchingManager = cipherMatchingManager,
            vaultRepository = vaultRepository,
            policyManager = policyManager,
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
            mutableCipherListViewsWithFailuresStateFlow.value = DataState.Loading

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
    fun `getCardAutofillCiphers when unlocked should decrypt then return non-null, non-deleted, non-reprompt, and non-restricted card ciphers`() =
        runTest {
            val deletedCardCipherView: CipherListView = mockk {
                every { deletedDate } returns mockk()
                every { type } returns CipherListViewType.Card(cardListView)
            }
            val repromptCardCipherView: CipherListView = mockk {
                every { deletedDate } returns null
                every { reprompt } returns CipherRepromptType.PASSWORD
                every { type } returns CipherListViewType.Card(cardListView)
            }
            val restrictedCardCipherView: CipherListView = mockk {
                every { deletedDate } returns null
                every { type } returns CipherListViewType.Card(cardListView)
                every { reprompt } returns CipherRepromptType.NONE
                every { organizationId } returns ORGANIZATION_ID_WITH_CARD_TYPE_RESTRICTIONS
            }
            val personalVaultCardCipherView: CipherListView = mockk {
                every { deletedDate } returns null
                every { type } returns CipherListViewType.Card(cardListView)
                every { reprompt } returns CipherRepromptType.NONE
                every { organizationId } returns null
            }
            val decryptCipherListViewsResult = DecryptCipherListResult(
                successes = listOf(
                    cardCipherListView,
                    deletedCardCipherView,
                    repromptCardCipherView,
                    restrictedCardCipherView,
                    personalVaultCardCipherView,
                    loginCipherListViewWithTotp,
                    loginCipherListViewWithoutTotp,
                ),
                failures = emptyList(),
            )

            every {
                policyManager.getActivePolicies(PolicyTypeJson.RESTRICT_ITEM_TYPES)
            } returns listOf(
                createMockPolicy(
                    number = 1,
                    organizationId = ORGANIZATION_ID_WITH_CARD_TYPE_RESTRICTIONS,
                ),
            )
            coEvery {
                vaultRepository.getCipher(CARD_CIPHER_ID)
            } returns GetCipherResult.Success(
                cipherView = cardCipherView,
            )

            mutableCipherListViewsWithFailuresStateFlow.value = DataState.Loaded(
                data = decryptCipherListViewsResult,
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
            every { cardCipherListView.organizationId } returns ORGANIZATION_ID
            every { cardCipherListView.subtitle } returns CARD_SUBTITLE

            // Test & Verify
            val actual = autofillCipherProvider.getCardAutofillCiphers()

            coVerify(exactly = 1) { vaultRepository.getCipher(CARD_CIPHER_ID) }
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
            mutableCipherListViewsWithFailuresStateFlow.value = DataState.Loading

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
    fun `getLoginAutofillCiphers when unlocked should decrypt then return matched, non-deleted, non-reprompt, login ciphers`() =
        runTest {
            val deletedLoginCipherView: CipherListView = mockk {
                every { deletedDate } returns mockk()
                every { type } returns CipherListViewType.Login(v1 = mockk())
            }
            val repromptLoginCipherView: CipherListView = mockk {
                every { deletedDate } returns null
                every { reprompt } returns CipherRepromptType.PASSWORD
                every { type } returns CipherListViewType.Login(v1 = mockk())
            }
            val cipherViews = listOf(
                cardCipherListView,
                loginCipherListViewWithTotp,
                loginCipherListViewWithoutTotp,
                deletedLoginCipherView,
                repromptLoginCipherView,
            )
            val decryptCipherListResult = DecryptCipherListResult(
                successes = cipherViews,
                failures = emptyList(),
            )
            val filteredCipherViews = listOf(
                loginCipherListViewWithTotp,
                loginCipherListViewWithoutTotp,
            )
            coEvery {
                vaultRepository.getCipher(LOGIN_WITH_TOTP_CIPHER_ID)
            } returns GetCipherResult.Success(cipherView = loginCipherViewWithTotp)
            coEvery {
                vaultRepository.getCipher(LOGIN_WITHOUT_TOTP_CIPHER_ID)
            } returns GetCipherResult.Success(cipherView = loginCipherViewWithoutTotp)
            coEvery {
                cipherMatchingManager.filterCiphersForMatches(
                    cipherListViews = filteredCipherViews,
                    matchUri = URI,
                )
            } returns filteredCipherViews
            mutableCipherListViewsWithFailuresStateFlow.value = DataState.Loaded(
                data = decryptCipherListResult,
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
            coVerify(exactly = 1) { vaultRepository.getCipher(LOGIN_WITH_TOTP_CIPHER_ID) }
            coVerify {
                cipherMatchingManager.filterCiphersForMatches(
                    cipherListViews = filteredCipherViews,
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

    @Suppress("MaxLineLength")
    @Test
    fun `getLoginAutofillCiphers when decryption fails should log the error and omit cipher from results`() =
        runTest {
            mockkObject(Timber.Forest)
            val cipherListViews = listOf(
                loginCipherListViewWithTotp,
                loginCipherListViewWithoutTotp,
            )

            coEvery {
                vaultRepository.getCipher(LOGIN_WITH_TOTP_CIPHER_ID)
            } returns GetCipherResult.Failure(
                error = Exception("Decryption failed"),
            )

            coEvery {
                vaultRepository.getCipher(LOGIN_WITHOUT_TOTP_CIPHER_ID)
            } returns GetCipherResult.Success(
                cipherView = loginCipherViewWithoutTotp,
            )
            coEvery {
                cipherMatchingManager.filterCiphersForMatches(
                    cipherListViews = cipherListViews,
                    matchUri = URI,
                )
            } returns cipherListViews

            mutableCipherListViewsWithFailuresStateFlow.value = DataState.Loaded(
                data = DecryptCipherListResult(
                    successes = cipherListViews,
                    failures = emptyList(),
                ),
            )
            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(
                    userId = ACTIVE_USER_ID,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )
            val expected = listOf(
                LOGIN_AUTOFILL_CIPHER_WITHOUT_TOTP,
            )

            every { loginCipherViewWithoutTotp.subtitle } returns LOGIN_SUBTITLE

            val actual = autofillCipherProvider.getLoginAutofillCiphers(
                uri = URI,
            )

            assertEquals(
                expected,
                actual,
            )

            verify(exactly = 1) {
                Timber.Forest.e(
                    t = any(),
                    message = "Failed to decrypt cipher for autofill.",
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getLoginAutofillCiphers when decryption result is CipherNotFound should log the error and omit cipher from results`() =
        runTest {
            mockkObject(Timber.Forest)
            val cipherListViews = listOf(
                loginCipherListViewWithTotp,
                loginCipherListViewWithoutTotp,
            )

            coEvery {
                vaultRepository.getCipher(LOGIN_WITH_TOTP_CIPHER_ID)
            } returns GetCipherResult.CipherNotFound

            coEvery {
                vaultRepository.getCipher(LOGIN_WITHOUT_TOTP_CIPHER_ID)
            } returns GetCipherResult.Success(
                cipherView = loginCipherViewWithoutTotp,
            )
            coEvery {
                cipherMatchingManager.filterCiphersForMatches(
                    cipherListViews = cipherListViews,
                    matchUri = URI,
                )
            } returns cipherListViews

            mutableCipherListViewsWithFailuresStateFlow.value = DataState.Loaded(
                data = DecryptCipherListResult(
                    successes = cipherListViews,
                    failures = emptyList(),
                ),
            )
            mutableVaultStateFlow.value = listOf(
                VaultUnlockData(
                    userId = ACTIVE_USER_ID,
                    status = VaultUnlockData.Status.UNLOCKED,
                ),
            )
            val expected = listOf(
                LOGIN_AUTOFILL_CIPHER_WITHOUT_TOTP,
            )

            every { loginCipherViewWithoutTotp.subtitle } returns LOGIN_SUBTITLE

            val actual = autofillCipherProvider.getLoginAutofillCiphers(
                uri = URI,
            )

            assertEquals(
                expected,
                actual,
            )

            verify(exactly = 1) {
                Timber.Forest.e("Cipher not found for autofill.")
            }
        }
}

private const val ACTIVE_USER_ID = "activeUserId"
private const val ORGANIZATION_ID = "organizationId"
private const val ORGANIZATION_ID_WITH_CARD_TYPE_RESTRICTIONS =
    "organizationIdWithCardTypeRestrictions"
private const val CARD_CARDHOLDER_NAME = "John Doe"
private const val CARD_CODE = "123"
private const val CARD_EXP_MONTH = "January"
private const val CARD_EXP_YEAR = "2029"
private const val CARD_NAME = "John's Card"
private const val CARD_NUMBER = "1234567890"
private const val CARD_BRAND = "Visa"
private const val CARD_SUBTITLE = "$CARD_BRAND, *7890"
private const val LOGIN_WITH_TOTP_CIPHER_ID = "1234567890"
private const val LOGIN_WITHOUT_TOTP_CIPHER_ID = "ABCDEFGHIJ"
private const val CARD_CIPHER_ID = "0987654321"
private val CARD_AUTOFILL_CIPHER = AutofillCipher.Card(
    cardholderName = CARD_CARDHOLDER_NAME,
    cipherId = CARD_CIPHER_ID,
    code = CARD_CODE,
    expirationMonth = CARD_EXP_MONTH,
    expirationYear = CARD_EXP_YEAR,
    name = CARD_NAME,
    number = CARD_NUMBER,
    subtitle = CARD_SUBTITLE,
    brand = CARD_BRAND,
)
private const val LOGIN_NAME = "John's Login"
private const val LOGIN_PASSWORD = "Password123"
private const val LOGIN_SUBTITLE = "John Doe"
private const val LOGIN_USERNAME = "John-Bitwarden"
private const val URI: String = "androidapp://com.x8bit.bitwarden"
private val LOGIN_AUTOFILL_CIPHER_WITH_TOTP = AutofillCipher.Login(
    cipherId = LOGIN_WITH_TOTP_CIPHER_ID,
    isTotpEnabled = true,
    name = LOGIN_NAME,
    password = LOGIN_PASSWORD,
    subtitle = LOGIN_SUBTITLE,
    username = LOGIN_USERNAME,
    website = URI,
)
private val LOGIN_AUTOFILL_CIPHER_WITHOUT_TOTP = AutofillCipher.Login(
    cipherId = LOGIN_WITHOUT_TOTP_CIPHER_ID,
    isTotpEnabled = false,
    name = LOGIN_NAME,
    password = LOGIN_PASSWORD,
    subtitle = LOGIN_SUBTITLE,
    username = LOGIN_USERNAME,
    website = URI,
)
