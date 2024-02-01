package com.x8bit.bitwarden.data.auth.repository.util

import app.cash.turbine.test
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.vault.datasource.network.model.createMockOrganization
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class AuthDiskSourceExtensionsTest {
    private val authDiskSource: AuthDiskSource = FakeAuthDiskSource()

    @Test
    fun `userOrganizationsList should return data for all available users`() {
        val mockAccounts = mapOf(
            "userId1" to mockk<AccountJson>(),
            "userId2" to mockk<AccountJson>(),
            "userId3" to mockk<AccountJson>(),
        )
        val userStateJson = mockk<UserStateJson>() {
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
            val userStateJson = mockk<UserStateJson>() {
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
}
