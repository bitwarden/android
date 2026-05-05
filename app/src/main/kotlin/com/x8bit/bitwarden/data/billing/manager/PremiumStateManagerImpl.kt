package com.x8bit.bitwarden.data.billing.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.util.isActive
import com.x8bit.bitwarden.data.platform.util.scanPairs
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Default implementation of [PremiumStateManager].
 */
@Suppress("LongParameterList")
class PremiumStateManagerImpl(
    private val authDiskSource: AuthDiskSource,
    authRepository: AuthRepository,
    private val billingRepository: BillingRepository,
    private val settingsDiskSource: SettingsDiskSource,
    vaultRepository: VaultRepository,
    private val featureFlagManager: FeatureFlagManager,
    pushManager: PushManager,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : PremiumStateManager {

    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isPremiumUpgradeBannerEligibleFlow: StateFlow<Boolean> =
        combine(
            authRepository.userStateFlow,
            billingRepository.isInAppBillingSupportedFlow,
            featureFlagManager.getFeatureFlagFlow(FlagKey.MobilePremiumUpgrade),
            authDiskSource.activeUserIdChangesFlow
                .flatMapLatest { userId ->
                    userId
                        ?.let { id ->
                            settingsDiskSource
                                .getPremiumUpgradeBannerDismissedFlow(id)
                                .map { it ?: false }
                        }
                        ?: flowOf(false)
                },
            vaultRepository.vaultDataStateFlow,
        ) {
                userState,
                isInAppBillingSupported,
                featureFlagEnabled,
                isDismissed,
                vaultDataState,
            ->
            val activeAccount = userState?.activeAccount
                ?: return@combine false
            val isPremium = activeAccount.isPremium
            val isAccountOldEnough = activeAccount.creationDate.isOlderThanDays(
                days = PREMIUM_UPGRADE_MINIMUM_ACCOUNT_AGE_DAYS,
                clock = clock,
            )
            val itemCount = vaultDataState.activeVaultItemCount()

            !isPremium &&
                isInAppBillingSupported &&
                featureFlagEnabled &&
                !isDismissed &&
                isAccountOldEnough &&
                itemCount >= PREMIUM_UPGRADE_MINIMUM_VAULT_ITEMS
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

    @OptIn(ExperimentalCoroutinesApi::class)
    override val isUpgradedToPremiumCardEligibleFlow: StateFlow<Boolean> =
        authDiskSource
            .activeUserIdChangesFlow
            .flatMapLatest { userId ->
                if (userId == null) {
                    flowOf(false)
                } else {
                    combine(
                        settingsDiskSource
                            .getUpgradedToPremiumCardPendingFlow(userId)
                            .map { it ?: false },
                        settingsDiskSource
                            .getUpgradedToPremiumCardConsumedFlow(userId)
                            .map { it ?: false },
                    ) { isPending, isConsumed -> isPending && !isConsumed }
                }
            }
            .distinctUntilChanged()
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

    init {
        // Personal premium upgrade signaled via push notification (standard flavor only). The
        // server emits PREMIUM_STATUS_CHANGED with the user's personal isPremium flag.
        pushManager
            .premiumStatusChangedFlow
            .onEach { data ->
                if (data.isPremium) {
                    markUpgradedToPremiumCardPending(userId = data.userId)
                }
            }
            .launchIn(unconfinedScope)

        // Sync-delta detection: observe the active user's premium flag transitioning false → true
        // (e.g., F-Droid users without push support). NOTE: UserState.Account.isPremium is
        // derived from `hasPremium = isPremium || isPremiumFromOrganization` so this path may
        // also fire for organization-granted premium. The push path (above) is personal-only and
        // takes precedence on flavors that support it.
        authRepository
            .userStateFlow
            .map { state ->
                state?.activeAccount?.let { it.userId to it.isPremium }
            }
            .distinctUntilChanged()
            .scanPairs()
            .onEach { (previous, current) ->
                if (current == null) return@onEach
                val (currentUserId, currentIsPremium) = current
                if (!currentIsPremium) return@onEach
                // Same user transitioning from non-premium to premium counts as an upgrade.
                if (previous?.first == currentUserId && !previous.second) {
                    markUpgradedToPremiumCardPending(userId = currentUserId)
                }
            }
            .launchIn(unconfinedScope)
    }

    override fun isInAppUpgradeAvailable(): Boolean =
        billingRepository.isInAppBillingSupportedFlow.value &&
            featureFlagManager.getFeatureFlag(FlagKey.MobilePremiumUpgrade)

    override fun dismissPremiumUpgradeBanner() {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storePremiumUpgradeBannerDismissed(
            userId = activeUserId,
            isDismissed = true,
        )
    }

    override fun dismissUpgradedToPremiumCard() {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storeUpgradedToPremiumCardConsumed(
            userId = activeUserId,
            isConsumed = true,
        )
        settingsDiskSource.storeUpgradedToPremiumCardPending(
            userId = activeUserId,
            isPending = false,
        )
    }

    private fun markUpgradedToPremiumCardPending(userId: String) {
        // Don't re-arm the card if the user has already consumed it for this account.
        if (settingsDiskSource.getUpgradedToPremiumCardConsumed(userId = userId) == true) {
            return
        }
        settingsDiskSource.storeUpgradedToPremiumCardPending(
            userId = userId,
            isPending = true,
        )
    }
}

/**
 * Returns `true` if this [Instant] is older than the given number of [days] based on
 * the provided [clock]. Returns `false` if the receiver is `null`.
 */
private fun Instant?.isOlderThanDays(days: Long, clock: Clock): Boolean {
    this ?: return false
    val now = clock.instant()
    val ageInDays = Duration.between(this, now).toDays()
    return ageInDays >= days
}

/**
 * Extracts the count of active (non-deleted, non-archived) vault items from the
 * current [DataState].
 */
private fun DataState<VaultData>.activeVaultItemCount(): Int =
    data
        ?.decryptCipherListResult
        ?.successes
        ?.count { it.isActive }
        ?: 0

private const val PREMIUM_UPGRADE_MINIMUM_VAULT_ITEMS: Int = 5
private const val PREMIUM_UPGRADE_MINIMUM_ACCOUNT_AGE_DAYS: Long = 7L
