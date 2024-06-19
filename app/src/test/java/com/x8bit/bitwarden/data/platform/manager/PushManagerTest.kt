package com.x8bit.bitwarden.data.platform.manager

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.base.FakeSharedPreferences
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSourceImpl
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.NotificationLogoutData
import com.x8bit.bitwarden.data.platform.manager.model.PasswordlessRequestData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncCipherUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncFolderUpsertData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendDeleteData
import com.x8bit.bitwarden.data.platform.manager.model.SyncSendUpsertData
import com.x8bit.bitwarden.data.platform.util.asFailure
import com.x8bit.bitwarden.data.platform.util.asSuccess
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
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

class PushManagerTest {
    private val clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
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
            json = PlatformNetworkModule.providesJson(),
        )
    }

    @Nested
    inner class PushNotificationHandling {
        private val userId = "any user ID"

        @BeforeEach
        fun setup() {
            val accountTokens = AccountTokensJson(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            )
            authDiskSource.storeAccountTokens(userId, accountTokens)
            authDiskSource.userState = UserStateJson(userId, mapOf(userId to mockk<AccountJson>()))
        }

        @Test
        fun `onMessageReceived with invalid JSON does not crash`() {
            pushManager.onMessageReceived(INVALID_NOTIFICATION_MAP)
        }

        @Test
        fun `onMessageReceived with auth request emits to passwordlessRequestFlow`() = runTest {
            pushManager.passwordlessRequestFlow.test {
                pushManager.onMessageReceived(AUTH_REQUEST_NOTIFICATION_MAP)
                assertEquals(
                    PasswordlessRequestData(
                        loginRequestId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                        userId = "078966a2-93c2-4618-ae2a-0a2394c88d37",
                    ),
                    awaitItem(),
                )
            }
        }

        @Test
        fun `onMessageReceived with auth request response emits to passwordlessRequestFlow`() =
            runTest {
                pushManager.passwordlessRequestFlow.test {
                    pushManager.onMessageReceived(AUTH_REQUEST_RESPONSE_NOTIFICATION_MAP)
                    assertEquals(
                        PasswordlessRequestData(
                            loginRequestId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            userId = "078966a2-93c2-4618-ae2a-0a2394c88d37",
                        ),
                        awaitItem(),
                    )
                }
            }

        @Test
        fun `onMessageReceived with logout should emit to logoutFlow`() = runTest {
            val accountTokens = AccountTokensJson(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            )
            authDiskSource.storeAccountTokens(userId, accountTokens)
            authDiskSource.userState = UserStateJson(userId, mapOf(userId to mockk<AccountJson>()))

            pushManager.logoutFlow.test {
                pushManager.onMessageReceived(LOGOUT_NOTIFICATION_MAP)
                assertEquals(
                    NotificationLogoutData(userId = "078966a2-93c2-4618-ae2a-0a2394c88d37"),
                    awaitItem(),
                )
            }
        }

        @Nested
        inner class LoggedOutUserState {
            @BeforeEach
            fun setUp() {
                val userId = "any user ID"
                authDiskSource.storeAccountTokens(userId, null)
                authDiskSource.userState =
                    UserStateJson(userId, mapOf(userId to mockk<AccountJson>()))
            }

            @Test
            fun `onMessageReceived with logout emits to logoutFlow`() = runTest {
                pushManager.logoutFlow.test {
                    pushManager.onMessageReceived(LOGOUT_NOTIFICATION_MAP)
                    assertEquals(
                        NotificationLogoutData(userId = "078966a2-93c2-4618-ae2a-0a2394c88d37"),
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with ciphers emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHERS_NOTIFICATION_MAP)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with sync org keys does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_ORG_KEYS_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync settings emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_SETTINGS_NOTIFICATION_MAP)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with vault emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_VAULT_NOTIFICATION_MAP)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }
        }

        @Nested
        inner class MatchingLoggedInUser {
            @BeforeEach
            fun setUp() {
                val userId = "078966a2-93c2-4618-ae2a-0a2394c88d37"
                val accountTokens = AccountTokensJson(
                    accessToken = "accessToken",
                    refreshToken = "refreshToken",
                )
                authDiskSource.storeAccountTokens(userId, accountTokens)
                val account = mockk<AccountJson>()
                authDiskSource.userState = UserStateJson(userId, mapOf(userId to account))
            }

            @Test
            fun `onMessageReceived with sync cipher create emits to syncCipherUpsertFlow`() =
                runTest {
                    pushManager.syncCipherUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_CIPHER_CREATE_NOTIFICATION_MAP)
                        assertEquals(
                            SyncCipherUpsertData(
                                cipherId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                                organizationId = "6a41d965-ed95-4eae-98c3-5f1ec609c2c1",
                                collectionIds = listOf(),
                                revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
                                isUpdate = false,
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived with sync cipher delete emits to syncCipherDeleteFlow`() =
                runTest {
                    pushManager.syncCipherDeleteFlow.test {
                        pushManager.onMessageReceived(SYNC_CIPHER_DELETE_NOTIFICATION_MAP)
                        assertEquals(
                            SyncCipherDeleteData(
                                cipherId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived with sync cipher update emits to syncCipherUpsertFlow`() =
                runTest {
                    pushManager.syncCipherUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_CIPHER_UPDATE_NOTIFICATION_MAP)
                        assertEquals(
                            SyncCipherUpsertData(
                                cipherId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                                organizationId = "6a41d965-ed95-4eae-98c3-5f1ec609c2c1",
                                collectionIds = listOf(),
                                revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
                                isUpdate = true,
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived with sync folder create emits to syncFolderUpsertFlow`() =
                runTest {
                    pushManager.syncFolderUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_FOLDER_CREATE_NOTIFICATION_MAP)
                        assertEquals(
                            SyncFolderUpsertData(
                                folderId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                                revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
                                isUpdate = false,
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived with sync folder delete emits to syncFolderDeleteFlow`() =
                runTest {
                    pushManager.syncFolderDeleteFlow.test {
                        pushManager.onMessageReceived(SYNC_FOLDER_DELETE_NOTIFICATION_MAP)
                        assertEquals(
                            SyncFolderDeleteData(
                                folderId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived with sync folder update emits to syncFolderUpsertFlow`() =
                runTest {
                    pushManager.syncFolderUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_FOLDER_UPDATE_NOTIFICATION_MAP)
                        assertEquals(
                            SyncFolderUpsertData(
                                folderId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                                revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
                                isUpdate = true,
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived with sync login delete emits to syncCipherDeleteFlow`() =
                runTest {
                    pushManager.syncCipherDeleteFlow.test {
                        pushManager.onMessageReceived(SYNC_LOGIN_DELETE_NOTIFICATION_MAP)
                        assertEquals(
                            SyncCipherDeleteData(
                                cipherId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived with sync send create emits to syncSendUpsertFlow`() = runTest {
                pushManager.syncSendUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_CREATE_NOTIFICATION_MAP)
                    assertEquals(
                        SyncSendUpsertData(
                            sendId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
                            isUpdate = false,
                        ),
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with sync send delete emits to syncSendDeleteFlow`() = runTest {
                pushManager.syncSendDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_DELETE_NOTIFICATION_MAP)
                    assertEquals(
                        SyncSendDeleteData(
                            sendId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                        ),
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with sync send update emits to syncSendUpsertFlow`() = runTest {
                pushManager.syncSendUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_UPDATE_NOTIFICATION_MAP)
                    assertEquals(
                        SyncSendUpsertData(
                            sendId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            revisionDate = ZonedDateTime.parse("2023-10-27T12:00:00.000Z"),
                            isUpdate = true,
                        ),
                        awaitItem(),
                    )
                }
            }
        }

        @Nested
        inner class NonMatchingLoggedInUser {
            @BeforeEach
            fun setUp() {
                val userId = "bad user ID"
                val accountTokens = AccountTokensJson(
                    accessToken = "accessToken",
                    refreshToken = "refreshToken",
                )
                authDiskSource.storeAccountTokens(userId, accountTokens)
                val account = mockk<AccountJson>()
                authDiskSource.userState = UserStateJson(userId, mapOf(userId to account))
            }

            @Test
            fun `onMessageReceived with sync cipher create does nothing`() = runTest {
                pushManager.syncCipherUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHER_CREATE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync cipher delete does nothing`() = runTest {
                pushManager.syncCipherDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHER_DELETE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync cipher update does nothing`() = runTest {
                pushManager.syncCipherUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHER_UPDATE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync folder create does nothing`() = runTest {
                pushManager.syncFolderUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_FOLDER_CREATE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync folder delete does nothing`() = runTest {
                pushManager.syncFolderDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_FOLDER_DELETE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync folder update does nothing`() = runTest {
                pushManager.syncFolderDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_FOLDER_UPDATE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync login delete does nothing`() = runTest {
                pushManager.syncCipherDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_LOGIN_DELETE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync send create does nothing`() = runTest {
                pushManager.syncSendUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_CREATE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync send delete does nothing`() = runTest {
                pushManager.syncSendDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_DELETE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync send update does nothing`() = runTest {
                pushManager.syncSendUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_UPDATE_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }
        }

        @Nested
        inner class NullUserState {
            @BeforeEach
            fun setUp() {
                authDiskSource.userState = null
            }

            @Test
            fun `onMessageReceived with logout does nothing`() = runTest {
                pushManager.logoutFlow.test {
                    pushManager.onMessageReceived(LOGOUT_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync ciphers does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHERS_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync org keys does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_ORG_KEYS_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync settings does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_SETTINGS_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived with sync vault does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_VAULT_NOTIFICATION_MAP)
                    expectNoEvents()
                }
            }
        }

        @Nested
        inner class NonNullUserState {
            @BeforeEach
            fun setUp() {
                val userId = "any user ID"
                val accountTokens = AccountTokensJson(
                    accessToken = "accessToken",
                    refreshToken = "refreshToken",
                )
                authDiskSource.storeAccountTokens(userId, accountTokens)
                val account = mockk<AccountJson>()
                authDiskSource.userState = UserStateJson(userId, mapOf(userId to account))
            }

            @Test
            fun `onMessageReceived with logout emits to logoutFlow`() = runTest {
                pushManager.logoutFlow.test {
                    pushManager.onMessageReceived(LOGOUT_NOTIFICATION_MAP)
                    assertEquals(
                        NotificationLogoutData(userId = "078966a2-93c2-4618-ae2a-0a2394c88d37"),
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with sync ciphers emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHERS_NOTIFICATION_MAP)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with sync org keys emits to syncOrgKeysFlow`() = runTest {
                pushManager.syncOrgKeysFlow.test {
                    pushManager.onMessageReceived(SYNC_ORG_KEYS_NOTIFICATION_MAP)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with sync settings emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_SETTINGS_NOTIFICATION_MAP)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived with sync vault emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_VAULT_NOTIFICATION_MAP)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }
        }
    }

    @Nested
    inner class PushNotificationRegistration {
        @Nested
        inner class LoggedOutUserState {
            @BeforeEach
            fun setUp() {
                val userId = "any user ID"
                authDiskSource.storeAccountTokens(userId = userId, accountTokens = null)
                val account = mockk<AccountJson>()
                authDiskSource.userState = UserStateJson(userId, mapOf(userId to account))
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
        inner class NonNullLoggedInUserState {
            private val existingToken = "existingToken"
            private val userId = "userId"

            @BeforeEach
            fun setUp() {
                pushDiskSource.storeCurrentPushToken(userId, existingToken)
                val accountTokens = AccountTokensJson(
                    accessToken = "accessToken",
                    refreshToken = "refreshToken",
                )
                authDiskSource.storeAccountTokens(userId, accountTokens)
                val account = mockk<AccountJson>()
                authDiskSource.userState = UserStateJson(userId, mapOf(userId to account))
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

                        coVerify(exactly = 1) {
                            pushService.putDeviceToken(PushTokenRequest(newToken))
                        }
                        assertEquals(
                            clock.instant().epochSecond,
                            pushDiskSource
                                .getLastPushTokenRegistrationDate(userId)
                                ?.toEpochSecond(),
                        )
                        assertEquals(newToken, pushDiskSource.registeredPushToken)
                        assertEquals(newToken, pushDiskSource.getCurrentPushToken(userId))
                    }

                    @Suppress("MaxLineLength")
                    @Test
                    fun `registerStoredPushTokenIfNecessary should update registeredPushToken, lastPushTokenRegistrationDate and currentPushToken`() {
                        pushDiskSource.registeredPushToken = newToken
                        pushManager.registerStoredPushTokenIfNecessary()

                        coVerify(exactly = 1) {
                            pushService.putDeviceToken(PushTokenRequest(newToken))
                        }
                        assertEquals(
                            clock.instant().epochSecond,
                            pushDiskSource
                                .getLastPushTokenRegistrationDate(userId)
                                ?.toEpochSecond(),
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

                        coVerify(exactly = 1) {
                            pushService.putDeviceToken(PushTokenRequest(newToken))
                        }
                        assertNull(pushDiskSource.getLastPushTokenRegistrationDate(userId))
                        assertEquals(newToken, pushDiskSource.registeredPushToken)
                        assertEquals(existingToken, pushDiskSource.getCurrentPushToken(userId))
                    }

                    @Test
                    fun `registerStoredPushTokenIfNecessary should update registeredPushToken`() {
                        pushDiskSource.registeredPushToken = newToken
                        pushManager.registerStoredPushTokenIfNecessary()

                        coVerify(exactly = 1) {
                            pushService.putDeviceToken(PushTokenRequest(newToken))
                        }
                        assertNull(pushDiskSource.getLastPushTokenRegistrationDate(userId))
                        assertEquals(newToken, pushDiskSource.registeredPushToken)
                        assertEquals(existingToken, pushDiskSource.getCurrentPushToken(userId))
                    }
                }
            }
        }
    }
}

private val AUTH_REQUEST_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "15",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37"
    }""",
)

private val AUTH_REQUEST_RESPONSE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "16",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37"
    }""",
)

private val INVALID_NOTIFICATION_MAP = emptyMap<String, String>()

private val LOGOUT_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "11",
    "payload" to """{
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "Date": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_CIPHER_CREATE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "1",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "OrganizationId": "6a41d965-ed95-4eae-98c3-5f1ec609c2c1",
      "CollectionIds": [],
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_CIPHER_DELETE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "9",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "OrganizationId": "6a41d965-ed95-4eae-98c3-5f1ec609c2c1",
      "CollectionIds": [],
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_CIPHER_UPDATE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "0",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "OrganizationId": "6a41d965-ed95-4eae-98c3-5f1ec609c2c1",
      "CollectionIds": [],
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_CIPHERS_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "4",
    "payload" to """{
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_FOLDER_CREATE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "7",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_FOLDER_DELETE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "3",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_FOLDER_UPDATE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "8",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_LOGIN_DELETE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "2",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "OrganizationId": "6a41d965-ed95-4eae-98c3-5f1ec609c2c1",
      "CollectionIds": [],
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_ORG_KEYS_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "6",
    "payload" to """{
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "Date": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_SEND_CREATE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "12",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_SEND_DELETE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "14",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_SEND_UPDATE_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "13",
    "payload" to """{
      "Id": "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "RevisionDate": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_SETTINGS_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "10",
    "payload" to """{
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "Date": "2023-10-27T12:00:00.000Z"
    }""",
)

private val SYNC_VAULT_NOTIFICATION_MAP = mapOf(
    "contextId" to "801f459d-8e51-47d0-b072-3f18c9f66f64",
    "type" to "5",
    "payload" to """{
      "UserId": "078966a2-93c2-4618-ae2a-0a2394c88d37",
      "Date": "2023-10-27T12:00:00.000Z"
    }""",
)
