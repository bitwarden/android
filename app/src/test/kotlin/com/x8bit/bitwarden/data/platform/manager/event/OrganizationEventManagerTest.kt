package com.x8bit.bitwarden.data.platform.manager.event

import com.bitwarden.core.data.manager.dispatcher.FakeDispatcherManager
import com.bitwarden.core.data.repository.model.DataState
import com.bitwarden.core.data.util.advanceTimeByAndRunCurrent
import com.bitwarden.core.data.util.asSuccess
import com.bitwarden.network.model.OrganizationEventJson
import com.bitwarden.network.model.OrganizationEventType
import com.bitwarden.network.model.createMockOrganization
import com.bitwarden.network.service.EventService
import com.bitwarden.vault.CipherView
import com.x8bit.bitwarden.data.auth.repository.AuthRepository
import com.x8bit.bitwarden.data.auth.repository.model.AuthState
import com.x8bit.bitwarden.data.platform.datasource.disk.EventDiskSource
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEvent
import com.x8bit.bitwarden.data.util.FakeLifecycleOwner
import com.x8bit.bitwarden.data.vault.datasource.sdk.model.createMockCipherView
import com.x8bit.bitwarden.data.vault.repository.VaultRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class OrganizationEventManagerTest {

    private val fakeLifecycleOwner = FakeLifecycleOwner()
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )
    private val dispatcher = StandardTestDispatcher()
    private val fakeDispatcherManager = FakeDispatcherManager(io = dispatcher)
    private val mutableAuthStateFlow = MutableStateFlow<AuthState>(value = AuthState.Uninitialized)
    private val authRepository = mockk<AuthRepository> {
        every { activeUserId } returns USER_ID
        every { authStateFlow } returns mutableAuthStateFlow
        every { organizations } returns emptyList()
    }
    private val mutableVaultItemStateFlow = MutableStateFlow<DataState<CipherView?>>(
        value = DataState.Loading,
    )
    private val vaultRepository = mockk<VaultRepository> {
        every { getVaultItemStateFlow(itemId = any()) } returns mutableVaultItemStateFlow
    }
    private val eventService = mockk<EventService>()
    private val eventDiskSource = mockk<EventDiskSource> {
        coEvery { addOrganizationEvent(userId = any(), event = any()) } just runs
    }

    private val organizationEventManager: OrganizationEventManager = OrganizationEventManagerImpl(
        processLifecycleOwner = fakeLifecycleOwner,
        clock = fixedClock,
        dispatcherManager = fakeDispatcherManager,
        authRepository = authRepository,
        vaultRepository = vaultRepository,
        eventService = eventService,
        eventDiskSource = eventDiskSource,
    )

    @Test
    fun `onLifecycleStart should upload events after 2 minutes and again after 5 more minutes`() =
        runTest {
            val organizationEvent = OrganizationEventJson(
                type = OrganizationEventType.CIPHER_UPDATED,
                cipherId = CIPHER_ID,
                date = ZonedDateTime.now(fixedClock),
            )
            val events = listOf(organizationEvent)
            coEvery { eventDiskSource.getOrganizationEvents(userId = USER_ID) } returns events
            coEvery {
                eventService.sendOrganizationEvents(events = events)
            } returns Unit.asSuccess()
            coEvery { eventDiskSource.deleteOrganizationEvents(userId = USER_ID) } just runs

            fakeLifecycleOwner.lifecycle.dispatchOnStart()

            dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 120_000L)
            coVerify(exactly = 1) {
                eventDiskSource.getOrganizationEvents(userId = USER_ID)
                eventService.sendOrganizationEvents(events = events)
                eventDiskSource.deleteOrganizationEvents(userId = USER_ID)
            }

            dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 300_000L)
            coVerify(exactly = 2) {
                eventDiskSource.getOrganizationEvents(userId = USER_ID)
                eventService.sendOrganizationEvents(events = events)
                eventDiskSource.deleteOrganizationEvents(userId = USER_ID)
            }
        }

    @Test
    fun `onLifecycleStop should upload events immediately`() = runTest {
        val organizationEvent = OrganizationEventJson(
            type = OrganizationEventType.CIPHER_UPDATED,
            cipherId = CIPHER_ID,
            date = ZonedDateTime.now(fixedClock),
        )
        val events = listOf(organizationEvent)
        coEvery { eventDiskSource.getOrganizationEvents(userId = USER_ID) } returns events
        coEvery { eventService.sendOrganizationEvents(events = events) } returns Unit.asSuccess()
        coEvery { eventDiskSource.deleteOrganizationEvents(userId = USER_ID) } just runs

        fakeLifecycleOwner.lifecycle.dispatchOnStop()

        dispatcher.advanceTimeByAndRunCurrent(delayTimeMillis = 120_000L)
        coVerify(exactly = 1) {
            eventDiskSource.getOrganizationEvents(userId = USER_ID)
            eventService.sendOrganizationEvents(events = events)
            eventDiskSource.deleteOrganizationEvents(userId = USER_ID)
        }
    }

    @Test
    fun `trackEvent should do nothing if there is no active user`() {
        every { authRepository.activeUserId } returns null

        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientAutoFilled(
                cipherId = CIPHER_ID,
            ),
        )

        coVerify(exactly = 0) {
            eventDiskSource.addOrganizationEvent(userId = any(), event = any())
        }
    }

    @Test
    fun `trackEvent should do nothing if the active user is not authenticated`() {
        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientAutoFilled(
                cipherId = CIPHER_ID,
            ),
        )

        coVerify(exactly = 0) {
            eventDiskSource.addOrganizationEvent(userId = any(), event = any())
        }
    }

    @Test
    fun `trackEvent should do nothing if the active user has no organizations that use events`() {
        mutableAuthStateFlow.value = AuthState.Authenticated(accessToken = "access-token")
        val organization = createMockOrganization(number = 1)
        every { authRepository.organizations } returns listOf(organization)

        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientAutoFilled(
                cipherId = CIPHER_ID,
            ),
        )

        coVerify(exactly = 0) {
            eventDiskSource.addOrganizationEvent(userId = any(), event = any())
        }
    }

    @Suppress("MaxLineLength")
    @Test
    fun `trackEvent should do nothing if the cipher does not belong to an organization that uses events`() {
        mutableAuthStateFlow.value = AuthState.Authenticated(accessToken = "access-token")
        val organization = createMockOrganization(number = 1).copy(shouldUseEvents = true)
        every { authRepository.organizations } returns listOf(organization)
        val cipherView = createMockCipherView(number = 1)
        mutableVaultItemStateFlow.value = DataState.Loaded(data = cipherView)

        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientAutoFilled(
                cipherId = CIPHER_ID,
            ),
        )

        coVerify(exactly = 0) {
            eventDiskSource.addOrganizationEvent(userId = any(), event = any())
        }
    }

    @Test
    fun `trackEvent should add the event to disk if the ciphers organization allows it`() {
        mutableAuthStateFlow.value = AuthState.Authenticated(accessToken = "access-token")
        val organization = createMockOrganization(number = 1).copy(
            id = "mockOrganizationId-1",
            shouldUseEvents = true,
        )
        every { authRepository.organizations } returns listOf(organization)
        val cipherView = createMockCipherView(number = 1)
        mutableVaultItemStateFlow.value = DataState.Loaded(data = cipherView)

        organizationEventManager.trackEvent(
            event = OrganizationEvent.CipherClientAutoFilled(cipherId = CIPHER_ID),
        )

        dispatcher.scheduler.runCurrent()
        coVerify(exactly = 1) {
            eventDiskSource.addOrganizationEvent(
                userId = USER_ID,
                event = OrganizationEventJson(
                    type = OrganizationEventType.CIPHER_CLIENT_AUTO_FILLED,
                    cipherId = CIPHER_ID,
                    date = ZonedDateTime.now(fixedClock),
                ),
            )
        }
    }
}

private const val CIPHER_ID: String = "mockId-1"
private const val USER_ID: String = "user-id"
