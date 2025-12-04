package com.x8bit.bitwarden.data.auth.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.network.model.GetTokenResponseJson
import com.bitwarden.network.model.KdfTypeJson
import com.bitwarden.network.model.PolicyTypeJson
import com.bitwarden.network.model.createMockOrganization
import com.bitwarden.network.model.createMockPolicy
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.model.UserKeyConnectorState
import com.x8bit.bitwarden.data.auth.repository.model.UserOrganizations
import com.x8bit.bitwarden.data.auth.repository.model.VaultUnlockType
import com.x8bit.bitwarden.data.auth.repository.util.toOrganizations
import com.x8bit.bitwarden.data.auth.repository.util.toUserState
import com.x8bit.bitwarden.data.platform.manager.FirstTimeActionManager
import com.x8bit.bitwarden.data.platform.manager.PolicyManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.vault.manager.VaultLockManager
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import java.time.ZonedDateTime

class UserStateManagerTest {

    private val fakeAuthDiskSource = FakeAuthDiskSource()
    private val firstTimeActionManager = mockk<FirstTimeActionManager> {
        every { currentOrDefaultUserFirstTimeState } returns FIRST_TIME_STATE
        every { firstTimeStateFlow } returns MutableStateFlow(FIRST_TIME_STATE)
    }
    private val mutableVaultUnlockDataStateFlow = MutableStateFlow(VAULT_UNLOCK_DATA)
    private val mutableIsActiveUserUnlockingFlow = MutableStateFlow(false)
    private val vaultLockManager: VaultLockManager = mockk {
        every { vaultUnlockDataStateFlow } returns mutableVaultUnlockDataStateFlow
        every { isActiveUserUnlockingFlow } returns mutableIsActiveUserUnlockingFlow
    }
    private val dispatcherManager: DispatcherManager = FakeDispatcherManager()
    private val policyManager: PolicyManager = mockk {
        every { getUserPolicies(any(), any()) } returns emptyList()
    }

