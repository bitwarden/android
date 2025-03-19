package com.bitwarden.authenticator.data.authenticator.manager.util

import app.cash.turbine.test
import com.bitwarden.authenticator.data.authenticator.datasource.sdk.AuthenticatorSdkSource
import com.bitwarden.authenticator.data.authenticator.manager.TotpCodeManagerImpl
import com.bitwarden.authenticator.data.authenticator.manager.model.VerificationCodeItem
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

    private val manager = TotpCodeManagerImpl(
        authenticatorSdkSource = authenticatorSdkSource,
        clock = clock,
    )

    @Test
    fun `getTotpCodesFlow should return flow that emits empty list when input list is empty`() =
        runTest {
            manager.getTotpCodesFlow(emptyList()).test {
                assertEquals(emptyList<VerificationCodeItem>(), awaitItem())
                awaitComplete()
            }
        }
}
