package com.x8bit.bitwarden.data.platform.manager

import com.x8bit.bitwarden.data.auth.datasource.disk.AuthDiskSource
import com.x8bit.bitwarden.data.platform.datasource.disk.PushDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.model.PushTokenRequest
import com.x8bit.bitwarden.data.platform.datasource.network.service.PushService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/**
 * Primary implementation of [PushManager].
 */
class PushManagerImpl @Inject constructor(
    private val authDiskSource: AuthDiskSource,
    private val pushDiskSource: PushDiskSource,
    private val pushService: PushService,
    private val clock: Clock,
    dispatcherManager: DispatcherManager,
) : PushManager {
    private val ioScope = CoroutineScope(dispatcherManager.io)
    private val unconfinedScope = CoroutineScope(dispatcherManager.unconfined)

    init {
        authDiskSource
            .userStateFlow
            .mapNotNull { it?.activeUserId }
            .distinctUntilChanged()
            .onEach { registerStoredPushTokenIfNecessary() }
            .launchIn(unconfinedScope)
    }

    override fun registerPushTokenIfNecessary(token: String) {
        pushDiskSource.registeredPushToken = token
        val userId = authDiskSource.userState?.activeUserId ?: return
        ioScope.launch {
            registerPushTokenIfNecessaryInternal(
                userId = userId,
                token = token,
            )
        }
    }

    override fun registerStoredPushTokenIfNecessary() {
        val userId = authDiskSource.userState?.activeUserId ?: return

        // If the last registered token is from less than a day before, skip this for now
        val lastRegistration = pushDiskSource.getLastPushTokenRegistrationDate(userId)?.toInstant()
        val dayBefore = clock.instant().minus(1, ChronoUnit.DAYS)
        if (lastRegistration?.isAfter(dayBefore) == true) return

        ioScope.launch {
            pushDiskSource.registeredPushToken?.let {
                registerPushTokenIfNecessaryInternal(
                    userId = userId,
                    token = it,
                )
            }
        }
    }

    private suspend fun registerPushTokenIfNecessaryInternal(userId: String, token: String) {
        val currentToken = pushDiskSource.getCurrentPushToken(userId)

        if (token == currentToken) {
            // Our token is up-to-date, so just update the last registration date
            pushDiskSource.storeLastPushTokenRegistrationDate(
                userId,
                ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
            )
            return
        }

        pushService
            .putDeviceToken(
                PushTokenRequest(token),
            )
            .fold(
                onSuccess = {
                    pushDiskSource.storeLastPushTokenRegistrationDate(
                        userId,
                        ZonedDateTime.ofInstant(clock.instant(), ZoneOffset.UTC),
                    )
                    pushDiskSource.storeCurrentPushToken(
                        userId = userId,
                        pushToken = token,
                    )
                },
                onFailure = {
                    // Silently fail. This call will be attempted again the next time the token
                    // registration is done.
                 },
            )
    }
}
