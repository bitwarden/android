package com.bitwarden.authenticator.data.authenticator.manager.util

import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManagerImpl
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.vault.TotpResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class TotpCodeManagerTest {

    private val clock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val authenticatorSdkSource: AuthenticatorSdkSource = mockk()
    private val dispatcherManager = FakeDispatcherManager()

    private val manager = TotpCodeManagerImpl(
        authenticatorSdkSource = authenticatorSdkSource,
        clock = clock,
        dispatcherManager = dispatcherManager,
    )

    @Test
    fun `getTotpCodesFlow should emit empty list when input list is empty`() =
        runTest {
            manager.getTotpCodesFlow(emptyList()).test {
                assertEquals(emptyList<VerificationCodeItem>(), awaitItem())
            }
        }

    @Test
    fun `getTotpCodesFlow should emit data if a valid value is passed in`() =
        runTest {
            val totp = "otpUri"
            val authenticatorItems = listOf(
                createMockAuthenticatorItem(number = 1, otpUri = totp),
            )
            val code = "123456"
            val totpResponse = TotpResponse(code = code, period = 30u)
            coEvery {
                authenticatorSdkSource.generateTotp(totp = totp, time = clock.instant())
            } returns totpResponse.asSuccess()

            val expected = createMockVerificationCodeItem(
                number = 1,
                code = code,
                issueTime = clock.instant().toEpochMilli(),
                timeLeftSeconds = 30,
            )

            manager.getTotpCodesFlow(authenticatorItems).test {
                assertEquals(listOf(expected), awaitItem())
            }
        }

    @Test
    fun `getTotpCodesFlow should emit empty list if unable to generate auth code`() =
        runTest {
            val totp = "otpUri"
            val authenticatorItems = listOf(
                createMockAuthenticatorItem(number = 1, otpUri = totp),
            )
            coEvery {
                authenticatorSdkSource.generateTotp(totp = totp, time = clock.instant())
            } returns Exception().asFailure()

            manager.getTotpCodesFlow(authenticatorItems).test {
                assertEquals(
                    emptyList<VerificationCodeItem>(),
                    awaitItem(),
                )
            }
        }
}
