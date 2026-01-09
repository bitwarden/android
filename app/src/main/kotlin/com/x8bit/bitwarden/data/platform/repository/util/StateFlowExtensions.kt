package com.x8bit.bitwarden.data.platform.repository.util

import com.x8bit.bitwarden.data.auth.datasource.disk.model.UserStateJson
import com.x8bit.bitwarden.data.vault.repository.model.VaultUnlockData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * Lazily invokes the [observer] callback with the active user's ID only when this MutableStateFlow
 * has external collectors and a user is logged in. Designed for operations that should only run
 * when UI actively observes the resulting data, but do not require the vault to be unlocked.
 *
 * **Active User Tracking:**
 * This function specifically tracks the active user from [userStateFlow]. When the active user
 * changes (e.g., account switching), the previous observer flow is canceled and a new one is
 * started for the new active user.
 *
 * **Subscription Detection:**
 * Uses [MutableStateFlow.subscriptionCount] to detect external collectors. Only external
 * `.collect()` calls increment subscriptionCount—internal flow operations (map, flatMapLatest,
 * update, etc.) do not affect it.
 *
 * **Common Pattern:**
 * ```kotlin
 * private val _triggerFlow = MutableStateFlow(Unit)
 * val dataFlow = _triggerFlow
 *     .observeWhenSubscribedAndLoggedIn(userFlow) { activeUserId ->
 *         repository.getData(activeUserId)  // Only runs when dataFlow is collected
 *     }
 * // _triggerFlow.update {} does NOT affect subscriptionCount
 * ```
 *
 * **Observer Lifecycle:**
 * - **Invoked** when subscriptionCount > 0 and a user is logged in
 * - **Re-invoked** when the active user changes (account switch)
 * - **Canceled** when subscribers disconnect or user logs out
 *
 * @see observeWhenSubscribedAndUnlocked for variant that also requires vault to be unlocked
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> MutableStateFlow<T>.observeWhenSubscribedAndLoggedIn(
    userStateFlow: Flow<UserStateJson?>,
    observer: (activeUserId: String) -> Flow<R>,
): Flow<R> =
    combine(
        this.subscriptionCount.map { it > 0 }.distinctUntilChanged(),
        userStateFlow.map { it?.activeUserId }.distinctUntilChanged(),
    ) { isSubscribed, activeUserId ->
        activeUserId.takeIf { isSubscribed }
    }
        .flatMapLatest { activeUserId ->
            activeUserId?.let(observer) ?: flow { awaitCancellation() }
        }

/**
 * Lazily invokes the [observer] callback with the active user's ID only when this MutableStateFlow
 * has external collectors, a user is logged in, and the active user's vault is unlocked. Designed
 * for expensive operations that should only run when UI actively observes the resulting data.
 *
 * **Active User Tracking:**
 * This function specifically tracks the active user from [userStateFlow]. When the active user
 * changes (e.g., account switching), the previous observer flow is canceled and a new one is
 * started for the new active user. The vault unlock state is also tracked per-user.
 *
 * **Subscription Detection:**
 * Uses [MutableStateFlow.subscriptionCount] to detect external collectors. Only external
 * `.collect()` calls increment subscriptionCount—internal flow operations (map, flatMapLatest,
 * update, etc.) do not affect it.
 *
 * **Common Pattern:**
 * ```kotlin
 * private val _triggerFlow = MutableStateFlow(Unit)
 * val dataFlow = _triggerFlow
 *     .observeWhenSubscribedAndUnlocked(userFlow, unlockFlow) { activeUserId ->
 *         repository.getExpensiveData(activeUserId)  // Only runs when dataFlow is collected
 *     }
 * // _triggerFlow.update {} does NOT affect subscriptionCount
 * ```
 *
 * **Observer Lifecycle:**
 * - **Invoked** when subscriptionCount > 0, a user is logged in, and active user's vault unlocked
 * - **Re-invoked** when the active user changes (account switch) or vault state changes
 * - **Canceled** when subscribers disconnect, user logs out, or vault locks
 *
 * @see observeWhenSubscribedAndLoggedIn for variant without vault unlock requirement
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun <T, R> MutableStateFlow<T>.observeWhenSubscribedAndUnlocked(
    userStateFlow: Flow<UserStateJson?>,
    vaultUnlockFlow: Flow<List<VaultUnlockData>>,
    observer: (activeUserId: String) -> Flow<R>,
): Flow<R> =
    combine(
        this.subscriptionCount.map { it > 0 }.distinctUntilChanged(),
        userStateFlow.map { it?.activeUserId }.distinctUntilChanged(),
        userStateFlow
            .map { it?.activeUserId }
            .distinctUntilChanged()
            .filterNotNull()
            .flatMapLatest { activeUserId ->
                vaultUnlockFlow
                    .map { unlockData -> unlockData.any { it.userId == activeUserId } }
                    .distinctUntilChanged()
            },
    ) { isSubscribed, activeUserId, isUnlocked ->
        activeUserId.takeIf { isSubscribed && isUnlocked }
    }
        .flatMapLatest { activeUserId ->
            activeUserId?.let(observer) ?: flow { awaitCancellation() }
        }
