package com.x8bit.bitwarden.data.billing.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.OnboardingStatus
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.model.FirstTimeState
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class PremiumStateManagerImplTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse(FIXED_DATETIME),
        ZoneOffset.UTC,
    )

    private val fakeAuthDiskSource = FakeAuthDiskSource().apply {
        userState = DISK_USER_STATE
    }

    private val mutableUserStateFlow = MutableStateFlow<UserState?>(DEFAULT_USER_STATE)
    private val authRepository: AuthRepository = mockk {
        every { userStateFlow } returns mutableUserStateFlow
    }

    private val mutableIsInAppBillingSupportedFlow = MutableStateFlow(true)
    private val billingRepository: BillingRepository = mockk {
        every { isInAppBillingSupportedFlow } returns mutableIsInAppBillingSupportedFlow
    }

    private val fakeSettingsDiskSource = FakeSettingsDiskSource()

    private val mutableVaultDataStateFlow =
        MutableStateFlow<DataState<VaultData>>(
            DataState.Loaded(createVaultDataWithItemCount(count = 5)),
        )
    private val vaultRepository: VaultRepository = mockk {
        every { vaultDataStateFlow } returns mutableVaultDataStateFlow
    }

    private val mutableMobilePremiumUpgradeFlagFlow = MutableStateFlow(true)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlagFlow(FlagKey.MobilePremiumUpgrade)
        } returns mutableMobilePremiumUpgradeFlagFlow
    }

    private val dispatcherManager = FakeDispatcherManager()

    private fun createManager(): PremiumStateManagerImpl = PremiumStateManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        authRepository = authRepository,
        billingRepository = billingRepository,
        settingsDiskSource = fakeSettingsDiskSource,
        vaultRepository = vaultRepository,
        featureFlagManager = featureFlagManager,
        clock = fixedClock,
        dispatcherManager = dispatcherManager,
    )

    @Test
    fun `eligible when all conditions met should emit true`() = runTest {
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `ineligible when user is Premium should emit false`() = runTest {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(DEFAULT_ACTIVE_ACCOUNT.copy(isPremium = true)),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ineligible when in-app billing is not supported should emit false`() =
        runTest {
            mutableIsInAppBillingSupportedFlow.value = false
            val manager = createManager()
            manager.isPremiumUpgradeBannerEligibleFlow.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `ineligible when feature flag is disabled should emit false`() = runTest {
        mutableMobilePremiumUpgradeFlagFlow.value = false
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ineligible when banner is dismissed should emit false`() = runTest {
        fakeSettingsDiskSource.storePremiumUpgradeBannerDismissed(
            userId = ACTIVE_USER_ID,
            isDismissed = true,
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ineligible when account is too new should emit false`() = runTest {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACTIVE_ACCOUNT.copy(
                    creationDate = Instant.parse("2023-10-25T12:00:00Z"),
                ),
            ),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ineligible when creation date is null should emit false`() = runTest {
        mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
            accounts = listOf(
                DEFAULT_ACTIVE_ACCOUNT.copy(creationDate = null),
            ),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ineligible when vault has fewer than 5 items should emit false`() = runTest {
        mutableVaultDataStateFlow.value = DataState.Loaded(
            createVaultDataWithItemCount(count = 4),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ineligible when userState is null should emit false`() = runTest {
        mutableUserStateFlow.value = null
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `vault data Loading should emit false`() = runTest {
        mutableVaultDataStateFlow.value = DataState.Loading
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `vault data Pending with enough items should emit true`() = runTest {
        mutableVaultDataStateFlow.value = DataState.Pending(
            createVaultDataWithItemCount(count = 5),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `vault data NoNetwork with enough items should emit true`() = runTest {
        mutableVaultDataStateFlow.value = DataState.NoNetwork(
            createVaultDataWithItemCount(count = 5),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `vault data Error with enough items should emit true`() = runTest {
        mutableVaultDataStateFlow.value = DataState.Error(
            error = IllegalStateException("test"),
            data = createVaultDataWithItemCount(count = 5),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `vault data NoNetwork without data should emit false`() = runTest {
        mutableVaultDataStateFlow.value = DataState.NoNetwork(data = null)
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `vault data Error without data should emit false`() = runTest {
        mutableVaultDataStateFlow.value = DataState.Error(
            error = IllegalStateException("test"),
            data = null,
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `deleted items should not be counted toward vault item threshold`() =
        runTest {
            val ciphers = (1..4).map {
                createMockCipherListView(number = it)
            } + createMockCipherListView(number = 5, isDeleted = true)
            mutableVaultDataStateFlow.value = DataState.Loaded(
                VaultData(
                    decryptCipherListResult = DecryptCipherListResult(
                        successes = ciphers,
                        failures = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val manager = createManager()
            manager.isPremiumUpgradeBannerEligibleFlow.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `archived items should not be counted toward vault item threshold`() =
        runTest {
            val ciphers = (1..4).map {
                createMockCipherListView(number = it)
            } + createMockCipherListView(number = 5, isArchived = true)
            mutableVaultDataStateFlow.value = DataState.Loaded(
                VaultData(
                    decryptCipherListResult = DecryptCipherListResult(
                        successes = ciphers,
                        failures = emptyList(),
                    ),
                    collectionViewList = emptyList(),
                    folderViewList = emptyList(),
                    sendViewList = emptyList(),
                ),
            )
            val manager = createManager()
            manager.isPremiumUpgradeBannerEligibleFlow.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `eligible when account age is exactly 7 days should emit true`() =
        runTest {
            mutableUserStateFlow.value = DEFAULT_USER_STATE.copy(
                accounts = listOf(
                    DEFAULT_ACTIVE_ACCOUNT.copy(
                        creationDate = Instant.parse("2023-10-20T12:00:00Z"),
                    ),
                ),
            )
            val manager = createManager()
            manager.isPremiumUpgradeBannerEligibleFlow.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `eligible when vault items exactly 5 should emit true`() = runTest {
        mutableVaultDataStateFlow.value = DataState.Loaded(
            createVaultDataWithItemCount(count = 5),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `eligibility should update when upstream flows change`() = runTest {
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertTrue(awaitItem())

            mutableIsInAppBillingSupportedFlow.value = false
            assertFalse(awaitItem())

            mutableIsInAppBillingSupportedFlow.value = true
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `dismissPremiumUpgradeBanner should store dismissed state for active user`() {
        val manager = createManager()
        manager.dismissPremiumUpgradeBanner()
        fakeSettingsDiskSource.assertPremiumUpgradeBannerDismissed(
            userId = ACTIVE_USER_ID,
            expected = true,
        )
    }

    @Test
    fun `dismissPremiumUpgradeBanner should do nothing when no active user`() {
        fakeAuthDiskSource.userState = null
        val manager = createManager()
        manager.dismissPremiumUpgradeBanner()
        fakeSettingsDiskSource.assertPremiumUpgradeBannerDismissed(
            userId = ACTIVE_USER_ID,
            expected = null,
        )
    }
}

/**
 * Creates [VaultData] with the given number of non-deleted cipher items.
 */
private fun createVaultDataWithItemCount(count: Int): VaultData = VaultData(
    decryptCipherListResult = DecryptCipherListResult(
        successes = (1..count).map { createMockCipherListView(number = it) },
        failures = emptyList(),
    ),
    collectionViewList = emptyList(),
    folderViewList = emptyList(),
    sendViewList = emptyList(),
)

private const val ACTIVE_USER_ID = "activeUserId"
private const val FIXED_DATETIME = "2023-10-27T12:00:00Z"

private val DEFAULT_ACTIVE_ACCOUNT = UserState.Account(
    userId = ACTIVE_USER_ID,
    name = "Active User",
    email = "active@bitwarden.com",
    avatarColorHex = "#aa00aa",
    environment = com.bitwarden.data.repository.model.Environment.Us,
    isPremium = false,
    isLoggedIn = true,
    isVaultUnlocked = true,
    needsPasswordReset = false,
    isBiometricsEnabled = false,
    organizations = emptyList(),
    needsMasterPassword = false,
    trustedDevice = null,
    hasMasterPassword = true,
    isUsingKeyConnector = false,
    onboardingStatus = OnboardingStatus.COMPLETE,
    firstTimeState = FirstTimeState(),
    isExportable = true,
    creationDate = Instant.parse("2023-10-01T12:00:00Z"),
)

private val DEFAULT_USER_STATE = UserState(
    activeUserId = ACTIVE_USER_ID,
    accounts = listOf(DEFAULT_ACTIVE_ACCOUNT),
)

private val DISK_USER_STATE = UserStateJson(
    activeUserId = ACTIVE_USER_ID,
    accounts = mapOf(
        ACTIVE_USER_ID to mockk<AccountJson>(),
    ),
)
