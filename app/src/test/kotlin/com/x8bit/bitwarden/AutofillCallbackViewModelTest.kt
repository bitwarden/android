package com.x8bit.bitwarden

import android.content.Intent
import app.cash.turbine.test
import com.bitwarden.ui.platform.base.BaseViewModelTest
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCallbackData
import com.x8bit.bitwarden.data.autofill.util.getAutofillCallbackIntentOrNull
import com.x8bit.bitwarden.data.vault.manager.model.GetCipherResult
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.awaits
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillCallbackViewModelTest : BaseViewModelTest() {
    private lateinit var autofillCallbackViewModel: AutofillCallbackViewModel

    private val mutableVaultUnlockDataStateFlow: MutableStateFlow<List<VaultUnlockData>> =
        MutableStateFlow(emptyList())

    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns null
    }
    private val vaultRepository: VaultRepository = mockk {
        every { vaultUnlockDataStateFlow } returns mutableVaultUnlockDataStateFlow
    }

    private val intent: Intent = mockk()

    @BeforeEach
    fun setup() {
        mockkStatic(Intent::getAutofillCallbackIntentOrNull)

        autofillCallbackViewModel = AutofillCallbackViewModel(
            authRepository = authRepository,
            vaultRepository = vaultRepository,
        )
    }

    @AfterEach
    fun teardown() {
        unmockkStatic(Intent::getAutofillCallbackIntentOrNull)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit CompleteAutofill when cipherID is extracted, vault unlocked, and cipherView found`() =
        runTest {
            // Setup
            val cipherView: CipherView = mockk {
                every { id } returns CIPHER_ID
            }
            val totpCopyData = AutofillCallbackData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillCallbackAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillCallbackEvent.CompleteAutofill(
                cipherView = cipherView,
            )
            val vaultUnlockData = VaultUnlockData(
                userId = ACTIVE_USER_ID,
                status = VaultUnlockData.Status.UNLOCKED,
            )
            every { intent.getAutofillCallbackIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns true
            coEvery {
                vaultRepository.getCipher(cipherId = CIPHER_ID)
            } returns GetCipherResult.Success(cipherView)
            mutableVaultUnlockDataStateFlow.value = listOf(vaultUnlockData)

            // Test
            autofillCallbackViewModel.trySendAction(action)

            // Verify
            autofillCallbackViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is not`() = runTest {
        // Setup
        val action = AutofillCallbackAction.IntentReceived(
            intent = intent,
        )
        val expectedEvent = AutofillCallbackEvent.FinishActivity
        every { intent.getAutofillCallbackIntentOrNull() } returns null

        // Test
        autofillCallbackViewModel.trySendAction(action)

        // Verify
        autofillCallbackViewModel.eventFlow.test {
            assertEquals(expectedEvent, awaitItem())
            expectNoEvents()
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is extracted and no active user`() =
        runTest {
            // Setup
            val totpCopyData = AutofillCallbackData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillCallbackAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillCallbackEvent.FinishActivity
            every { intent.getAutofillCallbackIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns null

            // Test
            autofillCallbackViewModel.trySendAction(action)

            // Verify
            autofillCallbackViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is extracted and vault locked`() =
        runTest {
            // Setup
            val totpCopyData = AutofillCallbackData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillCallbackAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillCallbackEvent.FinishActivity
            every { intent.getAutofillCallbackIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns false

            // Test
            autofillCallbackViewModel.trySendAction(action)

            // Verify
            autofillCallbackViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `on IntentReceived should emit FinishActivity when cipherID is extracted, vault unlocked, and cipherView not found`() =
        runTest {
            // Setup
            val totpCopyData = AutofillCallbackData(
                cipherId = CIPHER_ID,
            )
            val action = AutofillCallbackAction.IntentReceived(
                intent = intent,
            )
            val expectedEvent = AutofillCallbackEvent.FinishActivity
            val vaultUnlockData = VaultUnlockData(
                userId = ACTIVE_USER_ID,
                status = VaultUnlockData.Status.UNLOCKED,
            )
            every { intent.getAutofillCallbackIntentOrNull() } returns totpCopyData
            every { authRepository.activeUserId } returns ACTIVE_USER_ID
            every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns true
            coEvery {
                vaultRepository.getCipher(cipherId = CIPHER_ID)
            } returns GetCipherResult.CipherNotFound
            mutableVaultUnlockDataStateFlow.value = listOf(vaultUnlockData)

            // Test
            autofillCallbackViewModel.trySendAction(action)

            // Verify
            autofillCallbackViewModel.eventFlow.test {
                assertEquals(expectedEvent, awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `on IntentReceived should emit FinishActivity when timeout is elapsed`() = runTest {
        // Setup
        val totpCopyData = AutofillCallbackData(
            cipherId = CIPHER_ID,
        )
        val action = AutofillCallbackAction.IntentReceived(
            intent = intent,
        )
        val expectedEvent = AutofillCallbackEvent.FinishActivity
        val vaultUnlockData = VaultUnlockData(
            userId = ACTIVE_USER_ID,
            status = VaultUnlockData.Status.UNLOCKED,
        )
        every { intent.getAutofillCallbackIntentOrNull() } returns totpCopyData
        every { authRepository.activeUserId } returns ACTIVE_USER_ID
        every { vaultRepository.isVaultUnlocked(userId = ACTIVE_USER_ID) } returns true
        coEvery { vaultRepository.getCipher(cipherId = CIPHER_ID) } just awaits
        mutableVaultUnlockDataStateFlow.value = listOf(vaultUnlockData)

        // Test
        autofillCallbackViewModel.trySendAction(action)

        // Verify
        autofillCallbackViewModel.eventFlow.test {
            assertEquals(expectedEvent, awaitItem())
            expectNoEvents()
        }
    }
}

private const val ACTIVE_USER_ID: String = "ACTIVE_USER_ID"
private const val CIPHER_ID: String = "1234567890"
