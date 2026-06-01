package com.x8bit.bitwarden.data.vault.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.vault.CipherListViewType
import com.bitwarden.vault.CipherRepromptType
import com.bitwarden.vault.TotpResponse
import com.x8bit.bitwarden.data.vault.datasource.sdk.VaultSdkSource
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockLoginListView
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
    @Suppress("MaxLineLength")
    fun `getTotpCodesForCipherListViewsStateFlow should have loaded data with a valid values passed in`() =
        runTest {
            val cipherListViews = listOf(
                createMockCipherListView(number = 1),
            )
            val totpResponse = TotpResponse("123456", 30u)
            coEvery {
                vaultSdkSource.generateTotpForCipherListView(
                    userId = any(),
                    cipherListView = any(),
                    time = any(),
                )
            } returns totpResponse.asSuccess()

            val expected = createVerificationCodeItem()

            totpCodeManager.getTotpCodesForCipherListViewsStateFlow(userId, cipherListViews).test {
                assertEquals(DataState.Loaded(listOf(expected)), awaitItem())
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `getTotpCodesForCipherListViewsStateFlow should have loaded data with empty list if no totp code is provided`() =
        runTest {
            val totpResponse = TotpResponse("123456", 30u)
            coEvery {
                vaultSdkSource.generateTotpForCipherListView(
                    userId = any(),
                    cipherListView = any(),
                    time = any(),
                )
            } returns totpResponse.asSuccess()

            val cipherListView = createMockCipherListView(
                number = 1,
                type = CipherListViewType.Login(
                    createMockLoginListView(
                        number = 1,
                        totp = null,
                    ),
                ),
            )

            totpCodeManager.getTotpCodesForCipherListViewsStateFlow(userId, listOf(cipherListView))
                .test {
                    assertEquals(DataState.Loaded(emptyList<VerificationCodeItem>()), awaitItem())
                }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `getTotpCodesForCipherListViewsStateFlow should have loaded data with empty list if unable to generate auth code`() =
        runTest {
            coEvery {
                vaultSdkSource.generateTotpForCipherListView(
                    userId = any(),
                    cipherListView = any(),
                    time = any(),
                )
            } returns Exception().asFailure()

            val cipherListView = createMockCipherListView(
                number = 1,
                type = CipherListViewType.Login(createMockLoginListView(number = 1)),
            )

            totpCodeManager.getTotpCodesForCipherListViewsStateFlow(userId, listOf(cipherListView)).test {
                assertEquals(DataState.Loaded(emptyList<VerificationCodeItem>()), awaitItem())
            }
        }

    @Test
    @Suppress("MaxLineLength")
    fun `getTotpCodeStateFlow from CipherListView should have loaded item with valid data passed in`() =
        runTest {
            val totpResponse = TotpResponse("123456", 30u)
            coEvery {
                vaultSdkSource.generateTotpForCipherListView(
                    userId = any(),
                    cipherListView = any(),
                    time = any(),
                )
            } returns totpResponse.asSuccess()

            val cipherListView = createMockCipherListView(
                number = 1,
                reprompt = CipherRepromptType.PASSWORD,
            )

            val expected = createVerificationCodeItem().copy(hasPasswordReprompt = true)

            totpCodeManager.getTotpCodeStateFlow(userId, cipherListView).test {
                assertEquals(DataState.Loaded(expected), awaitItem())
            }
        }

    @Test
    fun `getTotpCodeFlow from CipherListView should have null data if unable to get item`() =
        runTest {
            val totpResponse = TotpResponse("123456", 30u)
            coEvery {
                vaultSdkSource.generateTotpForCipherListView(
                    userId = any(),
                    time = any(),
                    cipherListView = any(),
                )
            } returns totpResponse.asSuccess()

            val cipherListView = createMockCipherListView(
                number = 1,
                type = CipherListViewType.SshKey,
            )

            totpCodeManager.getTotpCodeStateFlow(userId, cipherListView).test {
                assertEquals(DataState.Loaded(null), awaitItem())
            }
        }
}
