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
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
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

    private val settingsDiskSource: SettingsDiskSource = FakeSettingsDiskSource()

    private val pushDiskSource: PushDiskSource = PushDiskSourceImpl(FakeSharedPreferences())

    private val pushService: PushService = mockk()

    private lateinit var pushManager: PushManager

    @BeforeEach
    fun setUp() {
        pushManager = PushManagerImpl(
            authDiskSource = authDiskSource,
            settingsDiskSource = settingsDiskSource,
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
        fun `onMessageReceived invalid JSON does not crash`() {
            pushManager.onMessageReceived(INVALID_NOTIFICATION_JSON)
        }

        @Suppress("MaxLineLength")
        @Test
        fun `onMessageReceived auth request emits to nothing when getApprovePasswordlessLoginsEnabled is not true`() =
            runTest {
                settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
                    userId = userId,
                    isApprovePasswordlessLoginsEnabled = false,
                )
                pushManager.passwordlessRequestFlow.test {
                    pushManager.onMessageReceived(AUTH_REQUEST_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `onMessageReceived auth request emits to passwordlessRequestFlow when getApprovePasswordlessLoginsEnabled is true`() =
            runTest {
                settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
                    userId = userId,
                    isApprovePasswordlessLoginsEnabled = true,
                )
                pushManager.passwordlessRequestFlow.test {
                    pushManager.onMessageReceived(AUTH_REQUEST_NOTIFICATION_JSON)
                    assertEquals(
                        PasswordlessRequestData(
                            loginRequestId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            userId = "078966a2-93c2-4618-ae2a-0a2394c88d37",
                        ),
                        awaitItem(),
                    )
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `onMessageReceived auth request response emits nothing when getApprovePasswordlessLoginsEnabled is not true`() =
            runTest {
                settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
                    userId = userId,
                    isApprovePasswordlessLoginsEnabled = false,
                )
                pushManager.passwordlessRequestFlow.test {
                    pushManager.onMessageReceived(AUTH_REQUEST_RESPONSE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

        @Suppress("MaxLineLength")
        @Test
        fun `onMessageReceived auth request response emits to passwordlessRequestFlow when getApprovePasswordlessLoginsEnabled is true`() =
            runTest {
                settingsDiskSource.storeApprovePasswordlessLoginsEnabled(
                    userId = userId,
                    isApprovePasswordlessLoginsEnabled = true,
                )
                pushManager.passwordlessRequestFlow.test {
                    pushManager.onMessageReceived(AUTH_REQUEST_RESPONSE_NOTIFICATION_JSON)
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
        fun `onMessageReceived logout should emit to logoutFlow`() = runTest {
            val accountTokens = AccountTokensJson(
                accessToken = "accessToken",
                refreshToken = "refreshToken",
            )
            authDiskSource.storeAccountTokens(userId, accountTokens)
            authDiskSource.userState = UserStateJson(userId, mapOf(userId to mockk<AccountJson>()))

            pushManager.logoutFlow.test {
                pushManager.onMessageReceived(LOGOUT_NOTIFICATION_JSON)
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
            fun `onMessageReceived logout emits to logoutFlow`() = runTest {
                pushManager.logoutFlow.test {
                    pushManager.onMessageReceived(LOGOUT_NOTIFICATION_JSON)
                    assertEquals(
                        NotificationLogoutData(userId = "078966a2-93c2-4618-ae2a-0a2394c88d37"),
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived sync ciphers emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHERS_NOTIFICATION_JSON)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived sync org keys does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_ORG_KEYS_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync settings emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_SETTINGS_NOTIFICATION_JSON)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived sync vault emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_VAULT_NOTIFICATION_JSON)
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
            fun `onMessageReceived sync cipher create emits to syncCipherUpsertFlow`() =
                runTest {
                    pushManager.syncCipherUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_CIPHER_CREATE_NOTIFICATION_JSON)
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
            fun `onMessageReceived sync cipher delete emits to syncCipherDeleteFlow`() =
                runTest {
                    pushManager.syncCipherDeleteFlow.test {
                        pushManager.onMessageReceived(SYNC_CIPHER_DELETE_NOTIFICATION_JSON)
                        assertEquals(
                            SyncCipherDeleteData(
                                cipherId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived sync cipher update emits to syncCipherUpsertFlow`() =
                runTest {
                    pushManager.syncCipherUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_CIPHER_UPDATE_NOTIFICATION_JSON)
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
            fun `onMessageReceived sync folder create emits to syncFolderUpsertFlow`() =
                runTest {
                    pushManager.syncFolderUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_FOLDER_CREATE_NOTIFICATION_JSON)
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
            fun `onMessageReceived sync folder delete emits to syncFolderDeleteFlow`() =
                runTest {
                    pushManager.syncFolderDeleteFlow.test {
                        pushManager.onMessageReceived(SYNC_FOLDER_DELETE_NOTIFICATION_JSON)
                        assertEquals(
                            SyncFolderDeleteData(
                                folderId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived sync folder update emits to syncFolderUpsertFlow`() =
                runTest {
                    pushManager.syncFolderUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_FOLDER_UPDATE_NOTIFICATION_JSON)
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
            fun `onMessageReceived sync login delete emits to syncCipherDeleteFlow`() =
                runTest {
                    pushManager.syncCipherDeleteFlow.test {
                        pushManager.onMessageReceived(SYNC_LOGIN_DELETE_NOTIFICATION_JSON)
                        assertEquals(
                            SyncCipherDeleteData(
                                cipherId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived sync send create emits to syncSendUpsertFlow`() =
                runTest {
                    pushManager.syncSendUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_SEND_CREATE_NOTIFICATION_JSON)
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
            fun `onMessageReceived sync send delete emits to syncSendDeleteFlow`() =
                runTest {
                    pushManager.syncSendDeleteFlow.test {
                        pushManager.onMessageReceived(SYNC_SEND_DELETE_NOTIFICATION_JSON)
                        assertEquals(
                            SyncSendDeleteData(
                                sendId = "aab5cdcc-f4a7-4e65-bf6d-5e0eab052321",
                            ),
                            awaitItem(),
                        )
                    }
                }

            @Test
            fun `onMessageReceived sync send update emits to syncSendUpsertFlow`() =
                runTest {
                    pushManager.syncSendUpsertFlow.test {
                        pushManager.onMessageReceived(SYNC_SEND_UPDATE_NOTIFICATION_JSON)
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
            fun `onMessageReceived sync cipher create does nothing`() = runTest {
                pushManager.syncCipherUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHER_CREATE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync cipher delete does nothing`() = runTest {
                pushManager.syncCipherDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHER_DELETE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync cipher update does nothing`() = runTest {
                pushManager.syncCipherUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHER_UPDATE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync folder create does nothing`() = runTest {
                pushManager.syncFolderUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_FOLDER_CREATE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync folder delete does nothing`() = runTest {
                pushManager.syncFolderDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_FOLDER_DELETE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync folder update does nothing`() = runTest {
                pushManager.syncFolderDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_FOLDER_UPDATE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync login delete does nothing`() = runTest {
                pushManager.syncCipherDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_LOGIN_DELETE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync send create does nothing`() = runTest {
                pushManager.syncSendUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_CREATE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync send delete does nothing`() = runTest {
                pushManager.syncSendDeleteFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_DELETE_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync send update does nothing`() = runTest {
                pushManager.syncSendUpsertFlow.test {
                    pushManager.onMessageReceived(SYNC_SEND_UPDATE_NOTIFICATION_JSON)
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
            fun `onMessageReceived logout does nothing`() = runTest {
                pushManager.logoutFlow.test {
                    pushManager.onMessageReceived(LOGOUT_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync ciphers does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHERS_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync org keys does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_ORG_KEYS_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync settings does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_SETTINGS_NOTIFICATION_JSON)
                    expectNoEvents()
                }
            }

            @Test
            fun `onMessageReceived sync vault does nothing`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_VAULT_NOTIFICATION_JSON)
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
            fun `onMessageReceived logout emits to logoutFlow`() = runTest {
                pushManager.logoutFlow.test {
                    pushManager.onMessageReceived(LOGOUT_NOTIFICATION_JSON)
                    assertEquals(
                        NotificationLogoutData(userId = "078966a2-93c2-4618-ae2a-0a2394c88d37"),
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived sync ciphers emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_CIPHERS_NOTIFICATION_JSON)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived sync org keys emits to syncOrgKeysFlow`() = runTest {
                pushManager.syncOrgKeysFlow.test {
                    pushManager.onMessageReceived(SYNC_ORG_KEYS_NOTIFICATION_JSON)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived sync settings emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_SETTINGS_NOTIFICATION_JSON)
                    assertEquals(
                        Unit,
                        awaitItem(),
                    )
                }
            }

            @Test
            fun `onMessageReceived sync vault emits to fullSyncFlow`() = runTest {
                pushManager.fullSyncFlow.test {
                    pushManager.onMessageReceived(SYNC_VAULT_NOTIFICATION_JSON)
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

private const val AUTH_REQUEST_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 15,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\"
      }"
    }
"""

private const val AUTH_REQUEST_RESPONSE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 16,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\"
      }"
    }
"""

private const val INVALID_NOTIFICATION_JSON = """
    {}
"""

private const val LOGOUT_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 11,
      "payload": "{
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"Date\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_CIPHER_CREATE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 1,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"OrganizationId\": \"6a41d965-ed95-4eae-98c3-5f1ec609c2c1\",
        \"CollectionIds\": [],
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_CIPHER_DELETE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 9,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"OrganizationId\": \"6a41d965-ed95-4eae-98c3-5f1ec609c2c1\",
        \"CollectionIds\": [],
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_CIPHER_UPDATE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 0,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"OrganizationId\": \"6a41d965-ed95-4eae-98c3-5f1ec609c2c1\",
        \"CollectionIds\": [],
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_CIPHERS_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 4,
      "payload": "{
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"Date\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_FOLDER_CREATE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 7,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_FOLDER_DELETE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 3,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_FOLDER_UPDATE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 8,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_LOGIN_DELETE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 2,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"OrganizationId\": \"6a41d965-ed95-4eae-98c3-5f1ec609c2c1\",
        \"CollectionIds\": [],
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_ORG_KEYS_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 6,
      "payload": "{
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"Date\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_SEND_CREATE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 12,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_SEND_DELETE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 14,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_SEND_UPDATE_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 13,
      "payload": "{
        \"Id\": \"aab5cdcc-f4a7-4e65-bf6d-5e0eab052321\",
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"RevisionDate\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_SETTINGS_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 10,
      "payload": "{
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"Date\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""

private const val SYNC_VAULT_NOTIFICATION_JSON = """
    {
      "contextId": "801f459d-8e51-47d0-b072-3f18c9f66f64",
      "type": 5,
      "payload": "{
        \"UserId\": \"078966a2-93c2-4618-ae2a-0a2394c88d37\",
        \"Date\": \"2023-10-27T12:00:00.000Z\"
      }"
    }
"""
