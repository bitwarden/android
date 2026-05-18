package com.x8bit.bitwarden.data.billing.manager

import com.bitwarden.core.data.manager.dispatcher.DispatcherManager
import com.bitwarden.core.data.manager.model.FlagKey
import com.bitwarden.core.data.repository.model.DataState
import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.UserState
import com.x8bit.bitwarden.data.auth.repository.util.activeUserIdChangesFlow
import com.x8bit.bitwarden.data.billing.repository.BillingRepository
import com.x8bit.bitwarden.data.billing.repository.model.PremiumSubscriptionStatus
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionResult
import com.x8bit.bitwarden.data.billing.repository.model.SubscriptionStatusState
import com.x8bit.bitwarden.data.platform.datasource.disk.SettingsDiskSource
import com.x8bit.bitwarden.data.platform.manager.FeatureFlagManager
import com.x8bit.bitwarden.data.platform.manager.PushManager
import com.x8bit.bitwarden.data.platform.util.isActive
import com.x8bit.bitwarden.data.platform.util.scanPairs
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Default implementation of [PremiumStateManager].
 */
@Suppress("LongParameterList", "LargeClass")
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

    private val subscriptionRefreshTriggerFlow =
        MutableSharedFlow<Unit>(replay = 0, extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val subscriptionStatusStateFlow: StateFlow<SubscriptionStatusState> =
        authRepository
            .userStateFlow
            .map { state -> state?.activeAccount?.let { it.userId to it.isPremium } }
            .distinctUntilChanged()
            .flatMapLatest { activeAccount ->
                if (activeAccount?.second == true) {
                    fetchSubscriptionStatusFlow()
                } else {
                    flowOf(SubscriptionStatusState.NoSubscription)
                }
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = SubscriptionStatusState.Loading,
            )

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
            BannerInputs(
                userState = userState,
                isInAppBillingSupported = isInAppBillingSupported,
                featureFlagEnabled = featureFlagEnabled,
                isDismissed = isDismissed,
                vaultDataState = vaultDataState,
            )
        }
            .combine(subscriptionStatusStateFlow) { inputs, subscriptionStatus ->
                val activeAccount = inputs.userState?.activeAccount
                    ?: return@combine false
                val isAccountOldEnough = activeAccount.creationDate.isOlderThanDays(
                    days = PREMIUM_UPGRADE_MINIMUM_ACCOUNT_AGE_DAYS,
                    clock = clock,
                )
                val itemCount = inputs.vaultDataState.activeVaultItemCount()
                val isEffectivelyPremium = activeAccount.isPremium &&
                    !subscriptionStatus.isInTroubleState()

                !isEffectivelyPremium &&
                    inputs.isInAppBillingSupported &&
                    inputs.featureFlagEnabled &&
                    !inputs.isDismissed &&
                    isAccountOldEnough &&
                    itemCount >= PREMIUM_UPGRADE_MINIMUM_VAULT_ITEMS
            }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

    /**
     * Eligibility is keyed on the user holding personal Premium (or being eligible to purchase
     * it). Organization-granted Premium does not surface the Plan row, since the user has no
     * personal subscription to manage.
     */
    override val isPlanRowEligibleFlow: StateFlow<Boolean> =
        combine(
            authRepository.userStateFlow,
            featureFlagManager.getFeatureFlagFlow(FlagKey.MobilePremiumUpgrade),
        ) { userState, featureFlagEnabled ->
            val activeAccount = userState?.activeAccount ?: return@combine false
            val isOrgOnlyPremium = activeAccount.isPremium && !activeAccount.isPremiumFromSelf
            featureFlagEnabled && !isOrgOnlyPremium
        }
            .stateIn(
                scope = unconfinedScope,
                started = SharingStarted.Eagerly,
                initialValue = false,
            )

    /**
     * The card surfaces only while the active user holds personal Premium. This guards against
     * non-personal upgrade signals (e.g., the debug menu trigger or a stray
     * `PREMIUM_STATUS_CHANGED` push for an organization grant) marking the card pending for users
     * with no personal subscription to celebrate.
     */
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
                        authRepository
                            .userStateFlow
                            .map { it?.activeAccount?.isPremiumFromSelf == true },
                    ) { isPending, isConsumed, isPremiumFromSelf ->
                        isPending && !isConsumed && isPremiumFromSelf
                    }
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
                // Push always re-fetches: a status push can mean either "newly premium" or
                // "subscription moved into a trouble state" (e.g. past_due → unpaid).
                subscriptionRefreshTriggerFlow.tryEmit(Unit)
            }
            .launchIn(unconfinedScope)

        // Sync-delta detection: observe the active user's personal premium flag transitioning
        // false → true (e.g., F-Droid users without push support). Keyed on `isPremiumFromSelf`
        // so that organization-granted premium does not trigger the personal-upgrade card.
        authRepository
            .userStateFlow
            .map { state ->
                state?.activeAccount?.let { it.userId to it.isPremiumFromSelf }
            }
            .distinctUntilChanged()
            .scanPairs()
            .onEach { (previous, current) ->
                if (current == null) return@onEach
                val (currentUserId, currentIsPremiumFromSelf) = current
                if (!currentIsPremiumFromSelf) return@onEach
                // Same user transitioning from non-personal-premium to personal-premium counts as
                // an upgrade.
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun fetchSubscriptionStatusFlow(): Flow<SubscriptionStatusState> =
        merge(
            flowOf(Unit),
            subscriptionRefreshTriggerFlow,
        )
            .flatMapLatest {
                flow {
                    emit(SubscriptionStatusState.Loading)
                    emit(fetchSubscriptionStatusOnce())
                }
            }

    private suspend fun fetchSubscriptionStatusOnce(): SubscriptionStatusState =
        when (val result = billingRepository.getSubscription()) {
            is SubscriptionResult.Success ->
                SubscriptionStatusState.Available(status = result.subscription.status)

            SubscriptionResult.NotFound -> SubscriptionStatusState.NoSubscription
            is SubscriptionResult.Error ->
                SubscriptionStatusState.Error(throwable = result.error)
        }
}

private data class BannerInputs(
    val userState: UserState?,
    val isInAppBillingSupported: Boolean,
    val featureFlagEnabled: Boolean,
    val isDismissed: Boolean,
    val vaultDataState: DataState<VaultData>,
)

/**
 * Returns `true` when the given [SubscriptionStatusState] represents a subscription substate
 * that should disqualify a user from being treated as effectively premium.
 */
private fun SubscriptionStatusState.isInTroubleState(): Boolean = this is
    SubscriptionStatusState.Available &&
    when (this.status) {
        PremiumSubscriptionStatus.CANCELED,
        PremiumSubscriptionStatus.PAST_DUE,
        PremiumSubscriptionStatus.PAUSED,
        PremiumSubscriptionStatus.UPDATE_PAYMENT,
        -> true

        PremiumSubscriptionStatus.ACTIVE -> false
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
