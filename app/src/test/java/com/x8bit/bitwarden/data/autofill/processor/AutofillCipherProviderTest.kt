package com.x8bit.bitwarden.data.autofill.processor

import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.autofill.model.AutofillCipher
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProvider
import com.x8bit.bitwarden.data.autofill.provider.AutofillCipherProviderImpl
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultState
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class AutofillCipherProviderTest {

    private val authRepository: AuthRepository = mockk {
        every { activeUserId } returns ACTIVE_USER_ID
    }
    private val mutableVaultStateFlow = MutableStateFlow<VaultState>(
        VaultState(
            unlockingVaultUserIds = emptySet(),
            unlockedVaultUserIds = emptySet(),
        ),
    )
    private val vaultRepository: VaultRepository = mockk {
        every { vaultStateFlow } returns mutableVaultStateFlow
        every { isVaultUnlocked(ACTIVE_USER_ID) } answers {
            ACTIVE_USER_ID in mutableVaultStateFlow.value.unlockedVaultUserIds
        }
    }

    private lateinit var autofillCipherProvider: AutofillCipherProvider

    @BeforeEach
    fun setup() {
        autofillCipherProvider = AutofillCipherProviderImpl(
            authRepository = authRepository,
            vaultRepository = vaultRepository,
        )
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
            mutableVaultStateFlow.value = VaultState(
                unlockedVaultUserIds = emptySet(),
                unlockingVaultUserIds = setOf(ACTIVE_USER_ID),
            )

            val result = async {
                autofillCipherProvider.isVaultLocked()
            }

            testScheduler.advanceUntilIdle()
            assertFalse(result.isCompleted)

            mutableVaultStateFlow.value = VaultState(
                unlockedVaultUserIds = setOf(ACTIVE_USER_ID),
                unlockingVaultUserIds = emptySet(),
            )

            testScheduler.advanceUntilIdle()
            assertTrue(result.isCompleted)

            assertFalse(result.await())
        }

    @Test
    fun `getCardAutofillCiphers when unlocked should return default list of card ciphers`() =
        runTest {
            mutableVaultStateFlow.value = VaultState(
                unlockedVaultUserIds = setOf(ACTIVE_USER_ID),
                unlockingVaultUserIds = emptySet(),
            )

            // Test & Verify
            val actual = autofillCipherProvider.getCardAutofillCiphers()

            assertEquals(CARD_CIPHERS, actual)
        }

    @Test
    fun `getCardAutofillCiphers when locked should return an empty list`() = runTest {
        mutableVaultStateFlow.value = VaultState(
            unlockedVaultUserIds = emptySet(),
            unlockingVaultUserIds = emptySet(),
        )

        // Test & Verify
        val actual = autofillCipherProvider.getCardAutofillCiphers()

        assertEquals(emptyList<AutofillCipher.Card>(), actual)
    }

    @Test
    fun `getLoginAutofillCiphers when unlocked should return default list of login ciphers`() =
        runTest {
            mutableVaultStateFlow.value = VaultState(
                unlockedVaultUserIds = setOf(ACTIVE_USER_ID),
                unlockingVaultUserIds = emptySet(),
            )

            // Test & Verify
            val actual = autofillCipherProvider.getLoginAutofillCiphers(
                uri = URI,
            )

            assertEquals(LOGIN_CIPHERS, actual)
        }

    @Test
    fun `getLoginAutofillCiphers when locked should return an empty list`() = runTest {
        mutableVaultStateFlow.value = VaultState(
            unlockedVaultUserIds = emptySet(),
            unlockingVaultUserIds = emptySet(),
        )

        // Test & Verify
        val actual = autofillCipherProvider.getLoginAutofillCiphers(
            uri = URI,
        )

        assertEquals(emptyList<AutofillCipher.Login>(), actual)
    }
}

private const val ACTIVE_USER_ID = "activeUserId"

private val CARD_CIPHERS = listOf(
    AutofillCipher.Card(
        cardholderName = "John",
        code = "123",
        expirationMonth = "January",
        expirationYear = "1999",
        name = "John",
        number = "1234567890",
        subtitle = "123...",
    ),
    AutofillCipher.Card(
        cardholderName = "Doe",
        code = "456",
        expirationMonth = "December",
        expirationYear = "2024",
        name = "Doe",
        number = "0987654321",
        subtitle = "098...",
    ),
)
private val LOGIN_CIPHERS = listOf(
    AutofillCipher.Login(
        name = "Bitwarden1",
        password = "password123",
        subtitle = "John-Bitwarden",
        username = "John-Bitwarden",
    ),
    AutofillCipher.Login(
        name = "Bitwarden2",
        password = "password123",
        subtitle = "Doe-Bitwarden",
        username = "Doe-Bitwarden",
    ),
)
private const val URI: String = "androidapp://com.x8bit.bitwarden"
