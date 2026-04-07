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
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import com.x8bit.bitwarden.data.vault.repository.model.VaultData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.Duration
import java.time.Instant

/**
 * Default implementation of [PremiumStateManager].
 *
 * Combines five upstream flows into a single eligibility signal using [combine].
 */
@Suppress("LongParameterList")
class PremiumStateManagerImpl(
    private val authDiskSource: AuthDiskSource,
    authRepository: AuthRepository,
    billingRepository: BillingRepository,
    private val settingsDiskSource: SettingsDiskSource,
    vaultRepository: VaultRepository,
    featureFlagManager: FeatureFlagManager,
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
        ) { userState,
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

    override fun dismissPremiumUpgradeBanner() {
        val activeUserId = authDiskSource.userState?.activeUserId ?: return
        settingsDiskSource.storePremiumUpgradeBannerDismissed(
            userId = activeUserId,
            isDismissed = true,
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
        ?.count { it.deletedDate == null && it.archivedDate == null }
        ?: 0

private const val PREMIUM_UPGRADE_MINIMUM_VAULT_ITEMS: Int = 5
private const val PREMIUM_UPGRADE_MINIMUM_ACCOUNT_AGE_DAYS: Long = 7L
