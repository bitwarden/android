package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.TimeZone

class PushManagerTest {
    private val clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        TimeZone.getTimeZone("UTC").toZoneId(),
    )

    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()

    private val authDiskSource: AuthDiskSource = FakeAuthDiskSource()

    private val pushDiskSource: PushDiskSource = PushDiskSourceImpl(FakeSharedPreferences())

    private val pushService: PushService = mockk()

    private lateinit var pushManager: PushManager

    @BeforeEach
    fun setUp() {
        pushManager = PushManagerImpl(
            authDiskSource = authDiskSource,
            pushDiskSource = pushDiskSource,
            pushService = pushService,
            dispatcherManager = dispatcherManager,
            clock = clock,
        )
    }

    @Nested
    inner class NullUserState {
        @BeforeEach
        fun setUp() {
            authDiskSource.userState = null
        }

        @Test
        fun `registerPushTokenIfNecessary should update registeredPushToken`() {
            assertEquals(null, pushDiskSource.registeredPushToken)

            val token = "token"
            pushManager.registerPushTokenIfNecessary(token)

            assertEquals(token, pushDiskSource.registeredPushToken)
        }

        @Test
        fun `registerStoredPushTokenIfNecessary should do nothing`() {
            pushManager.registerStoredPushTokenIfNecessary()

            assertNull(pushDiskSource.registeredPushToken)
        }
    }

    @Nested
    inner class NonNullUserState {
        private val existingToken = "existingToken"
        private val userId = "userId"

        @BeforeEach
        fun setUp() {
            pushDiskSource.storeCurrentPushToken(userId, existingToken)
            authDiskSource.userState = UserStateJson(userId, mapOf(userId to mockk()))
        }

        @Suppress("MaxLineLength")
        @Test
        fun `registerStoredPushTokenIfNecessary should do nothing if registered less than a day before`() {
            val lastRegistration = ZonedDateTime.ofInstant(
                clock.instant().minus(23, ChronoUnit.HOURS),
                ZoneOffset.UTC,
            )
            pushDiskSource.registeredPushToken = existingToken
            pushDiskSource.storeLastPushTokenRegistrationDate(
                userId,
                lastRegistration,
            )
            pushManager.registerStoredPushTokenIfNecessary()

            // Assert the last registration value has not changed
            assertEquals(
                lastRegistration.toEpochSecond(),
                pushDiskSource.getLastPushTokenRegistrationDate(userId)!!.toEpochSecond(),
            )
        }

        @Nested
        inner class MatchingToken {
            private val newToken = "existingToken"

            @Suppress("MaxLineLength")
            @Test
            fun `registerPushTokenIfNecessary should update registeredPushToken and lastPushTokenRegistrationDate`() {
                pushManager.registerPushTokenIfNecessary(newToken)

                coVerify(exactly = 0) { pushService.putDeviceToken(any()) }
                assertEquals(newToken, pushDiskSource.registeredPushToken)
                assertEquals(
                    clock.instant().epochSecond,
                    pushDiskSource.getLastPushTokenRegistrationDate(userId)?.toEpochSecond(),
                )
            }

            @Suppress("MaxLineLength")
            @Test
            fun `registerStoredPushTokenIfNecessary should update registeredPushToken and lastPushTokenRegistrationDate`() {
                pushDiskSource.registeredPushToken = newToken
                pushManager.registerStoredPushTokenIfNecessary()

                coVerify(exactly = 0) { pushService.putDeviceToken(any()) }
                assertEquals(newToken, pushDiskSource.registeredPushToken)
                assertEquals(
                    clock.instant().epochSecond,
                    pushDiskSource.getLastPushTokenRegistrationDate(userId)?.toEpochSecond(),
                )
            }
        }

        @Nested
        inner class DifferentToken {
            private val newToken = "newToken"

            @Nested
            inner class SuccessfulRequest {
                @BeforeEach
                fun setUp() {
                    coEvery {
                        pushService.putDeviceToken(any())
                    } returns Unit.asSuccess()
                }

                @Suppress("MaxLineLength")
                @Test
                fun `registerPushTokenIfNecessary should update registeredPushToken, lastPushTokenRegistrationDate and currentPushToken`() {
                    pushManager.registerPushTokenIfNecessary(newToken)

                    coVerify(exactly = 1) { pushService.putDeviceToken(PushTokenRequest(newToken)) }
                    assertEquals(
                        clock.instant().epochSecond,
                        pushDiskSource.getLastPushTokenRegistrationDate(userId)?.toEpochSecond(),
                    )
                    assertEquals(newToken, pushDiskSource.registeredPushToken)
                    assertEquals(newToken, pushDiskSource.getCurrentPushToken(userId))
                }

                @Suppress("MaxLineLength")
                @Test
                fun `registerStoredPushTokenIfNecessary should update registeredPushToken, lastPushTokenRegistrationDate and currentPushToken`() {
                    pushDiskSource.registeredPushToken = newToken
                    pushManager.registerStoredPushTokenIfNecessary()

                    coVerify(exactly = 1) { pushService.putDeviceToken(PushTokenRequest(newToken)) }
                    assertEquals(
                        clock.instant().epochSecond,
                        pushDiskSource.getLastPushTokenRegistrationDate(userId)?.toEpochSecond(),
                    )
                    assertEquals(newToken, pushDiskSource.registeredPushToken)
                    assertEquals(newToken, pushDiskSource.getCurrentPushToken(userId))
                }
            }

            @Nested
            inner class FailedRequest {
                @BeforeEach
                fun setUp() {
                    coEvery {
                        pushService.putDeviceToken(any())
                    } returns Throwable().asFailure()
                }

                @Test
                fun `registerPushTokenIfNecessary should update registeredPushToken`() {
                    pushManager.registerPushTokenIfNecessary(newToken)

                    coVerify(exactly = 1) { pushService.putDeviceToken(PushTokenRequest(newToken)) }
                    assertNull(pushDiskSource.getLastPushTokenRegistrationDate(userId))
                    assertEquals(newToken, pushDiskSource.registeredPushToken)
                    assertEquals(existingToken, pushDiskSource.getCurrentPushToken(userId))
                }

                @Test
                fun `registerStoredPushTokenIfNecessary should update registeredPushToken`() {
                    pushDiskSource.registeredPushToken = newToken
                    pushManager.registerStoredPushTokenIfNecessary()

                    coVerify(exactly = 1) { pushService.putDeviceToken(PushTokenRequest(newToken)) }
                    assertNull(pushDiskSource.getLastPushTokenRegistrationDate(userId))
                    assertEquals(newToken, pushDiskSource.registeredPushToken)
                    assertEquals(existingToken, pushDiskSource.getCurrentPushToken(userId))
                }
            }
        }
    }
}