    private val userStateManager: UserStateManager = UserStateManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        firstTimeActionManager = firstTimeActionManager,
        vaultLockManager = vaultLockManager,
        dispatcherManager = dispatcherManager,
        policyManager = policyManager,
    )

    @BeforeEach
    fun setup() {
        mockkStatic(GetTokenResponseJson.Success::toUserState)
    }

    @AfterEach
    fun tearDown() {
        unmockkStatic(GetTokenResponseJson.Success::toUserState)
    }

    @Test
    fun `userStateFlow should update according to changes in its underlying data sources`() =
        runTest {
            fakeAuthDiskSource.userState = null
            userStateManager.userStateFlow.test {
                assertNull(awaitItem())

                mutableVaultUnlockDataStateFlow.value = VAULT_UNLOCK_DATA
                fakeAuthDiskSource.userState = SINGLE_USER_STATE_1
                assertEquals(
                    SINGLE_USER_STATE_1.toUserState(
                        vaultState = VAULT_UNLOCK_DATA,
                        userAccountTokens = emptyList(),
                        userOrganizationsList = emptyList(),
                        userIsUsingKeyConnectorList = emptyList(),
                        hasPendingAccountAddition = false,
                        onboardingStatus = null,
                        isBiometricsEnabledProvider = { false },
                        vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                        isDeviceTrustedProvider = { false },
                        firstTimeState = FIRST_TIME_STATE,
                        getUserPolicies = { _, _ -> emptyList() },
                    ),
                    awaitItem(),
                )

                fakeAuthDiskSource.apply {
                    storePinProtectedUserKeyEnvelope(
                        userId = USER_ID_1,
                        pinProtectedUserKeyEnvelope = "pinProtectedUseKey",
                    )
                    storePinProtectedUserKeyEnvelope(
                        userId = USER_ID_2,
                        pinProtectedUserKeyEnvelope = "pinProtectedUseKey",
                    )
                    userState = MULTI_USER_STATE
                }
                assertEquals(
                    MULTI_USER_STATE.toUserState(
                        vaultState = VAULT_UNLOCK_DATA,
                        userAccountTokens = emptyList(),
                        userOrganizationsList = emptyList(),
                        userIsUsingKeyConnectorList = emptyList(),
                        hasPendingAccountAddition = false,
                        isBiometricsEnabledProvider = { false },
                        vaultUnlockTypeProvider = { VaultUnlockType.PIN },
                        isDeviceTrustedProvider = { false },
                        onboardingStatus = null,
                        firstTimeState = FIRST_TIME_STATE,
                        getUserPolicies = { _, _ -> emptyList() },
                    ),
                    awaitItem(),
                )

                val emptyVaultState = emptyList<VaultUnlockData>()
                mutableVaultUnlockDataStateFlow.value = emptyVaultState
                assertEquals(
                    MULTI_USER_STATE.toUserState(
                        vaultState = emptyVaultState,
                        userAccountTokens = emptyList(),
                        userOrganizationsList = emptyList(),
                        userIsUsingKeyConnectorList = emptyList(),
                        hasPendingAccountAddition = false,
                        isBiometricsEnabledProvider = { false },
                        vaultUnlockTypeProvider = { VaultUnlockType.PIN },
                        isDeviceTrustedProvider = { false },
                        onboardingStatus = null,
                        firstTimeState = FIRST_TIME_STATE,
                        getUserPolicies = { _, _ -> emptyList() },
                    ),
                    awaitItem(),
                )

                fakeAuthDiskSource.apply {
                    storePinProtectedUserKeyEnvelope(
                        userId = USER_ID_1,
                        pinProtectedUserKeyEnvelope = null,
                    )
                    storePinProtectedUserKeyEnvelope(
                        userId = USER_ID_2,
                        pinProtectedUserKeyEnvelope = null,
                    )
                    storeOrganizations(
                        userId = USER_ID_1,
                        organizations = ORGANIZATIONS,
                    )
                }
                assertEquals(
                    MULTI_USER_STATE.toUserState(
                        vaultState = emptyVaultState,
                        userAccountTokens = emptyList(),
                        userOrganizationsList = USER_ORGANIZATIONS,
                        userIsUsingKeyConnectorList = USER_SHOULD_USER_KEY_CONNECTOR,
                        hasPendingAccountAddition = false,
                        isBiometricsEnabledProvider = { false },
                        vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                        isDeviceTrustedProvider = { false },
                        onboardingStatus = null,
                        firstTimeState = FIRST_TIME_STATE,
                        getUserPolicies = { _, _ -> emptyList() },
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `clear Pending Account Deletion should unblock userState updates`() = runTest {
        val originalUserState = SINGLE_USER_STATE_1.toUserState(
            vaultState = VAULT_UNLOCK_DATA,
            userAccountTokens = emptyList(),
            userOrganizationsList = emptyList(),
            userIsUsingKeyConnectorList = emptyList(),
            hasPendingAccountAddition = false,
            isBiometricsEnabledProvider = { false },
            vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
            isDeviceTrustedProvider = { false },
            onboardingStatus = null,
            firstTimeState = FIRST_TIME_STATE,
            getUserPolicies = { _, _ -> emptyList() },
        )
        val finalUserState = SINGLE_USER_STATE_2.toUserState(
            vaultState = VAULT_UNLOCK_DATA,
            userAccountTokens = emptyList(),
            userOrganizationsList = emptyList(),
            userIsUsingKeyConnectorList = emptyList(),
            hasPendingAccountAddition = false,
            isBiometricsEnabledProvider = { false },
            vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
            isDeviceTrustedProvider = { false },
            onboardingStatus = null,
            firstTimeState = FIRST_TIME_STATE,
            getUserPolicies = { _, _ -> emptyList() },
        )
        fakeAuthDiskSource.userState = SINGLE_USER_STATE_1

        userStateManager.userStateFlow.test {
            assertEquals(originalUserState, awaitItem())

            // Set the pending deletion flag
            userStateManager.hasPendingAccountDeletion = true

            // Update the account. No changes are emitted because
            // the pending deletion blocks the update.
            fakeAuthDiskSource.userState = SINGLE_USER_STATE_2
            expectNoEvents()

            // Clearing the pending deletion allows the change to go through
            userStateManager.hasPendingAccountDeletion = false
            assertEquals(finalUserState, awaitItem())
        }
    }

    @Test
    fun `hasPendingAccountAdditionStateFlow updates when hasPendingAccountAddition changes`() =
        runTest {
            userStateManager.hasPendingAccountAdditionStateFlow.test {
                assertFalse(awaitItem())
                userStateManager.hasPendingAccountAddition = true
                assertTrue(awaitItem())
                userStateManager.hasPendingAccountAddition = false
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `hasPendingAccountAddition updates when hasPendingAccountAddition changes`() {
        assertFalse(userStateManager.hasPendingAccountAddition)
        userStateManager.hasPendingAccountAddition = true
        assertTrue(userStateManager.hasPendingAccountAddition)
        userStateManager.hasPendingAccountAddition = false
        assertFalse(userStateManager.hasPendingAccountAddition)
    }

    @Test
    fun `hasPendingAccountDeletion updates when hasPendingAccountDeletion changes`() {
        assertFalse(userStateManager.hasPendingAccountDeletion)
        userStateManager.hasPendingAccountDeletion = true
        assertTrue(userStateManager.hasPendingAccountDeletion)
        userStateManager.hasPendingAccountDeletion = false
        assertFalse(userStateManager.hasPendingAccountDeletion)
    }

    @Test
    fun `userStateFlow should update isExportable when getUserPolicies returns policies`() =
        runTest {
            val policy = createMockPolicy(
                id = "policyId",
                organizationId = "mockId-1",
                type = PolicyTypeJson.DISABLE_PERSONAL_VAULT_EXPORT,
                data = null,
                isEnabled = true,
            )
            every { policyManager.getUserPolicies(any(), any()) } returns listOf(policy)

            fakeAuthDiskSource.userState = SINGLE_USER_STATE_1

            val userState = SINGLE_USER_STATE_1.toUserState(
                vaultState = VAULT_UNLOCK_DATA,
                userAccountTokens = emptyList(),
                userOrganizationsList = emptyList(),
                userIsUsingKeyConnectorList = emptyList(),
                hasPendingAccountAddition = false,
                isBiometricsEnabledProvider = { false },
                vaultUnlockTypeProvider = { VaultUnlockType.MASTER_PASSWORD },
                isDeviceTrustedProvider = { false },
                onboardingStatus = null,
                firstTimeState = FIRST_TIME_STATE,
                getUserPolicies = { _, _ -> listOf(policy) },
            )

            // Assert
            userStateManager.userStateFlow.test {
                val actualItem = awaitItem()
                assertEquals(userState, actualItem)
            }
        }
}

private const val EMAIL_1 = "test@bitwarden.com"
private const val EMAIL_2 = "test2@bitwarden.com"
private const val USER_ID_1 = "2a135b23-e1fb-42c9-bec3-573857bc8181"
private const val USER_ID_2 = "b9d32ec0-6497-4582-9798-b350f53bfa02"

private val FIRST_TIME_STATE = FirstTimeState(
    showImportLoginsCard = true,
)

private val ORGANIZATIONS = listOf(createMockOrganization(number = 0))
private val USER_ORGANIZATIONS = listOf(
    UserOrganizations(
        userId = USER_ID_1,
        organizations = ORGANIZATIONS.toOrganizations(),
    ),
)
private val USER_SHOULD_USER_KEY_CONNECTOR = listOf(
    UserKeyConnectorState(
        userId = USER_ID_1,
        isUsingKeyConnector = null,
    ),
)

private val VAULT_UNLOCK_DATA = listOf(
    VaultUnlockData(
        userId = USER_ID_1,
        status = VaultUnlockData.Status.UNLOCKED,
    ),
)

private val PROFILE_1 = AccountJson.Profile(
    userId = USER_ID_1,
    email = EMAIL_1,
    isEmailVerified = true,
    name = "Bitwarden Tester",
    hasPremium = false,
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
    creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
)
private val ACCOUNT_1 = AccountJson(
    profile = PROFILE_1,
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
    ),
)
private val ACCOUNT_2 = AccountJson(
    profile = AccountJson.Profile(
        userId = USER_ID_2,
        email = EMAIL_2,
        isEmailVerified = true,
        name = "Bitwarden Tester 2",
        hasPremium = false,
        stamp = null,
        organizationId = null,
        avatarColorHex = null,
        forcePasswordResetReason = null,
        kdfType = KdfTypeJson.PBKDF2_SHA256,
        kdfIterations = 400000,
        kdfMemory = null,
        kdfParallelism = null,
        userDecryptionOptions = null,
        isTwoFactorEnabled = true,
        creationDate = ZonedDateTime.parse("2024-09-13T01:00:00.00Z"),
    ),
    settings = AccountJson.Settings(
        environmentUrlData = EnvironmentUrlDataJson.DEFAULT_EU,
    ),
)
private val SINGLE_USER_STATE_1 = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
    ),
)
private val SINGLE_USER_STATE_2 = UserStateJson(
    activeUserId = USER_ID_2,
    accounts = mapOf(
        USER_ID_2 to ACCOUNT_2,
    ),
)
private val MULTI_USER_STATE = UserStateJson(
    activeUserId = USER_ID_1,
    accounts = mapOf(
        USER_ID_1 to ACCOUNT_1,
        USER_ID_2 to ACCOUNT_2,
    ),
)
