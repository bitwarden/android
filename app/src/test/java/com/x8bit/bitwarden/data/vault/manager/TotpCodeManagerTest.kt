package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.TotpResponse
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.repository.model.DataState
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginView
import com.x8bit.bitwarden.data.vault.manager.model.VerificationCodeItem
import com.x8bit.bitwarden.ui.vault.feature.verificationcode.util.createVerificationCodeItem
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TotpCodeManagerTest {
    private val userId = "userId"
    private val cipherList = listOf(
        createMockCipherView(1, isDeleted = false),
    )

    private val vaultSdkSource: VaultSdkSource = mockk()
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val totpCodeManager: TotpCodeManager = TotpCodeManagerImpl(
        vaultSdkSource = vaultSdkSource,
        dispatcherManager = dispatcherManager,
        clock = clock,
    )

    @Test
    fun `getTotpCodeStateFlow should have loaded data with a valid values passed in`() = runTest {
        val totpResponse = TotpResponse("123456", 30u)
        coEvery {
            vaultSdkSource.generateTotp(any(), any(), any())
        } returns totpResponse.asSuccess()

        val expected = createVerificationCodeItem().copy(orgId = "mockOrganizationId-1")

        totpCodeManager.getTotpCodesStateFlow(userId, cipherList).test {
            assertEquals(DataState.Loaded(listOf(expected)), awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `getTotpCodeStateFlow should have loaded data with empty list if no totp code is provided`() =
        runTest {
            val totpResponse = TotpResponse("123456", 30u)
            coEvery {
                vaultSdkSource.generateTotp(any(), any(), any())
            } returns totpResponse.asSuccess()

            val cipherView = createMockCipherView(1).copy(
                login = createMockLoginView(number = 1, clock = clock).copy(
                    totp = null,
                ),
            )

            totpCodeManager.getTotpCodesStateFlow(userId, listOf(cipherView)).test {
                assertEquals(DataState.Loaded(emptyList<VerificationCodeItem>()), awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getTotpCodesStateFlow should have loaded data with empty list if unable to generate auth code`() =
        runTest {
            coEvery {
                vaultSdkSource.generateTotp(any(), any(), any())
            } returns Exception().asFailure()

            val cipherView = createMockCipherView(1).copy(
                login = createMockLoginView(number = 1, clock = clock).copy(
                    totp = null,
                ),
            )

            totpCodeManager.getTotpCodesStateFlow(userId, listOf(cipherView)).test {
                assertEquals(DataState.Loaded(emptyList<VerificationCodeItem>()), awaitItem())
            }
        }

    @Test
    fun `getTotpCodeStateFlow should have loaded item with valid data passed in`() = runTest {
        val totpResponse = TotpResponse("123456", 30u)
        coEvery {
            vaultSdkSource.generateTotp(any(), any(), any())
        } returns totpResponse.asSuccess()

        val cipherView = createMockCipherView(
            number = 1,
            repromptType = CipherRepromptType.PASSWORD,
        )

        val expected = createVerificationCodeItem().copy(
            hasPasswordReprompt = true,
            orgId = cipherView.organizationId,
        )

        totpCodeManager.getTotpCodeStateFlow(userId, cipherView).test {
            assertEquals(DataState.Loaded(expected), awaitItem())
        }
    }

    @Test
    fun `getTotpCodeFlow should have null data if unable to get item`() =
        runTest {
            val totpResponse = TotpResponse("123456", 30u)
            coEvery {
                vaultSdkSource.generateTotp(any(), any(), any())
            } returns totpResponse.asSuccess()

            val cipherView = createMockCipherView(1).copy(
                login = null,
            )

            totpCodeManager.getTotpCodeStateFlow(userId, cipherView).test {
                assertEquals(DataState.Loaded(null), awaitItem())
            }
        }
}
