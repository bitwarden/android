package com.x8bit.bitwarden.data.platform.manager.restriction

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.RestrictionsManager
import android.os.Bundle
import com.x8bit.bitwarden.data.platform.manager.AppStateManager
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.AppForegroundState
import com.x8bit.bitwarden.data.platform.repository.EnvironmentRepository
import com.x8bit.bitwarden.data.platform.repository.model.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/**
 * The default implementation of the [RestrictionManager].
 */
class RestrictionManagerImpl(
    appStateManager: AppStateManager,
    dispatcherManager: DispatcherManager,
    private val context: Context,
    private val environmentRepository: EnvironmentRepository,
    private val restrictionsManager: RestrictionsManager,
) : RestrictionManager {
    private val mainScope = CoroutineScope(dispatcherManager.main)
    private val intentFilter = IntentFilter(Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED)
    private val restrictionsChangedReceiver = RestrictionsChangedReceiver()
    private var isReceiverRegistered = false

    init {
        appStateManager
            .appForegroundStateFlow
            .onEach {
                when (it) {
                    AppForegroundState.BACKGROUNDED -> handleBackground()
                    AppForegroundState.FOREGROUNDED -> handleForeground()
                }
            }
            .launchIn(mainScope)
    }

    private fun handleBackground() {
        if (isReceiverRegistered) {
            context.unregisterReceiver(restrictionsChangedReceiver)
        }
        isReceiverRegistered = false
    }

    private fun handleForeground() {
        context.registerReceiver(restrictionsChangedReceiver, intentFilter)
        isReceiverRegistered = true
        updatePreconfiguredRestrictionSettings()
    }

    private fun updatePreconfiguredRestrictionSettings() {
        restrictionsManager
            .applicationRestrictions
            ?.takeUnless { it.isEmpty }
            ?.let { setPreconfiguredSettings(it) }
    }

    private fun setPreconfiguredSettings(bundle: Bundle) {
        bundle
            .getString(BASE_ENVIRONMENT_URL_RESTRICTION_KEY)
            ?.let { url -> setPreconfiguredUrl(baseEnvironmentUrl = url) }
    }

    private fun setPreconfiguredUrl(baseEnvironmentUrl: String) {
        environmentRepository.environment = when (val current = environmentRepository.environment) {
            Environment.Us -> {
                when (baseEnvironmentUrl) {
                    // If the base matches the predefined US environment, leave it alone
                    Environment.Us.environmentUrlData.base -> current
                    // If the base does not match the predefined US environment, create a
                    // self-hosted environment with the new base
                    else -> current.toSelfHosted(base = baseEnvironmentUrl)
                }
            }

            Environment.Eu -> {
                when (baseEnvironmentUrl) {
                    // If the base matches the predefined EU environment, leave it alone
                    Environment.Eu.environmentUrlData.base -> current
                    // If the base does not match the predefined EU environment, create a
                    // self-hosted environment with the new base
                    else -> current.toSelfHosted(base = baseEnvironmentUrl)
                }
            }

            is Environment.SelfHosted -> current.toSelfHosted(base = baseEnvironmentUrl)
        }
    }

    /**
     * A [BroadcastReceiver] used to listen for [Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED]
     * updates.
     *
     * Note: The `Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED` will only be received if the
     * `BroadcastReceiver` is dynamically registered, so this cannot be registered in the manifest.
     */
    private inner class RestrictionsChangedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED) {
                updatePreconfiguredRestrictionSettings()
            }
        }
    }
}

private const val BASE_ENVIRONMENT_URL_RESTRICTION_KEY: String = "baseEnvironmentUrl"

/**
 * Helper method for creating a new [Environment.SelfHosted] with a new base.
 */
private fun Environment.toSelfHosted(
    base: String,
): Environment.SelfHosted =
    Environment.SelfHosted(
        environmentUrlData = environmentUrlData.copy(base = base),
    )
