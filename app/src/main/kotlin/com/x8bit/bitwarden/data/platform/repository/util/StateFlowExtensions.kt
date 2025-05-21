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
 * Invokes the [observer] callback whenever the user is logged in, the active changes, and there
 * are subscribers to the [MutableStateFlow]. The flow from all previous calls to the `observer`
 * is canceled whenever the `observer` is re-invoked, there is no active user (logged-out), or
 * there are no subscribers to the [MutableStateFlow].
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
 * Invokes the [observer] callback whenever the user is logged in, the active changes,
 * the vault for the user changes and there are subscribers to the [MutableStateFlow].
 * The flow from all previous calls to the `observer`
 * is canceled whenever the `observer` is re-invoked, there is no active user (logged-out),
 * there are no subscribers to the [MutableStateFlow] or the vault is not unlocked.
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
                    .map { it.any { it.userId == activeUserId } }
                    .distinctUntilChanged()
            },
    ) { isSubscribed, activeUserId, isUnlocked ->
        activeUserId.takeIf { isSubscribed && isUnlocked }
    }
        .flatMapLatest { activeUserId ->
            activeUserId?.let(observer) ?: flow { awaitCancellation() }
        }
