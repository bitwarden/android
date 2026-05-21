package com.x8bit.bitwarden.data.billing.manager

import app.cash.turbine.test
import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.data.datasource.disk.model.EnvironmentUrlDataJson
import com.bitwarden.data.repository.model.Environment
import com.bitwarden.vault.DecryptCipherListResult
import com.x8bit.bitwarden.data.auth.datasource.disk.model.AccountJson
import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.auth.datasource.disk.util.FakeAuthDiskSource
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.model.PlanCadence
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionInfo
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionResult
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionStatusState
import com.x8bit.bitwarden.data.platform.datasource.disk.util.FakeSettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.manager.model.PremiumStatusChangedData
import com.x8bit.bitwarden.data.platform.repository.util.FakeEnvironmentRepository
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherListView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

@Suppress("LargeClass")
class PremiumStateManagerTest {

    private val fixedClock: Clock = Clock.fixed(
        Instant.parse(FIXED_DATETIME),
        ZoneOffset.UTC,
    )

    private val fakeAuthDiskSource = FakeAuthDiskSource().apply {
        userState = DEFAULT_USER_STATE_JSON
    }

    private val mutableIsInAppBillingSupportedFlow = MutableStateFlow(true)
    private val billingRepository: BillingRepository = mockk {
        every { isInAppBillingSupportedFlow } returns mutableIsInAppBillingSupportedFlow
        coEvery { getSubscription() } returns SubscriptionResult.NotFound
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
    private val mutableDebugDisableSelfHostPremiumCheckFlagFlow = MutableStateFlow(false)
    private val featureFlagManager: FeatureFlagManager = mockk {
        every {
            getFeatureFlagFlow(FlagKey.MobilePremiumUpgrade)
        } returns mutableMobilePremiumUpgradeFlagFlow
        every {
            getFeatureFlag(FlagKey.MobilePremiumUpgrade)
        } answers { mutableMobilePremiumUpgradeFlagFlow.value }
        every {
            getFeatureFlag(FlagKey.DebugDisableSelfHostPremiumCheck)
        } answers { mutableDebugDisableSelfHostPremiumCheckFlagFlow.value }
        every {
            getFeatureFlagFlow(FlagKey.DebugDisableSelfHostPremiumCheck)
        } returns mutableDebugDisableSelfHostPremiumCheckFlagFlow
    }

    private val fakeEnvironmentRepository = FakeEnvironmentRepository()

    private val dispatcherManager = FakeDispatcherManager()

    private val mutablePremiumStatusChangedFlow =
        MutableSharedFlow<PremiumStatusChangedData>(replay = 0, extraBufferCapacity = 1)
    private val pushManager: PushManager = mockk(relaxed = true) {
        every { premiumStatusChangedFlow } returns mutablePremiumStatusChangedFlow
    }

    private fun createManager(): PremiumStateManagerImpl = PremiumStateManagerImpl(
        authDiskSource = fakeAuthDiskSource,
        billingRepository = billingRepository,
        settingsDiskSource = fakeSettingsDiskSource,
        vaultRepository = vaultRepository,
        featureFlagManager = featureFlagManager,
        environmentRepository = fakeEnvironmentRepository,
        pushManager = pushManager,
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
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(hasPremiumPersonally = true),
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
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(
                creationDate = Instant.parse("2023-10-25T12:00:00Z"),
            ),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `ineligible when creation date is null should emit false`() = runTest {
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(creationDate = null),
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
        fakeAuthDiskSource.userState = null
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
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(
                    creationDate = Instant.parse("2023-10-20T12:00:00Z"),
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
    fun `isInAppUpgradeAvailable should return true when billing supported and flag enabled`() {
        val manager = createManager()
        assertTrue(manager.isInAppUpgradeAvailable())
    }

    @Test
    fun `isInAppUpgradeAvailable should return false when billing not supported`() {
        mutableIsInAppBillingSupportedFlow.value = false
        val manager = createManager()
        assertFalse(manager.isInAppUpgradeAvailable())
    }

    @Test
    fun `isInAppUpgradeAvailable should return false when feature flag disabled`() {
        mutableMobilePremiumUpgradeFlagFlow.value = false
        val manager = createManager()
        assertFalse(manager.isInAppUpgradeAvailable())
    }

    @Test
    fun `isInAppUpgradeAvailable should return false when both conditions are false`() {
        mutableIsInAppBillingSupportedFlow.value = false
        mutableMobilePremiumUpgradeFlagFlow.value = false
        val manager = createManager()
        assertFalse(manager.isInAppUpgradeAvailable())
    }

    @Test
    fun `isSelfHosted should return false on cloud environment regardless of flag`() {
        fakeEnvironmentRepository.environment = Environment.Us
        val manager = createManager()
        mutableDebugDisableSelfHostPremiumCheckFlagFlow.value = false
        assertFalse(manager.isSelfHosted)
        mutableDebugDisableSelfHostPremiumCheckFlagFlow.value = true
        assertFalse(manager.isSelfHosted)
    }

    @Test
    fun `isSelfHosted should return true on self-hosted environment when flag is disabled`() {
        fakeEnvironmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
        )
        mutableDebugDisableSelfHostPremiumCheckFlagFlow.value = false
        val manager = createManager()
        assertTrue(manager.isSelfHosted)
    }

    @Test
    fun `isSelfHosted should return false on self-hosted environment when flag is enabled`() {
        fakeEnvironmentRepository.environment = Environment.SelfHosted(
            environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
        )
        mutableDebugDisableSelfHostPremiumCheckFlagFlow.value = true
        val manager = createManager()
        assertFalse(manager.isSelfHosted)
    }

    @Test
    fun `isSelfHostedFlow should emit false on cloud environment regardless of flag`() = runTest {
        fakeEnvironmentRepository.environment = Environment.Us
        val manager = createManager()
        manager.isSelfHostedFlow.test {
            assertFalse(awaitItem())
            mutableDebugDisableSelfHostPremiumCheckFlagFlow.value = true
            expectNoEvents()
        }
    }

    @Test
    fun `isSelfHostedFlow should emit true on self-hosted environment when flag is disabled`() =
        runTest {
            fakeEnvironmentRepository.environment = Environment.SelfHosted(
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            )
            val manager = createManager()
            manager.isSelfHostedFlow.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `isSelfHostedFlow should re-emit when debug-disable flag toggles on self-hosted env`() =
        runTest {
            fakeEnvironmentRepository.environment = Environment.SelfHosted(
                environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US,
            )
            val manager = createManager()
            manager.isSelfHostedFlow.test {
                assertTrue(awaitItem())
                mutableDebugDisableSelfHostPremiumCheckFlagFlow.value = true
                assertFalse(awaitItem())
                mutableDebugDisableSelfHostPremiumCheckFlagFlow.value = false
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

    @Test
    fun `isPlanRowEligibleFlow emits true for a free user when feature flag is enabled`() =
        runTest {
            fakeAuthDiskSource.userState = userStateJsonWith(account = createAccountJson())
            val manager = createManager()
            manager.isPlanRowEligibleFlow.test {
                assertTrue(awaitItem())
            }
        }

    @Test
    fun `isPlanRowEligibleFlow emits true for a personal Premium user`() = runTest {
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(hasPremiumPersonally = true),
        )
        val manager = createManager()
        manager.isPlanRowEligibleFlow.test {
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `isPlanRowEligibleFlow emits false for a user with only org-granted Premium`() =
        runTest {
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(hasPremiumFromOrganization = true),
            )
            val manager = createManager()
            manager.isPlanRowEligibleFlow.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `isPlanRowEligibleFlow emits false when feature flag is disabled`() = runTest {
        mutableMobilePremiumUpgradeFlagFlow.value = false
        val manager = createManager()
        manager.isPlanRowEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isPlanRowEligibleFlow emits false when userState is null`() = runTest {
        fakeAuthDiskSource.userState = null
        val manager = createManager()
        manager.isPlanRowEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `isPlanRowEligibleFlow updates as the user gains and loses org-only Premium`() = runTest {
        fakeAuthDiskSource.userState = userStateJsonWith(account = createAccountJson())
        val manager = createManager()
        manager.isPlanRowEligibleFlow.test {
            assertTrue(awaitItem())
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(hasPremiumFromOrganization = true),
            )
            assertFalse(awaitItem())
            fakeAuthDiskSource.userState = userStateJsonWith(account = createAccountJson())
            assertTrue(awaitItem())
        }
    }

    @Test
    fun `isUpgradedToPremiumCardEligibleFlow emits false when nothing has been observed`() =
        runTest {
            val manager = createManager()
            manager.isUpgradedToPremiumCardEligibleFlow.test {
                assertFalse(awaitItem())
            }
        }

    @Test
    fun `premium-status push with isPremium=true marks the card pending and emits true`() =
        runTest {
            // Sync has caught up to the upgrade by the time the push lands — the active user
            // already holds personal Premium.
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(hasPremiumPersonally = true),
            )
            val manager = createManager()
            manager.isUpgradedToPremiumCardEligibleFlow.test {
                assertFalse(awaitItem())
                mutablePremiumStatusChangedFlow.tryEmit(
                    PremiumStatusChangedData(
                        userId = ACTIVE_USER_ID,
                        isPremium = true,
                    ),
                )
                assertTrue(awaitItem())
                fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
                    userId = ACTIVE_USER_ID,
                    expected = true,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `isUpgradedToPremiumCardEligibleFlow emits false when pending is set but the active user lacks personal Premium`() =
        runTest {
            // Simulates the debug-menu trigger (or a stray PREMIUM_STATUS_CHANGED push routed to
            // an organization-granted user): pending is written directly to disk, but the active
            // user has no personal subscription. The read-side gate must withhold the card.
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(hasPremiumFromOrganization = true),
            )
            fakeSettingsDiskSource.storeUpgradedToPremiumCardPending(
                userId = ACTIVE_USER_ID,
                isPending = true,
            )
            val manager = createManager()
            manager.isUpgradedToPremiumCardEligibleFlow.test {
                assertFalse(awaitItem())
                expectNoEvents()
            }
        }

    @Test
    fun `premium-status push with isPremium=false should not mark the card pending`() =
        runTest {
            val manager = createManager()
            manager.isUpgradedToPremiumCardEligibleFlow.test {
                assertFalse(awaitItem())
                mutablePremiumStatusChangedFlow.tryEmit(
                    PremiumStatusChangedData(
                        userId = ACTIVE_USER_ID,
                        isPremium = false,
                    ),
                )
                expectNoEvents()
                fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
                    userId = ACTIVE_USER_ID,
                    expected = null,
                )
            }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `userState transition from non-Premium to personal Premium for the same user marks card pending`() =
        runTest {
            // Start as Free.
            fakeAuthDiskSource.userState = userStateJsonWith(account = createAccountJson())
            val manager = createManager()
            manager.isUpgradedToPremiumCardEligibleFlow.test {
                assertFalse(awaitItem())
                // Transition to personal Premium for the same user.
                fakeAuthDiskSource.userState = userStateJsonWith(
                    account = createAccountJson(hasPremiumPersonally = true),
                )
                assertTrue(awaitItem())
                fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
                    userId = ACTIVE_USER_ID,
                    expected = true,
                )
            }
        }

    @Test
    fun `userState transition that only flips org-granted Premium should not mark card pending`() =
        runTest {
            fakeAuthDiskSource.userState = userStateJsonWith(account = createAccountJson())
            val manager = createManager()
            manager.isUpgradedToPremiumCardEligibleFlow.test {
                assertFalse(awaitItem())
                // Aggregate isPremium becomes true via the user's organization, but personal
                // premium stays false — the card must not arm.
                fakeAuthDiskSource.userState = userStateJsonWith(
                    account = createAccountJson(hasPremiumFromOrganization = true),
                )
                expectNoEvents()
                fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
                    userId = ACTIVE_USER_ID,
                    expected = null,
                )
            }
        }

    @Test
    fun `Premium account that signs in for the first time should not mark the card pending`() =
        runTest {
            // Initial userState is null, then becomes a Premium account in one step.
            fakeAuthDiskSource.userState = null
            val manager = createManager()
            manager.isUpgradedToPremiumCardEligibleFlow.test {
                assertFalse(awaitItem())
                fakeAuthDiskSource.userState = userStateJsonWith(
                    account = createAccountJson(hasPremiumPersonally = true),
                )
                expectNoEvents()
                fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
                    userId = ACTIVE_USER_ID,
                    expected = null,
                )
            }
        }

    @Test
    fun `card not re-armed once consumed for the user`() = runTest {
        fakeSettingsDiskSource.storeUpgradedToPremiumCardConsumed(
            userId = ACTIVE_USER_ID,
            isConsumed = true,
        )
        val manager = createManager()
        manager.isUpgradedToPremiumCardEligibleFlow.test {
            assertFalse(awaitItem())
            mutablePremiumStatusChangedFlow.tryEmit(
                PremiumStatusChangedData(
                    userId = ACTIVE_USER_ID,
                    isPremium = true,
                ),
            )
            expectNoEvents()
            fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
                userId = ACTIVE_USER_ID,
                expected = null,
            )
        }
    }

    @Test
    fun `dismissUpgradedToPremiumCard marks the card consumed and clears pending`() {
        fakeSettingsDiskSource.storeUpgradedToPremiumCardPending(
            userId = ACTIVE_USER_ID,
            isPending = true,
        )
        val manager = createManager()
        manager.dismissUpgradedToPremiumCard()
        fakeSettingsDiskSource.assertUpgradedToPremiumCardConsumed(
            userId = ACTIVE_USER_ID,
            expected = true,
        )
        fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
            userId = ACTIVE_USER_ID,
            expected = false,
        )
    }

    @Test
    fun `dismissUpgradedToPremiumCard with no active user is a no-op`() {
        fakeAuthDiskSource.userState = null
        val manager = createManager()
        manager.dismissUpgradedToPremiumCard()
        fakeSettingsDiskSource.assertUpgradedToPremiumCardConsumed(
            userId = ACTIVE_USER_ID,
            expected = null,
        )
        fakeSettingsDiskSource.assertUpgradedToPremiumCardPending(
            userId = ACTIVE_USER_ID,
            expected = null,
        )
    }

    @Test
    fun `subscriptionStatusStateFlow emits NoSubscription when there is no active user`() =
        runTest {
            fakeAuthDiskSource.userState = null
            val manager = createManager()
            manager.subscriptionStatusStateFlow.test {
                assertEquals(SubscriptionStatusState.NoSubscription, awaitItem())
            }
        }

    @Test
    fun `subscriptionStatusStateFlow emits NoSubscription on 404 for a free active user`() =
        runTest {
            // Default mock returns SubscriptionResult.NotFound; the fetch runs even when
            // isPremium is false so users with canceled / expired subscriptions are routed
            // through the Premium view rather than the Free view.
            val manager = createManager()
            manager.subscriptionStatusStateFlow.test {
                assertEquals(SubscriptionStatusState.NoSubscription, awaitItem())
            }
            coVerify(exactly = 1) { billingRepository.getSubscription() }
        }

    @Suppress("MaxLineLength")
    @Test
    fun `subscriptionStatusStateFlow emits Available when free active user has a canceled subscription`() =
        runTest {
            coEvery {
                billingRepository.getSubscription()
            } returns SubscriptionResult.Success(
                subscription = createSubscriptionInfo(
                    status = PremiumSubscriptionStatus.CANCELED,
                ),
            )
            val manager = createManager()
            manager.subscriptionStatusStateFlow.test {
                assertEquals(
                    SubscriptionStatusState.Available(
                        status = PremiumSubscriptionStatus.CANCELED,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `subscriptionStatusStateFlow emits NoSubscription on 404 from BillingRepository`() =
        runTest {
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(hasPremiumPersonally = true),
            )
            coEvery { billingRepository.getSubscription() } returns SubscriptionResult.NotFound
            val manager = createManager()
            manager.subscriptionStatusStateFlow.test {
                assertEquals(SubscriptionStatusState.NoSubscription, awaitItem())
            }
        }

    @Test
    fun `subscriptionStatusStateFlow emits Available with status on Success`() = runTest {
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(hasPremiumPersonally = true),
        )
        coEvery {
            billingRepository.getSubscription()
        } returns SubscriptionResult.Success(
            subscription = createSubscriptionInfo(
                status = PremiumSubscriptionStatus.CANCELED,
            ),
        )
        val manager = createManager()
        manager.subscriptionStatusStateFlow.test {
            assertEquals(
                SubscriptionStatusState.Available(
                    status = PremiumSubscriptionStatus.CANCELED,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `subscriptionStatusStateFlow emits Error on non-404 failure`() = runTest {
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(hasPremiumPersonally = true),
        )
        val exception = IllegalStateException("boom")
        coEvery {
            billingRepository.getSubscription()
        } returns SubscriptionResult.Error(error = exception)
        val manager = createManager()
        manager.subscriptionStatusStateFlow.test {
            assertEquals(SubscriptionStatusState.Error(throwable = exception), awaitItem())
        }
    }

    @Test
    fun `subscriptionStatusStateFlow refetches when active user changes`() = runTest {
        coEvery {
            billingRepository.getSubscription()
        } returns SubscriptionResult.Success(
            subscription = createSubscriptionInfo(
                status = PremiumSubscriptionStatus.ACTIVE,
            ),
        ) andThen SubscriptionResult.Success(
            subscription = createSubscriptionInfo(
                status = PremiumSubscriptionStatus.CANCELED,
            ),
        )
        val manager = createManager()
        manager.subscriptionStatusStateFlow.test {
            assertEquals(
                SubscriptionStatusState.Available(
                    status = PremiumSubscriptionStatus.ACTIVE,
                ),
                awaitItem(),
            )
            val otherUserId = "otherUserId"
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(userId = otherUserId),
            )
            assertEquals(
                SubscriptionStatusState.Available(
                    status = PremiumSubscriptionStatus.CANCELED,
                ),
                awaitItem(),
            )
        }
        coVerify(exactly = 2) { billingRepository.getSubscription() }
    }

    @Test
    fun `subscriptionStatusStateFlow refetches on push when active user is premium`() =
        runTest {
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(hasPremiumPersonally = true),
            )
            coEvery {
                billingRepository.getSubscription()
            } returns SubscriptionResult.Success(
                subscription = createSubscriptionInfo(
                    status = PremiumSubscriptionStatus.ACTIVE,
                ),
            ) andThen SubscriptionResult.Success(
                subscription = createSubscriptionInfo(
                    status = PremiumSubscriptionStatus.PAST_DUE,
                ),
            )
            val manager = createManager()
            manager.subscriptionStatusStateFlow.test {
                assertEquals(
                    SubscriptionStatusState.Available(
                        status = PremiumSubscriptionStatus.ACTIVE,
                    ),
                    awaitItem(),
                )
                mutablePremiumStatusChangedFlow.tryEmit(
                    PremiumStatusChangedData(
                        userId = ACTIVE_USER_ID,
                        isPremium = true,
                    ),
                )
                assertEquals(
                    SubscriptionStatusState.Available(
                        status = PremiumSubscriptionStatus.PAST_DUE,
                    ),
                    awaitItem(),
                )
            }
        }

    @Test
    fun `banner ineligible when account is premium and status is ACTIVE`() = runTest {
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(hasPremiumPersonally = true),
        )
        coEvery {
            billingRepository.getSubscription()
        } returns SubscriptionResult.Success(
            subscription = createSubscriptionInfo(status = PremiumSubscriptionStatus.ACTIVE),
        )
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            assertFalse(awaitItem())
        }
    }

    @Test
    fun `banner eligible when account is premium but status is in a trouble state`() = runTest {
        listOf(
            PremiumSubscriptionStatus.CANCELED,
            PremiumSubscriptionStatus.PAST_DUE,
            PremiumSubscriptionStatus.PAUSED,
            PremiumSubscriptionStatus.UPDATE_PAYMENT,
        ).forEach { status ->
            fakeAuthDiskSource.userState = userStateJsonWith(
                account = createAccountJson(hasPremiumPersonally = true),
            )
            coEvery {
                billingRepository.getSubscription()
            } returns SubscriptionResult.Success(
                subscription = createSubscriptionInfo(status = status),
            )
            val manager = createManager()
            manager.isPremiumUpgradeBannerEligibleFlow.test {
                assertTrue(awaitItem(), "Expected banner eligible for status=$status")
            }
        }
    }

    @Test
    fun `banner ineligible when account is premium and substate is still loading`() = runTest {
        fakeAuthDiskSource.userState = userStateJsonWith(
            account = createAccountJson(hasPremiumPersonally = true),
        )
        coEvery {
            billingRepository.getSubscription()
        } coAnswers {
            kotlinx.coroutines.awaitCancellation()
        }
        val manager = createManager()
        manager.isPremiumUpgradeBannerEligibleFlow.test {
            // Loading is not treated as a trouble state so a premium user is still effectively
            // premium during the initial fetch.
            assertFalse(awaitItem())
        }
    }
}

private fun createSubscriptionInfo(
    status: PremiumSubscriptionStatus,
): SubscriptionInfo = SubscriptionInfo(
    status = status,
    cadence = PlanCadence.ANNUALLY,
    seatsCost = java.math.BigDecimal("19.80"),
    storageCost = null,
    discountAmount = null,
    estimatedTax = java.math.BigDecimal.ZERO,
    nextChargeTotal = java.math.BigDecimal("19.80"),
    nextCharge = null,
    canceledDate = null,
    suspensionDate = null,
    gracePeriodDays = null,
)

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
private val DEFAULT_CREATION_DATE: Instant = Instant.parse("2023-10-01T12:00:00Z")

/**
 * Builds an [AccountJson] for tests, defaulting to a non-premium account whose creation date is
 * old enough to satisfy the banner's account-age gate.
 */
private fun createAccountJson(
    userId: String = ACTIVE_USER_ID,
    hasPremiumPersonally: Boolean = false,
    hasPremiumFromOrganization: Boolean = false,
    creationDate: Instant? = DEFAULT_CREATION_DATE,
): AccountJson = AccountJson(
    profile = AccountJson.Profile(
        userId = userId,
        email = "active@bitwarden.com",
        isEmailVerified = true,
        isTwoFactorEnabled = false,
        name = "Active User",
        stamp = null,
        organizationId = null,
        avatarColorHex = "#aa00aa",
        hasPremiumPersonally = hasPremiumPersonally,
        hasPremiumFromOrganization = hasPremiumFromOrganization,
        forcePasswordResetReason = null,
        kdfType = null,
        kdfIterations = null,
        kdfMemory = null,
        kdfParallelism = null,
        userDecryptionOptions = null,
        creationDate = creationDate,
    ),
    settings = AccountJson.Settings(environmentUrlData = EnvironmentUrlDataJson.DEFAULT_US),
)

/**
 * Builds a [UserStateJson] whose active account is [account], keyed by `userId`.
 */
private fun userStateJsonWith(account: AccountJson): UserStateJson = UserStateJson(
    activeUserId = account.profile.userId,
    accounts = mapOf(account.profile.userId to account),
)

private val DEFAULT_ACTIVE_ACCOUNT_JSON: AccountJson = createAccountJson()
private val DEFAULT_USER_STATE_JSON: UserStateJson =
    userStateJsonWith(account = DEFAULT_ACTIVE_ACCOUNT_JSON)
