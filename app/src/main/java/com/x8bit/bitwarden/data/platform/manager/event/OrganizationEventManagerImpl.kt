package com.x8bit.bitwarden.data.platform.manager.event

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.datasource.disk.EventDiskSource
import com.x8bit.bitwarden.data.platform.datasource.network.model.OrganizationEventJson
import com.x8bit.bitwarden.data.platform.datasource.network.service.EventService
import com.x8bit.bitwarden.data.platform.manager.dispatcher.DispatcherManager
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.ZonedDateTime

/**
 * The amount of time to delay before attempting the first upload events after the app is
 * foregrounded.
 */
private const val UPLOAD_DELAY_INITIAL_MS: Long = 120_000L

/**
 * The amount of time to delay before a subsequent attempts to upload events after the first one.
 */
private const val UPLOAD_DELAY_INTERVAL_MS: Long = 300_000L

/**
 * Default implementation of [OrganizationEventManager].
 */
@Suppress("LongParameterList")
class OrganizationEventManagerImpl(
    private val clock: Clock,
    private val authRepository: AuthRepository,
    private val vaultRepository: VaultRepository,
    private val eventDiskSource: EventDiskSource,
    private val eventService: EventService,
    dispatcherManager: DispatcherManager,
    processLifecycleOwner: LifecycleOwner = ProcessLifecycleOwner.get(),
) : OrganizationEventManager {
    private val ioScope = CoroutineScope(dispatcherManager.io)
    private var job: Job = Job().apply { complete() }

    init {
        processLifecycleOwner.lifecycle.addObserver(
            object : DefaultLifecycleObserver {
                override fun onStart(owner: LifecycleOwner) = start()

                override fun onStop(owner: LifecycleOwner) = stop()
            },
        )
    }

    override fun trackEvent(event: OrganizationEvent) {
        val userId = authRepository.activeUserId ?: return
        if (authRepository.authStateFlow.value !is AuthState.Authenticated) return
        val organizations = authRepository.organizations.filter { it.shouldUseEvents }
        if (organizations.none()) return

        ioScope.launch {
            event.cipherId?.let { id ->
                val cipherOrganizationId = vaultRepository
                    .getVaultItemStateFlow(itemId = id)
                    .first { it.data != null }
                    .data
                    ?.organizationId
                    ?: return@launch
                if (organizations.none { it.id == cipherOrganizationId }) return@launch
            }
            eventDiskSource.addOrganizationEvent(
                userId = userId,
                event = OrganizationEventJson(
                    type = event.type,
                    cipherId = event.cipherId,
                    date = ZonedDateTime.now(clock),
                ),
            )
        }
    }

    private suspend fun uploadEvents() {
        val userId = authRepository.activeUserId ?: return
        val events = eventDiskSource
            .getOrganizationEvents(userId = userId)
            .takeUnless { it.isEmpty() }
            ?: return
        eventService
            .sendOrganizationEvents(events = events)
            .onSuccess { eventDiskSource.deleteOrganizationEvents(userId = userId) }
    }

    private fun start() {
        job.cancel()
        job = ioScope.launch {
            delay(timeMillis = UPLOAD_DELAY_INITIAL_MS)
            uploadEvents()
            while (coroutineContext.isActive) {
                delay(timeMillis = UPLOAD_DELAY_INTERVAL_MS)
                uploadEvents()
            }
        }
    }

    private fun stop() {
        job.cancel()
        ioScope.launch { uploadEvents() }
    }
}
