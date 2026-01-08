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
 * Lazily invokes the [observer] callback only when this MutableStateFlow has external collectors
 * and the user is authenticated. Designed for operations that should only run when UI actively
 * observes the resulting data, but do not require the vault to be unlocked.
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
 *     .observeWhenSubscribedAndLoggedIn(userFlow) { userId ->
 *         repository.getData(userId)  // Only runs when dataFlow is collected
 *     }
 * // _triggerFlow.update {} does NOT affect subscriptionCount
 * ```
 *
 * **Observer Lifecycle:**
 * - **Invoked** when subscriptionCount > 0 and user is logged in
 * - **Re-invoked** when active user changes
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
 * Lazily invokes the [observer] callback only when this MutableStateFlow has external collectors,
 * the user is authenticated, and their vault is unlocked. Designed for expensive operations that
 * should only run when UI actively observes the resulting data.
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
 *     .observeWhenSubscribedAndUnlocked(userFlow, unlockFlow) { userId ->
 *         repository.getExpensiveData(userId)  // Only runs when dataFlow is collected
 *     }
 * // _triggerFlow.update {} does NOT affect subscriptionCount
 * ```
 *
 * **Observer Lifecycle:**
 * - **Invoked** when subscriptionCount > 0, user logged in, vault unlocked
 * - **Re-invoked** when active user or vault state changes
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
