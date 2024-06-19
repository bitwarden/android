package com.x8bit.bitwarden

import android.content.Intent
import app.cash.turbine.test
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillTotpCopyData
import com.x8bit.bitwarden.data.autofill.util.getTotpCopyIntentOrNull
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import com.x8bit.bitwarden.ui.platform.base.BaseViewModelTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillTotpCopyViewModelTest : BaseViewModelTest() {
    private lateinit var autofillTotpCopyViewModel: AutofillTotpCopyViewModel

    private val mutableCiphersStateFlow: MutableStateFlow<DataState<List<CipherView>>> =
        MutableStateFlow(DataState.Loading)
    private val mutableVaultUnlockDataStateFlow: MutableStateFlow<List<VaultUnlockData>> =
        MutableStateFlow(emptyList())

    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns null
    }
    private val vaultRepository: VaultRepository = mockk {
        every { ciphersStateFlow } returns mutableCiphersStateFlow
        every { vaultUnlockDataStateFlow } returns mutableVaultUnlockDataStateFlow
    }

    private val intent: Intent = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(Intent::getTotpCopyIntentOrNull)

        autofillTotpCopyViewModel = AutofillTotpCopyViewModel(
            authRepository = authRepository,
            vaultRepository = vaultRepository,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Intent::getTotpCopyIntentOrNull)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit CompleteAutofill when cipherID is extracted, vault unlocked, and cipherView found`() =
        runTest {
            // Setup
            val cipherView: CipherView = mockk {
                every { id } returns CIPHER_ID
            }
            val totpCopyData = AutofillTotpCopyData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillTotpCopyAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillTotpCopyEvent.CompleteAutofill(
                cipherView = cipherView,
            )
            val vaultUnlockData = VaultUnlockData(
                userId = ACTIVE_USER_ID,
                status = VaultUnlockData.Status.UNLOCKED,
            )
            every { intent.getTotpCopyIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns true
            mutableCiphersStateFlow.value = DataState.Loaded(
                listOf(
                    cipherView,
                ),
            )
            mutableVaultUnlockDataStateFlow.value = listOf(vaultUnlockData)

            // Test
            autofillTotpCopyViewModel.trySendAction(action)

            // Verify
            autofillTotpCopyViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is not`() = runTest {
        // Setup
        val action = AutofillTotpCopyAction.IntentReceived(
            intent = intent,
        )
        val expectedEvent = AutofillTotpCopyEvent.FinishActivity
        every { intent.getTotpCopyIntentOrNull() } returns null

        // Test
        autofillTotpCopyViewModel.trySendAction(action)

        // Verify
        autofillTotpCopyViewModel.eventFlow.test {
            assertEquals(expectedEvent, awaitItem())
            expectNoEvents()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is extracted and no active user`() =
        runTest {
            // Setup
            val totpCopyData = AutofillTotpCopyData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillTotpCopyAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillTotpCopyEvent.FinishActivity
            every { intent.getTotpCopyIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns null

            // Test
            autofillTotpCopyViewModel.trySendAction(action)

            // Verify
            autofillTotpCopyViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is extracted and vault locked`() =
        runTest {
            // Setup
            val totpCopyData = AutofillTotpCopyData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillTotpCopyAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillTotpCopyEvent.FinishActivity
            every { intent.getTotpCopyIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns false

            // Test
            autofillTotpCopyViewModel.trySendAction(action)

            // Verify
            autofillTotpCopyViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is extracted, vault unlocked, and cipherView not found`() =
        runTest {
            // Setup
            val cipherView: CipherView = mockk {
                every { id } returns "NEW CIPHER ID"
            }
            val totpCopyData = AutofillTotpCopyData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillTotpCopyAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillTotpCopyEvent.FinishActivity
            val vaultUnlockData = VaultUnlockData(
                userId = ACTIVE_USER_ID,
                status = VaultUnlockData.Status.UNLOCKED,
            )
            every { intent.getTotpCopyIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns true
            mutableCiphersStateFlow.value = DataState.Loaded(
                listOf(
                    cipherView,
                ),
            )
            mutableVaultUnlockDataStateFlow.value = listOf(vaultUnlockData)

            // Test
            autofillTotpCopyViewModel.trySendAction(action)

            // Verify
            autofillTotpCopyViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `on IntentReceived should emit FinishActivity when timeout is elapsed`() = runTest {
        // Setup
        val totpCopyData = AutofillTotpCopyData(
            cipherId = CIPHER_ID,
        )
        val action = AutofillTotpCopyAction.IntentReceived(
            intent = intent,
        )
        val expectedEvent = AutofillTotpCopyEvent.FinishActivity
        val vaultUnlockData = VaultUnlockData(
            userId = ACTIVE_USER_ID,
            status = VaultUnlockData.Status.UNLOCKED,
        )
        every { intent.getTotpCopyIntentOrNull() } returns totpCopyData
        every { authRepository.activeUserId } returns ACTIVE_USER_ID
        every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns true
        mutableVaultUnlockDataStateFlow.value = listOf(vaultUnlockData)

        // Test
        autofillTotpCopyViewModel.trySendAction(action)

        // Verify
        autofillTotpCopyViewModel.eventFlow.test {
            assertEquals(expectedEvent, awaitItem())
            expectNoEvents()
        }
    }
}

private const val ACTIVE_USER_ID: String = "ACTIVE_USER_ID"
private const val CIPHER_ID: String = "1234567890"
