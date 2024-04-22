package com.x8bit.bitwarden.data.auth.repository.util

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountTokensJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserAccountTokens
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.UserSwitchingData
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthDiskSourceExtensionsTest {
    private val authDiskSource: AuthDiskSource = FakeAuthDiskSource()

    @Test
    fun `userAccountTokens should return data for all available users`() {
        val mockAccounts = mapOf(
            "userId1" to mockk<AccountJson>(),
            "userId2" to mockk<AccountJson>(),
            "userId3" to mockk<AccountJson>(),
        )
        val userStateJson = mockk<UserStateJson> {
            every { accounts } returns mockAccounts
        }
        authDiskSource.apply {
            userState = userStateJson
            storeAccountTokens(
                userId = "userId1",
                accountTokens = AccountTokensJson(
                    accessToken = "accessToken1",
                    refreshToken = "refreshToken1",
                ),
            )
            storeAccountTokens(
                userId = "userId2",
                accountTokens = AccountTokensJson(
                    accessToken = "accessToken2",
                    refreshToken = "refreshToken2",
                ),
            )
            storeAccountTokens(
                userId = "userId3",
                accountTokens = AccountTokensJson(
                    accessToken = null,
                    refreshToken = null,
                ),
            )
        }

        assertEquals(
            listOf(
                UserAccountTokens(
                    userId = "userId1",
                    accessToken = "accessToken1",
                    refreshToken = "refreshToken1",
                ),
                UserAccountTokens(
                    userId = "userId2",
                    accessToken = "accessToken2",
                    refreshToken = "refreshToken2",
                ),
                UserAccountTokens(
                    userId = "userId3",
                    accessToken = null,
                    refreshToken = null,
                ),
            ),
            authDiskSource.userAccountTokens,
        )
    }

    @Test
    fun `userAccountTokensFlow should emit whenever there are changes to the token data`() =
        runTest {
            val mockAccounts = mapOf(
                "userId1" to mockk<AccountJson>(),
                "userId2" to mockk<AccountJson>(),
                "userId3" to mockk<AccountJson>(),
            )
            val userStateJson = mockk<UserStateJson> {
                every { accounts } returns mockAccounts
            }
            authDiskSource.apply {
                userState = userStateJson
                storeAccountTokens(
                    userId = "userId1",
                    accountTokens = AccountTokensJson(
                        accessToken = "accessToken1",
                        refreshToken = "refreshToken1",
                    ),
                )
            }

            authDiskSource.userAccountTokensFlow.test {
                assertEquals(
                    listOf(
                        UserAccountTokens(
                            userId = "userId1",
                            accessToken = "accessToken1",
                            refreshToken = "refreshToken1",
                        ),
                        UserAccountTokens(
                            userId = "userId2",
                            accessToken = null,
                            refreshToken = null,
                        ),
                        UserAccountTokens(
                            userId = "userId3",
                            accessToken = null,
                            refreshToken = null,
                        ),
                    ),
                    awaitItem(),
                )

                authDiskSource.storeAccountTokens(
                    userId = "userId2",
                    accountTokens = AccountTokensJson(
                        accessToken = "accessToken2",
                        refreshToken = "refreshToken2",
                    ),
                )

                assertEquals(
                    listOf(
                        UserAccountTokens(
                            userId = "userId1",
                            accessToken = "accessToken1",
                            refreshToken = "refreshToken1",
                        ),
                        UserAccountTokens(
                            userId = "userId2",
                            accessToken = "accessToken2",
                            refreshToken = "refreshToken2",
                        ),
                        UserAccountTokens(
                            userId = "userId3",
                            accessToken = null,
                            refreshToken = null,
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `userOrganizationsList should return data for all available users`() {
        val mockAccounts = mapOf(
            "userId1" to mockk<AccountJson>(),
            "userId2" to mockk<AccountJson>(),
            "userId3" to mockk<AccountJson>(),
        )
        val userStateJson = mockk<UserStateJson> {
            every { accounts } returns mockAccounts
        }
        authDiskSource.apply {
            userState = userStateJson
            storeOrganizations(
                userId = "userId1",
                organizations = listOf(createMockOrganization(number = 1)),
            )
            storeOrganizations(
                userId = "userId2",
                organizations = listOf(createMockOrganization(number = 2)),
            )
            storeOrganizations(
                userId = "userId3",
                organizations = listOf(createMockOrganization(number = 3)),
            )
        }

        assertEquals(
            listOf(
                UserOrganizations(
                    userId = "userId1",
                    organizations = listOf(
                        Organization(
                            id = "mockId-1",
                            name = "mockName-1",
                        ),
                    ),
                ),
                UserOrganizations(
                    userId = "userId2",
                    organizations = listOf(
                        Organization(
                            id = "mockId-2",
                            name = "mockName-2",
                        ),
                    ),
                ),
                UserOrganizations(
                    userId = "userId3",
                    organizations = listOf(
                        Organization(
                            id = "mockId-3",
                            name = "mockName-3",
                        ),
                    ),
                ),
            ),
            authDiskSource.userOrganizationsList,
        )
    }

    @Test
    fun `userOrganizationsListFlow should emit whenever there are changes to organization data`() =
        runTest {
            val mockAccounts = mapOf(
                "userId1" to mockk<AccountJson>(),
                "userId2" to mockk<AccountJson>(),
                "userId3" to mockk<AccountJson>(),
            )
            val userStateJson = mockk<UserStateJson> {
                every { accounts } returns mockAccounts
            }
            authDiskSource.apply {
                userState = userStateJson
                storeOrganizations(
                    userId = "userId1",
                    organizations = listOf(createMockOrganization(number = 1)),
                )
            }

            authDiskSource.userOrganizationsListFlow.test {
                assertEquals(
                    listOf(
                        UserOrganizations(
                            userId = "userId1",
                            organizations = listOf(
                                Organization(
                                    id = "mockId-1",
                                    name = "mockName-1",
                                ),
                            ),
                        ),
                        UserOrganizations(
                            userId = "userId2",
                            organizations = emptyList(),
                        ),
                        UserOrganizations(
                            userId = "userId3",
                            organizations = emptyList(),
                        ),
                    ),
                    awaitItem(),
                )

                authDiskSource.storeOrganizations(
                    userId = "userId2",
                    organizations = listOf(createMockOrganization(number = 2)),
                )

                assertEquals(
                    listOf(
                        UserOrganizations(
                            userId = "userId1",
                            organizations = listOf(
                                Organization(
                                    id = "mockId-1",
                                    name = "mockName-1",
                                ),
                            ),
                        ),
                        UserOrganizations(
                            userId = "userId2",
                            organizations = listOf(
                                Organization(
                                    id = "mockId-2",
                                    name = "mockName-2",
                                ),
                            ),
                        ),
                        UserOrganizations(
                            userId = "userId3",
                            organizations = emptyList(),
                        ),
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `userSwitchingChangesFlow should emit changes when active user changes`() = runTest {
        authDiskSource.userSwitchingChangesFlow.test {
            assertEquals(
                UserSwitchingData(
                    previousActiveUserId = null,
                    currentActiveUserId = null,
                ),
                awaitItem(),
            )
            authDiskSource.userState = MOCK_USER_STATE
            assertEquals(
                UserSwitchingData(
                    previousActiveUserId = null,
                    currentActiveUserId = MOCK_USER_ID,
                ),
                awaitItem(),
            )
            authDiskSource.userState = MOCK_USER_STATE.copy(
                accounts = mapOf(
                    MOCK_USER_ID to MOCK_ACCOUNT,
                    "mockId-2" to mockk(),
                ),
            )
            expectNoEvents()
            authDiskSource.userState = null
            assertEquals(
                UserSwitchingData(
                    previousActiveUserId = MOCK_USER_ID,
                    currentActiveUserId = null,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `activeUserIdChangesFlow should emit changes when active user changes`() = runTest {
        authDiskSource.activeUserIdChangesFlow.test {
            assertNull(awaitItem())
            authDiskSource.userState = MOCK_USER_STATE
            assertEquals(MOCK_USER_ID, awaitItem())
            authDiskSource.userState = MOCK_USER_STATE.copy(
                accounts = mapOf(
                    MOCK_USER_ID to MOCK_ACCOUNT,
                    "mockId-2" to mockk(),
                ),
            )
            expectNoEvents()
            authDiskSource.userState = null
            assertNull(awaitItem())
        }
    }
}

private const val MOCK_USER_ID: String = "mockId-1"

private val MOCK_PROFILE = AccountJson.Profile(
    userId = MOCK_USER_ID,
    email = "email",
    isEmailVerified = true,
    name = null,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    hasPremium = false,
    forcePasswordResetReason = null,
    kdfType = null,
    kdfIterations = null,
    kdfMemory = null,
    kdfParallelism = null,
    userDecryptionOptions = null,
)

private val MOCK_ACCOUNT = AccountJson(
    profile = MOCK_PROFILE,
    tokens = AccountTokensJson(
        accessToken = "accessToken",
        refreshToken = "refreshToken",
    ),
    settings = AccountJson.Settings(
        environmentUrlData = null,
    ),
)

private val MOCK_USER_STATE = UserStateJson(
    activeUserId = MOCK_USER_ID,
    accounts = mapOf(MOCK_USER_ID to MOCK_ACCOUNT),
)
