package com.x8bit.bitwarden.data.platform.manager.policy

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.policies.PolicyType
import com.bitwarden.policies.PolicyView
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.policy.model.UserNotificationPolicyData
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockPolicyView
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant

class UserNotificationPolicyManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val fakeSettingsDiskSource = FakeSettingsDiskSource()
    private val mutablePoliciesFlow = MutableStateFlow(emptyList<PolicyView>())
    private val userPolicies = mutableMapOf<String, List<PolicyView>>()
    private val policyManager: PolicyManager = mockk {
        every {
            getActivePolicies(type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
        } answers { mutablePoliciesFlow.value }
        every {
            getActivePoliciesFlow(type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
        } returns mutablePoliciesFlow
        every {
            getUserPolicies(userId = any(), type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
        } answers { userPolicies[firstArg<String>()].orEmpty() }
    }

    private val userNotificationPolicyManager: UserNotificationPolicyManager =
        UserNotificationPolicyManagerImpl(
            authDiskSource = fakeAuthDiskSource,
            settingsDiskSource = fakeSettingsDiskSource,
            policyManager = policyManager,
            dispatcherManager = FakeDispatcherManager(),
        )

    @Test
    fun `shouldClearOnSoftLogout should return true when the user has no policies`() {
        assertTrue(userNotificationPolicyManager.shouldClearOnSoftLogout(userId = USER_ID_1))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldClearOnSoftLogout should return true when the user policy shows after every login`() {
        setUserPolicies(
            userId = USER_ID_1,
            createPolicy(data = createPolicyJson(showAfterEveryLogin = true)),
        )

        assertTrue(userNotificationPolicyManager.shouldClearOnSoftLogout(userId = USER_ID_1))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldClearOnSoftLogout should return false when the user policy does not show after every login`() {
        setUserPolicies(
            userId = USER_ID_1,
            createPolicy(data = createPolicyJson(showAfterEveryLogin = false)),
        )

        assertFalse(userNotificationPolicyManager.shouldClearOnSoftLogout(userId = USER_ID_1))
    }

    @Test
    fun `shouldClearOnSoftLogout should return true when the user policy has no data`() {
        setUserPolicies(userId = USER_ID_1, createPolicy(data = null))

        assertTrue(userNotificationPolicyManager.shouldClearOnSoftLogout(userId = USER_ID_1))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldClearOnSoftLogout should use the first user policy that has data`() {
        setUserPolicies(
            userId = USER_ID_1,
            createPolicy(number = 1, data = null),
            createPolicy(number = 2, data = createPolicyJson(showAfterEveryLogin = false)),
        )

        assertFalse(userNotificationPolicyManager.shouldClearOnSoftLogout(userId = USER_ID_1))
    }

    @Suppress("MaxLineLength")
    @Test
    fun `shouldClearOnSoftLogout should use the policies of the given user instead of the active user`() {
        fakeAuthDiskSource.userState = MULTI_USER_STATE_ACTIVE_USER_2
        setUserPolicies(
            userId = USER_ID_1,
            createPolicy(data = createPolicyJson(showAfterEveryLogin = false)),
        )
        setUserPolicies(
            userId = USER_ID_2,
            createPolicy(data = createPolicyJson(showAfterEveryLogin = true)),
        )

        assertFalse(userNotificationPolicyManager.shouldClearOnSoftLogout(userId = USER_ID_1))
        assertTrue(userNotificationPolicyManager.shouldClearOnSoftLogout(userId = USER_ID_2))
        verify(exactly = 0) {
            policyManager.getActivePolicies(type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
        }
    }

    @Test
    fun `displayData should return null when there is no active user`() {
        setPolicies(createPolicy())

        assertNull(userNotificationPolicyManager.displayData)
        verify(exactly = 0) {
            policyManager.getActivePolicies(type = PolicyType.ORGANIZATION_USER_NOTIFICATION)
        }
    }

    @Test
    fun `displayData should return null when there are no active policies`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE

        assertNull(userNotificationPolicyManager.displayData)
    }

    @Test
    fun `displayData should return null when the active policy has no data`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        setPolicies(createPolicy(data = null))

        assertNull(userNotificationPolicyManager.displayData)
    }

    @Test
    fun `displayData should return the policy data when the banner has not been dismissed`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        setPolicies(createPolicy())

        assertEquals(EXPECTED_DISPLAY_DATA, userNotificationPolicyManager.displayData)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `displayData should return null when the banner has been dismissed for the policy revision date`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        fakeSettingsDiskSource.storeVaultPolicyBannerDismissedDate(
            userId = USER_ID_1,
            dismissalRevisionDate = REVISION_DATE,
        )
        setPolicies(createPolicy(revisionDate = REVISION_DATE))

        assertNull(userNotificationPolicyManager.displayData)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `displayData should return the policy data when the policy revision date differs from the dismissed date`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        fakeSettingsDiskSource.storeVaultPolicyBannerDismissedDate(
            userId = USER_ID_1,
            dismissalRevisionDate = REVISION_DATE,
        )
        setPolicies(createPolicy(revisionDate = OTHER_REVISION_DATE))

        assertEquals(EXPECTED_DISPLAY_DATA, userNotificationPolicyManager.displayData)
    }

    @Suppress("MaxLineLength")
    @Test
    fun `displayData should return the policy data when the policy has no revision date and the banner has been dismissed`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        fakeSettingsDiskSource.storeVaultPolicyBannerDismissedDate(
            userId = USER_ID_1,
            dismissalRevisionDate = REVISION_DATE,
        )
        setPolicies(createPolicy(revisionDate = null))

        assertEquals(EXPECTED_DISPLAY_DATA, userNotificationPolicyManager.displayData)
    }

    @Test
    fun `displayData should return the data of the first active policy`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        setPolicies(
            createPolicy(number = 1),
            createPolicy(number = 2, data = createPolicyJson(descriptionText = "otherDescription")),
        )

        assertEquals(EXPECTED_DISPLAY_DATA, userNotificationPolicyManager.displayData)
    }

    @Test
    fun `displayDataFlow should have an initial value matching displayData`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        setPolicies(createPolicy())

        assertEquals(EXPECTED_DISPLAY_DATA, userNotificationPolicyManager.displayDataFlow.value)
    }

    @Test
    fun `displayDataFlow should emit null when there is no active user`() = runTest {
        setPolicies(createPolicy())

        userNotificationPolicyManager.displayDataFlow.test {
            assertNull(awaitItem())
            fakeAuthDiskSource.userState = SINGLE_USER_STATE
            assertEquals(EXPECTED_DISPLAY_DATA, awaitItem())
        }
    }

    @Test
    fun `displayDataFlow should emit when the active policies change`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE

        userNotificationPolicyManager.displayDataFlow.test {
            assertNull(awaitItem())
            setPolicies(createPolicy())
            assertEquals(EXPECTED_DISPLAY_DATA, awaitItem())
            setPolicies()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `displayDataFlow should emit null when the banner is dismissed`() = runTest {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        setPolicies(createPolicy(revisionDate = REVISION_DATE))

        userNotificationPolicyManager.displayDataFlow.test {
            assertEquals(EXPECTED_DISPLAY_DATA, awaitItem())
            userNotificationPolicyManager.dismissBanner()
            assertNull(awaitItem())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `displayDataFlow should emit the policy data when the revision date changes after a dismissal`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE
            setPolicies(createPolicy(revisionDate = REVISION_DATE))

            userNotificationPolicyManager.displayDataFlow.test {
                assertEquals(EXPECTED_DISPLAY_DATA, awaitItem())
                userNotificationPolicyManager.dismissBanner()
                assertNull(awaitItem())
                setPolicies(createPolicy(revisionDate = OTHER_REVISION_DATE))
                assertEquals(EXPECTED_DISPLAY_DATA, awaitItem())
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `displayDataFlow should emit null when switching to a user that has dismissed the banner`() =
        runTest {
            fakeAuthDiskSource.userState = SINGLE_USER_STATE
            fakeSettingsDiskSource.storeVaultPolicyBannerDismissedDate(
                userId = USER_ID_2,
                dismissalRevisionDate = REVISION_DATE,
            )
            setPolicies(createPolicy(revisionDate = REVISION_DATE))

            userNotificationPolicyManager.displayDataFlow.test {
                assertEquals(EXPECTED_DISPLAY_DATA, awaitItem())
                fakeAuthDiskSource.userState = MULTI_USER_STATE_ACTIVE_USER_2
                assertNull(awaitItem())
            }
        }

    @Test
    fun `dismissBanner should do nothing when there are no active policies`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE

        userNotificationPolicyManager.dismissBanner()

        fakeSettingsDiskSource.assertVaultPolicyBannerDismissedDate(
            userId = USER_ID_1,
            expected = null,
        )
    }

    @Test
    fun `dismissBanner should do nothing when there is no active user`() {
        setPolicies(createPolicy(revisionDate = REVISION_DATE))

        userNotificationPolicyManager.dismissBanner()

        fakeSettingsDiskSource.assertVaultPolicyBannerDismissedDate(
            userId = USER_ID_1,
            expected = null,
        )
    }

    @Test
    fun `dismissBanner should store the revision date of the active policy for the active user`() {
        fakeAuthDiskSource.userState = SINGLE_USER_STATE
        setPolicies(createPolicy(revisionDate = REVISION_DATE))

        userNotificationPolicyManager.dismissBanner()

        fakeSettingsDiskSource.assertVaultPolicyBannerDismissedDate(
            userId = USER_ID_1,
            expected = REVISION_DATE,
        )
    }

    /**
     * Sets the active organization user notification [policies], each of which should be created
     * via [createPolicy].
     */
    private fun setPolicies(vararg policies: PolicyView) {
        mutablePoliciesFlow.value = policies.toList()
    }

    /**
     * Sets the organization user notification [policies] applicable to the given [userId], each of
     * which should be created via [createPolicy].
     */
    private fun setUserPolicies(userId: String, vararg policies: PolicyView) {
        userPolicies[userId] = policies.toList()
    }
}

/**
 * Creates an active organization user notification [PolicyView].
 */
private fun createPolicy(
    number: Int = 1,
    data: String? = createPolicyJson(),
    revisionDate: Instant? = REVISION_DATE,
): PolicyView = createMockPolicyView(
    number = number,
    type = PolicyType.ORGANIZATION_USER_NOTIFICATION,
    enabled = true,
    data = data,
    revisionDate = revisionDate,
)

/**
 * Builds the `data` payload of an organization user notification [PolicyView].
 */
private fun createPolicyJson(
    headerText: String? = "mockHeaderText",
    descriptionText: String = "mockDescriptionText",
    buttonText: String? = "mockButtonText",
    showAfterEveryLogin: Boolean = true,
): String =
    """
    {
      "header":${headerText?.let { "\"$it\"" }},
      "description":"$descriptionText",
      "buttonText":${buttonText?.let { "\"$it\"" }},
      "showAfterEveryLogin":$showAfterEveryLogin
    }
    """

private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val USER_ID_2 = "b9d32ec0-6497-4582-9798-b350f53bfa02"

private val REVISION_DATE: Instant = Instant.parse("2024-09-13T01:00:00.00Z")
private val OTHER_REVISION_DATE: Instant = Instant.parse("2025-01-27T12:00:00.00Z")

private val EXPECTED_DISPLAY_DATA = UserNotificationPolicyData(
    organizationId = "mockOrganizationId-1",
    headerText = "mockHeaderText",
    descriptionText = "mockDescriptionText",
    buttonText = "mockButtonText",
)

private fun createAccount(userId: String): AccountJson = AccountJson(
    profile = AccountJson.Profile(
        userId = userId,
        email = "test@bitwarden.com",
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
        userDecryptionOptions = null,
        isTwoFactorEnabled = false,
        creationDate = Instant.parse("2024-09-13T01:00:00.00Z"),
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)

private val SINGLE_USER_STATE = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(USER_ID_1 to createAccount(userId = USER_ID_1)),
)

private val MULTI_USER_STATE_ACTIVE_USER_2 = UserStateJson(
    activeUserId = USER_ID_2,
    accounts = mapOf(
        USER_ID_1 to createAccount(userId = USER_ID_1),
        USER_ID_2 to createAccount(userId = USER_ID_2),
    ),
)
