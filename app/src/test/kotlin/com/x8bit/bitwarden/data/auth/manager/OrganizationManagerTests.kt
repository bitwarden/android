package com.x8bit.bitwarden.data.auth.manager

import com.bitwarden.core.data.util.asFailure
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.KdfJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.MasterPasswordUnlockDataJson
import com.bitwarden.network.model.UserDecryptionOptionsJson
import com.bitwarden.network.model.createMockOrganizationNetwork
import com.bitwarden.network.service.OrganizationService
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.LeaveOrganizationResult
import com.x8bit.bitwarden.data.auth.repository.model.Organization
import com.x8bit.bitwarden.data.auth.repository.model.RevokeFromOrganizationResult
import com.x8bit.bitwarden.data.auth.repository.model.createMockOrganization
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class OrganizationManagerTests {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val organizationService: OrganizationService = mockk()

    private val organizationManager: OrganizationManager = OrganizationManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        organizationService = organizationService,
    )

    @Test
    fun `organizations should return an empty list when there is no active user`() = runTest {
        assertEquals(emptyList<Organization>(), organizationManager.organizations)
    }

    @Test
    fun `organizations should pull from the organizations in the AuthDiskSource`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
        fakeAuthDiskSource.storeOrganizations(
            userId = USER_ID_1,
            organizations = listOf(createMockOrganizationNetwork(number = 0)),
        )
        assertEquals(listOf(createMockOrganization(number = 0)), organizationManager.organizations)
    }

    @Test
    @Suppress("MaxLineLength")
    fun `leaveOrganization should return success when organizationService leaveOrganization succeeds`() =
        runTest {
            coEvery {
                organizationService.leaveOrganization(organizationId = any())
            } returns Unit.asSuccess()

            val continueResult = organizationManager.leaveOrganization(organizationId = "mockId-1")

            coVerify(exactly = 1) {
                organizationService.leaveOrganization(organizationId = any())
            }
            assertEquals(LeaveOrganizationResult.Success, continueResult)
        }

    @Test
    fun `leaveOrganization should return error when organizationService leaveOrganization fails`() =
        runTest {
            val error = Throwable("Fail")
            coEvery {
                organizationService.leaveOrganization(organizationId = any())
            } returns error.asFailure()

            val continueResult = organizationManager.leaveOrganization(organizationId = "mockId-1")

            coVerify(exactly = 1) {
                organizationService.leaveOrganization(organizationId = any())
            }
            assertEquals(LeaveOrganizationResult.Error(error = error), continueResult)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `revokeFromOrganization should return success when organizationService revokeFromOrganization succeeds`() =
        runTest {
            coEvery {
                organizationService.revokeFromOrganization(organizationId = any())
            } returns Unit.asSuccess()

            val result = organizationManager.revokeFromOrganization(organizationId = "mockId-1")

            coVerify(exactly = 1) {
                organizationService.revokeFromOrganization(organizationId = any())
            }
            assertEquals(RevokeFromOrganizationResult.Success, result)
        }

    @Test
    @Suppress("MaxLineLength")
    fun `revokeFromOrganization should return error when organizationService revokeFromOrganization fails`() =
        runTest {
            val error = Throwable("Fail")
            coEvery {
                organizationService.revokeFromOrganization(organizationId = any())
            } returns error.asFailure()

            val result = organizationManager.revokeFromOrganization(organizationId = "mockId-1")

            coVerify(exactly = 1) {
                organizationService.revokeFromOrganization(organizationId = any())
            }
            assertEquals(RevokeFromOrganizationResult.Error(error = error), result)
        }
}

private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val EMAIL = "test@bitwarden.com"

private val PROFILE_1 = AccountJson.Profile(
    userId = USER_ID_1,
    email = EMAIL,
    isEmailVerified = true,
    name = "Bitwarden Tester",
    hasPremiumPersonally = false,
    hasPremiumFromOrganization = null,
    stamp = null,
    organizationId = null,
    avatarColorHex = null,
    forcePasswordResetReason = null,
    kdfType = KdfTypeJson.ARGON2_ID,
    kdfIterations = 600000,
    kdfMemory = 16,
    kdfParallelism = 4,
    isTwoFactorEnabled = false,
    creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
    userDecryptionOptions = UserDecryptionOptionsJson(
        hasMasterPassword = true,
        trustedDeviceUserDecryptionOptions = null,
        keyConnectorUserDecryptionOptions = null,
        masterPasswordUnlock = MasterPasswordUnlockDataJson(
            kdf = KdfJson(
                kdfType = KdfTypeJson.ARGON2_ID,
                iterations = 600000,
                memory = 16,
                parallelism = 4,
            ),
            masterKeyWrappedUserKey = "encryptedUserKey",
            salt = EMAIL,
        ),
    ),
)
private val ACCOUNT_1 = AccountJson(
    profile = PROFILE_1,
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)
private val SINGLE_USER_STATE_1 = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(USER_ID_1 to ACCOUNT_1),
)
