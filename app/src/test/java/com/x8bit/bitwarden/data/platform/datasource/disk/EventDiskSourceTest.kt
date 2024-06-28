package com.x8bit.bitwarden.data.platform.datasource.disk

import com.x8bit.bitwarden.data.platform.base.FakeDispatcherManager
import com.x8bit.bitwarden.data.platform.datasource.disk.dao.FakeOrganizationEventDao
import com.x8bit.bitwarden.data.platform.datasource.disk.entity.OrganizationEventEntity
import com.x8bit.bitwarden.data.platform.datasource.network.di.PlatformNetworkModule
import com.x8bit.bitwarden.data.platform.datasource.network.model.OrganizationEventJson
import com.x8bit.bitwarden.data.platform.manager.model.OrganizationEventType
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.ZonedDateTime

class EventDiskSourceTest {
    private val fixedClock: Clock = Clock.fixed(
        Instant.parse("2023-10-27T12:00:00Z"),
        ZoneOffset.UTC,
    )

    private val fakeOrganizationEventDao = FakeOrganizationEventDao()
    private val fakeDispatcherManager = FakeDispatcherManager()
    private val json = PlatformNetworkModule.providesJson()

    private val eventDiskSource: EventDiskSource = EventDiskSourceImpl(
        organizationEventDao = fakeOrganizationEventDao,
        dispatcherManager = fakeDispatcherManager,
        json = json,
    )

    @Test
    fun `addOrganizationEvent should insert a new organization event`() = runTest {
        val userId = "userId-1"
        val organizationEvent = OrganizationEventJson(
            type = OrganizationEventType.CIPHER_DELETED,
            cipherId = "cipherId-1",
            date = ZonedDateTime.now(fixedClock),
        )

        eventDiskSource.addOrganizationEvent(
            userId = userId,
            event = organizationEvent,
        )

        assertEquals(
            listOf(
                OrganizationEventEntity(
                    id = 0,
                    userId = userId,
                    organizationEventType = "1102",
                    cipherId = "cipherId-1",
                    date = ZonedDateTime.now(fixedClock),
                ),
            ),
            fakeOrganizationEventDao.storedEvents,
        )
        assertFalse(fakeOrganizationEventDao.isDeleteCalled)
        assertTrue(fakeOrganizationEventDao.isInsertCalled)
    }

    @Test
    fun `deleteOrganizationEvents should delete all organization events`() = runTest {
        val userId = "userId-1"
        fakeOrganizationEventDao.storedEvents.addAll(
            listOf(
                OrganizationEventEntity(
                    id = 1,
                    userId = userId,
                    organizationEventType = "1102",
                    cipherId = "cipherId-1",
                    date = ZonedDateTime.now(fixedClock),
                ),
                OrganizationEventEntity(
                    id = 2,
                    userId = "userId-2",
                    organizationEventType = "1102",
                    cipherId = "cipherId-2",
                    date = ZonedDateTime.now(fixedClock),
                ),
            ),
        )

        eventDiskSource.deleteOrganizationEvents(userId = userId)

        assertEquals(
            listOf(
                OrganizationEventEntity(
                    id = 2,
                    userId = "userId-2",
                    organizationEventType = "1102",
                    cipherId = "cipherId-2",
                    date = ZonedDateTime.now(fixedClock),
                ),
            ),
            fakeOrganizationEventDao.storedEvents,
        )
        assertTrue(fakeOrganizationEventDao.isDeleteCalled)
        assertFalse(fakeOrganizationEventDao.isInsertCalled)
    }

    @Test
    fun `getOrganizationEvents should retrieve the correct organization events`() = runTest {
        val userId = "userId-1"
        fakeOrganizationEventDao.storedEvents.addAll(
            listOf(
                OrganizationEventEntity(
                    id = 1,
                    userId = userId,
                    organizationEventType = "1102",
                    cipherId = "cipherId-1",
                    date = ZonedDateTime.now(fixedClock),
                ),
                OrganizationEventEntity(
                    id = 2,
                    userId = "userId-2",
                    organizationEventType = "1102",
                    cipherId = "cipherId-2",
                    date = ZonedDateTime.now(fixedClock),
                ),
            ),
        )

        val result = eventDiskSource.getOrganizationEvents(userId = userId)

        assertEquals(
            listOf(
                OrganizationEventJson(
                    type = OrganizationEventType.CIPHER_DELETED,
                    cipherId = "cipherId-1",
                    date = ZonedDateTime.now(fixedClock),
                ),
            ),
            result,
        )
        assertFalse(fakeOrganizationEventDao.isDeleteCalled)
        assertFalse(fakeOrganizationEventDao.isInsertCalled)
    }
}
